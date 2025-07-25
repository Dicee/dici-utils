#!/usr/bin/env python3

import datetime as dt
import functools
import os
import os.path
import shlex
import signal
import subprocess as sp
from multiprocessing import Pool
from concurrent.futures import ThreadPoolExecutor
from threading import Lock, Semaphore

import workspace
from formatting_utils import bold, red, green, yellow, LogLevel
from program_utils import get_user_input, exit_if_task_failed
from utils import ExecutionException

SEQUENTIAL_RUNNING_TIME = 'sequential_running_time'

def exec_cmd(cmd, stderr=None, stdout=None, cwd=None, raise_on_failure=False):
    start = dt.datetime.now()
    process = sp.Popen(_normalize_command(cmd), stdout=stdout, stderr=stderr, cwd=cwd)
    process.wait()
    result = None if stdout != sp.PIPE else process.communicate()[0].decode("utf-8")
    task_result = TaskResult(cmd, process.returncode, dt.datetime.now() - start, result, cwd)

    if raise_on_failure and task_result.is_failure():
        raise ExecutionException(task_result)

    return task_result


def exec(args):
    log_level = LogLevel.verbose() if args.verbose else LogLevel.normal()
    exec_for_all_packages(args.command, args.packages, args.threads, args.working_dir, args.is_template_command, log_level)


def exec_for_all_packages(command, packages, threads, working_dir=None, is_template_command=False, log_level=LogLevel.normal()):
    # for some reason I don't understand, I'm getting race conditions if the root is not created before any parallel
    # tasks are started. To be honest, I find the whole threading vs multiprocessing thing a big mess.
    logs_root = workspace.Context.logs_root()
    start = dt.datetime.now()

    logger = ExecutionLogger(log_level)
    logger.before_all(command, packages, logs_root)

    # for deterministic order when printing the summary
    packages = _sort_packages_by_name(packages)

    with ThreadPoolExecutor(threads) as thread_pool:
        task_results = list(thread_pool.map(functools.partial(_execute_for_package, command=command, is_template_command=is_template_command, working_dir=working_dir, logger=logger), packages))

    running_time = dt.datetime.now() - start
    logger.after_all(command, task_results, running_time)
    logger.execution_summary(task_results)

    return task_results


def _execute_for_package(package, command, is_template_command, working_dir, logger):
    if is_template_command:
        command = command.format(package.name)

    if working_dir is None:
        working_dir = package.path()

    start = dt.datetime.now()
    logger.before_package(command, working_dir)

    process = sp.Popen(_normalize_command(command), stdout=package.open_stdout(), stderr=package.open_stderr(), cwd=working_dir)
    process.wait()
    end = dt.datetime.now()

    result = PackageTaskResult(package, command, process.returncode, end - start, working_dir)
    logger.after_package(result)

    return result


def _normalize_command(cmd):
    return shlex.split(cmd)


def interactive_package_operation(operation, packages):
    # for deterministic order when making decisions on each package
    packages = _sort_packages_by_name(packages)

    for package in packages:
        operation.prepare(package)
        action = operation.select_action()
        action_result = operation.execute_action(action, package)

        if action_result == InteractiveActionResult.INTERRUPT:
            print(bold(yellow('Interrupting interactive {}'.format(operation.name))))
            return
        elif action_result == InteractiveActionResult.SKIP:
            print(bold(yellow('Skipping {}'.format(package))))
        elif action_result == InteractiveActionResult.DONE_SELECTING:
            # done selecting but still want to execute the operation at the end
            break
        elif action_result == InteractiveActionResult.FAILURE:
            # relying on the InteractivePackageOperation instance to do appropriate logging
            break
        else:
            if action_result != InteractiveActionResult.SUCCESS:
                raise Exception('Unrecognized result type: {}'.format(action_result))

    operation.finalize()


def execute_on_dependency_graph(command, graph, threads):
    start = dt.datetime.now()
    nodes = set(graph.nodes.values())

    unbuilt_dependencies = {node.name: len(node.dependencies) for node in nodes}
    to_build = nodes.copy()
    leaves = {workspace.Package(n.name) for n in nodes if len(n.dependencies.intersection(to_build)) == 0}

    checked_packages = workspace.Context.ws.checked_packages()
    to_actually_build = {workspace.Package(n.name) for n in to_build}.intersection(checked_packages)

    # for some reason I don't understand, I'm getting race conditions if the root is not created before any parallel
    # tasks are started. To be honest, I find the whole threading vs multiprocessing thing a big mess.
    workspace.Context.logs_root()

    execution_context = ParallelGraphExecutionContext(threads)
    for leaf in leaves:
        _build_async(execution_context, command, leaf, graph, to_build, unbuilt_dependencies)

    for i in range(len(to_actually_build)):
        execution_context.semaphore.acquire()
        print('Built {}/{} package(s)'.format(i + 1, len(to_actually_build)))

    running_time = dt.datetime.now() - start
    time_saved = execution_context.stats[SEQUENTIAL_RUNNING_TIME] - running_time

    print(bold(green('Built {} packages in {}s'.format(len(to_actually_build), round(running_time.total_seconds())))))
    print('You saved approximately {}s compared to running sequentially'.format(round(time_saved.total_seconds())))


def _build_async(execution_context, command, package, graph, to_build, unbuilt_dependencies):
    should_build = package in workspace.Context.ws.checked_packages()

    args = [package, command, False, None, ExecutionLogger(LogLevel.normal())]
    callback = lambda task_result: _handle_build_complete(execution_context, task_result, command, package, to_build, graph, unbuilt_dependencies)

    if not should_build:
        # nothing to do, immediately complete the 'build' to trigger upstream dependencies. Can't do on the same thread, this would
        # cause a deadlock as we would try to acquire the lock for the main thread although somebody is already holding it.
        execution_context.apply_async(_noop, [], callback=callback, error_callback=_fail_build)
    else:
        print(bold('Build logs for {} will be located at {}'.format(package.name, package.logs_root())))
        execution_context.apply_async(_execute_for_package, args, callback=callback, error_callback=_fail_build)


def _noop():
    return None


def _handle_build_complete(execution_context, task_result, command, package, to_build, graph, unbuilt_dependencies):
    checked_packages = workspace.Context.ws.checked_packages()

    if package in checked_packages:
        print(task_result)
        if task_result.is_failure():
            _fail_build(None)
        else:
            execution_context.semaphore.release()

    execution_context.lock.acquire()
    try:
        to_build.remove(graph[package.name])

        if package in checked_packages:
            if SEQUENTIAL_RUNNING_TIME not in execution_context.stats:
                execution_context.stats[SEQUENTIAL_RUNNING_TIME] = dt.timedelta(0)

            execution_context.stats[SEQUENTIAL_RUNNING_TIME] += task_result.running_time

        for parent in graph[package.name].parents:
            unbuilt_dependencies[parent.name] -= 1
            if unbuilt_dependencies[parent.name] == 0:
                _build_async(execution_context, command, workspace.Package(parent.name), graph, to_build, unbuilt_dependencies)

    except Exception as err:
        _fail_build(err)
    finally:
        execution_context.lock.release()


def _fail_build(error):
    if error is not None:
        print(bold(red('A fatal error occurred during the build')))
        print(red(str(error)))
    # couldn't find a way to reliably kill all child processes and give control back to the user's console =(
    os.kill(os.getpid(), signal.SIGKILL)


def _dump_log_file(log_path, is_failure):
    with open(log_path, 'rt') as log_file:
        for line in log_file.readlines():
            print(red(line) if is_failure else line, end='')


def _pool_initializer():
    # ignore CTRL+C in the worker process. It prevents dumping annoying stacktraces from all child processes when interrupting
    signal.signal(signal.SIGINT, signal.SIG_IGN)


# unfortunately has to return a list as there's no sorted set in Python...
def _sort_packages_by_name(packages):
    packages = list(packages)
    packages.sort(key=lambda pkg: pkg.name)
    return packages


class InteractivePackageOperation:
    _CONFIRM = 'y'

    def __init__(self, name, prepare_op, op, menu, confirmation_message, skip_check):
        self.name = name
        self.prepare_op = prepare_op
        self.op = op
        self.menu = menu
        self.confirmation_message = confirmation_message
        self.skip_check = skip_check

        if len(menu) == 0:
            raise Exception('Menu cannot be empty for operation {}'.format(name))

        if menu[0] != InteractivePackageOperation._CONFIRM:
            raise Exception("Expected first menu option to be '{}' but options where: {}".format(InteractivePackageOperation._CONFIRM, menu))


    def prepare(self, package):
        os.chdir(package.path())
        print(bold('====== {} ======'.format(package)))
        self.prepare_op(package)


    def select_action(self):
        if self.skip_check:
            return InteractivePackageOperation._CONFIRM
        return get_user_input(self.menu, self.confirmation_message)


    def execute_action(self, action, package):
        raise Exception('execute_action must be implemented for {}'.format(self.name))


    def _raise_unknown_action(self, action):
        raise Exception("Unknown action '{}'. This is most likely a bug in this tool, it shouldn't be possible to select an invalid option".format(action))


    def finalize(self):
        pass


class InteractiveRebase(InteractivePackageOperation):
    _CONFIRMATION_MESSAGE = 'Do you want to rebase this package given the changes displayed above?'

    def __init__(self, skip_check):
        super().__init__('rebase', lambda ignored: exec_cmd('gcheck'), 'grebase', ['y', 'n', 'exit'], InteractiveRebase._CONFIRMATION_MESSAGE, skip_check)


    # override
    def execute_action(self, action, package):
        if action == 'y':
            if exec_cmd(self.op, stderr=package.open_stderr()).is_failure():
                print(bold(red('Failed to {} {}. Interrupting.'.format(self.name, package))))
                _dump_log_file(package.stderr_path(), True)
                return InteractiveActionResult.FAILURE
            return InteractiveActionResult.SUCCESS
        elif action == 'n':
            return InteractiveActionResult.SKIP
        elif action == 'exit':
            return InteractiveActionResult.INTERRUPT
        raise super()._raise_unknown_action(action)


class InteractivePush(InteractivePackageOperation):
    _CONFIRMATION_MESSAGE = 'Do you want to push this package given the changes displayed above?'

    def __init__(self, skip_check):
        super().__init__('push', lambda ignored: exec_cmd('gcheck'), 'gpush', ['y', 'n', 'done', 'abort'], InteractivePush._CONFIRMATION_MESSAGE, skip_check)
        self.selected_packages = []


    # override
    def execute_action(self, action, package):
        if action == 'y':
            self.selected_packages.append(package)
            return InteractiveActionResult.SUCCESS
        elif action == 'n':
            return InteractiveActionResult.SKIP
        elif action == 'done':
            return InteractiveActionResult.DONE_SELECTING
        elif action == 'abort':
            return InteractiveActionResult.INTERRUPT
        raise super()._raise_unknown_action(action)

    # override
    def finalize(self):
        print(bold('Selected {} package(s) for {}'.format(len(self.selected_packages), self.name)))
        if len(self.selected_packages) > 0:
            task_results = exec_for_all_packages(self.op, self.selected_packages, threads=5)
            if any(task_result.is_failure() for task_result in task_results):
                exit(1)

        debug_advice = "Your packages were pushed but not related. You can try relating them manually using 'ws relate'."
        try:
            if len(self.selected_packages) > 1:
                workspace.Package.relate_package_heads(self.selected_packages)
        except ExecutionException as e:
            exit_if_task_failed(e.task_result, additional_message=debug_advice)


class InteractiveActionResult:
    INTERRUPT = 'INTERRUPT'
    SKIP = 'SKIP'
    DONE_SELECTING = 'DONE_SELECTING'
    FAILURE = 'FAILURE'
    SUCCESS = 'SUCCESS'


class ExecutionLogger:
    def __init__(self, log_level):
        self.log_level = log_level


    def before_all(self, command, packages, logs_root):
        if self.log_level.should_print_execution_details:
            print("Executing command {} in {} package(s). Logs will be located under {}".format(command, len(packages), logs_root))


    def after_all(self, command, task_results, running_time):
        if self.log_level.should_print_execution_details:
            print("Executed command '{}' in {} package(s) in {}s".format(command, len(task_results), round(running_time.total_seconds())))


    def before_package(self, command, working_dir):
        if self.log_level.should_print_execution_details:
            print(yellow("Executing '{}' in {}...".format(command, working_dir)))


    def after_package(self, task_result):
        if self.log_level.should_print_execution_details:
            completion_msg = "Executed '{}' in {}.".format(task_result.cmd, task_result.package.name)
            print(green(completion_msg) if task_result.is_success() else red(completion_msg))


    def execution_summary(self, task_results):
        sep = '\n- '
        if self.log_level.should_print_successes:
            print(bold('======= Execution summary ======='))
            succeeded_tasks = [str(result) for result in task_results if result.is_success()]
            if len(succeeded_tasks) > 0:
                print(bold('==== Successes ===='))
                print(sep + sep.join(succeeded_tasks))

        # always print failures
        failed_tasks = [str(result) for result in task_results if result.is_failure()]
        if len(failed_tasks) > 0:
            print(bold('==== Failures ===='))
            print(sep + sep.join(failed_tasks))

        if self.log_level.should_print_logs:
            # include the header only if the execution summary header was only present
            if self.log_level.should_print_successes and self.log_level.should_print_logs:
                print(bold('======= Logs ======='))

            for task_result in task_results:
                print(bold('==== {} ===='.format(task_result.package)))
                task_result.dump_stdout()
                task_result.dump_stderr()


class TaskResult:
    def __init__(self, cmd, return_code, running_time, result, working_dir):
        self.cmd = cmd
        self.return_code = return_code
        self.running_time = running_time
        self._result = result
        self.working_dir = working_dir

    def describe(self):
        status = 'succeeded' if self.is_success() else 'failed with return code ' + str(self.return_code)
        base_msg = "{} {} in {}s.".format(self.describe_command(), status, round(self.running_time.total_seconds()))
        if self.is_success():
            return bold(green(base_msg))
        else:
            return bold(red("{} {}".format(base_msg, self._additional_debug_info())))

    def describe_command(self):
        return "Command '{}'".format(self.cmd)

    def _additional_debug_info(self):
        return 'Working directory was: {}'.format(self.working_dir) if self.working_dir is not None else ''

    def result(self):
        self._check_is_success()
        return self._result

    def _check_is_success(self):
        if self.is_failure():
            raise Exception("Cannot get the result of a failed task")

    def is_success(self):
        return self.return_code == 0

    def is_failure(self):
        return not self.is_success()

    def __str__(self):
        return self.describe()

    def __repr__(self):
        return self.describe()


class PackageTaskResult(TaskResult):
    def __init__(self, package, cmd, return_code, running_time, working_dir):
        super().__init__(cmd, return_code, running_time, None, working_dir)
        self.package = package
        self.working_dir = working_dir

    # override
    def describe_command(self):
        return "Command '{}' for package {}".format(self.cmd, self.package.name)

    # override
    def _additional_debug_info(self):
        return 'You can check the logs under {}'.format(self.package.logs_root())

    # override
    def result(self):
        self._check_is_success()
        with open(self.stdout(), 'rt') as stdout:
            return stdout.read().strip()

    def stdout(self):
        return self.package.stdout_path()

    def stderr(self):
        return self.package.stderr_path()

    def dump_stdout(self):
        _dump_log_file(self.stdout(), False)

    def dump_stderr(self):
        _dump_log_file(self.stderr(), True)


class ParallelGraphExecutionContext:
    def __init__(self, threads):
        self.pool = Pool(processes=threads, initializer=_pool_initializer)
        self._pool = ThreadPoolExecutor(threads)
        self.lock = Lock()
        self.semaphore = Semaphore(0)
        self.stats = {}

    def apply_async(self, task, args, callback=_noop, error_callback=_noop):
        def _future_callback(future):
            try:
                result = future.result()
            except Exception as e:
                error_callback(e)

            callback(result)

        future = self._pool.submit(task, *args)
        future.add_done_callback(_future_callback)


