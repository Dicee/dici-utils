import os
import os.path
import sys

import execution_utils
from arghandler import *
from argparser_utils import ArgParser
from dependency_graph import DependencyGraph
from formatting_utils import bold, yellow, LogLevel
from program_utils import exit_if_task_failed, exit_with_error
from utils import *
from workspace import Package, Context


@subcmd(help='Creates a new workspace and pulls selected packages in parallel')
def create(parser, context, args):
    parser.add_argument('--name', '-n', type=str, help='Name of the workspace', required=True)
    parser.add_argument('--versionSet', '-vs', type=str, help='Version set of the workspace', required=True)
    ArgParser.configure_run_all_parser(parser, default_num_threads=DEFAULT_NUM_THREADS_FOR_CLONING)
    parsed_args = ArgParser.parse_and_normalize_args(parser, args)

    vs_option = '' if parsed_args.versionSet is None else '-vs {}'.format(parsed_args.versionSet)
    task_result = execution_utils.exec_cmd('brazil ws --create --name {} {}'.format(parsed_args.name, vs_option))
    if task_result.is_failure():
        exit_with_error('ERROR: Failed to create new workspace using {}'.format(task_result.describe_command()))
    else:
        os.chdir(parsed_args.name)
        _do_clone(parser, args)
    

@subcmd(help='Clones multiple Brazil repositories in parallel')
def clone(parser, context, args):
    ArgParser.configure_run_all_parser(parser, default_num_threads=DEFAULT_NUM_THREADS_FOR_CLONING)
    _do_clone(parser, args)


def _do_clone(parser, args):
    args = ArgParser.parse_and_normalize_args(parser, args, is_template_command=True, working_dir=Context.ws.root())
    args.command = 'brazil ws --use -p {}'
    execution_utils.exec(args)
    Context.ws.sync_package_info() # the workspaceInfo file might be modified concurrently by all threads, so we repair it at the end


@subcmd(help='Removes a list of Brazil packages')
def remove(parser, context, args):
    parser.add_argument('--packages', '-p', nargs='+', type=str, help="Space-separated list of packages to remove from the workspace", required=True)
    args = parser.parse_args(args)
    cmd = 'brazil ws --remove {}'.format(' '.join(['-p ' + package for package in args.packages]))
    execution_utils.exec_cmd(cmd, raise_on_failure=True)


@subcmd(help="Syncs the version set's metadata and pulls all checked packages in parallel")
def sync(parser, context, args):
    task_result = execution_utils.exec_cmd('brazil workspace sync --metadata')
    exit_if_task_failed(task_result, additional_message="Failed to sync version set's metadata")
    pull(parser, context, args)


@subcmd(help='Pulls selected packages in parallel')
def pull(parser, context, args):
    ArgParser.configure_run_all_parser(parser, default_num_threads=DEFAULT_NUM_THREADS_FOR_PULLING, default_packages=Context.ws.checked_packages())
    args = ArgParser.parse_and_normalize_args(parser, args)
    args.command = 'gpull'
    execution_utils.exec(args)


@subcmd(help='Executes an arbitrary command in parallel to all selected packages')
def run(parser, context, args):
    ArgParser.configure_run_all_parser(parser, default_packages=Context.ws.checked_packages())
    parser.add_argument('--command', '-c', type=str, help='Command to apply to selected packages', required=True)
    args = ArgParser.parse_and_normalize_args(parser, args)
    execution_utils.exec(args)


@subcmd(help='Lists the differences between the local HEAD and the associated remote branch for all packages with unpushed local commits')
def check(parser, context, args):
    parser.add_argument('--branch', '-b', type=str, help=
        'Name of the default remote to compare against in order to detect unpushed local commits (default: mainline). This parameter is only read'
        'used for packages that are not tracking a remote yet, otherwise it is determined on the fly.', default='mainline')
    branch = parser.parse_args(args).branch
    packages_with_changes = Context.ws.list_packages_with_unpushed_commits(branch)

    if len(packages_with_changes) > 0:
        Context.logs_root(force_create_new_root=True) # we had to run a command to find packages with changes, don't want to see these logs
        execution_utils.exec_for_all_packages('gcheck ' + branch, packages_with_changes, DEFAULT_NUM_THREADS, log_level=LogLevel.execution_logs_only())


@subcmd(help="Runs 'git status' on all packages with unstaged changes")
def status(parser, context, args):
    _execute_on_all_packages_with_unstaged_changes(parser, 'git status')


@subcmd(help="Stashes unstaged changes in all packages with unstaged changes")
def stash(parser, context, args):
    parser.add_argument('--name', '-n', type=str, help='Name of the stash', required=True)
    args = parser.parse_args(args)
    _execute_on_all_packages_with_unstaged_changes(parser, 'git stash save {}'.format(args.name))


@subcmd(help='Detects all packages with unpushed local commits and interactively allows rebasing them one by one')
def rebase(parser, context, args):
    _interactive_operation_on_unpushed_commits(parser, args, lambda parsed_args: execution_utils.InteractiveRebase(parsed_args.skip_check))


@subcmd(help='Detects all packages with unpushed local commits and interactively allows pushing them one by one')
def push(parser, context, args):
    _interactive_operation_on_unpushed_commits(parser, args, lambda parsed_args: execution_utils.InteractivePush(parsed_args.skip_check))


@subcmd(help='Relates specified commits in selected packages to each other (will show in code.amazon.com)')
def relate(parser, context, args):
    ArgParser.configure_multi_packages_command_parser(parser, default_packages=[], additional_help="Mutually exclusive with --commits (the current HEAD and branch of each package will be used).")
    parser.add_argument('--commits', '-c', type=str, help="A comma-separated list of commits. A commit has the following format: <PACKAGE>[:<COMMIT>[:<BRANCH>]]"
                                                          " where COMMIT is either a commit id or a reference of the form HEAD~<integer>."
                                                          " If not specified, the default commit will be HEAD and the default branch the current one."
                                                          " Mutually exclusive with --packages.", required=False)
    args = ArgParser.parse_and_normalize_args(parser, args)

    if len(args.packages) > 0:
        if args.commits is not None:
            raise InvalidInputException('--packages and --commits are mutually exclusive.')

        invalid_packages = [pkg for pkg in args.packages if not pkg.exists()]
        if len(invalid_packages):
            raise InvalidInputException('The following packages do not exist: {}. Is it a typo or did you want to use -c option instead?'.format(invalid_packages))

        Package.relate_package_heads(args.packages)
    else:
        if args.commits is None:
            raise InvalidInputException('Exactly one of --packages or --commits is required.')
        Package.relate_commits(args.commits)


@subcmd(help='Generates the dependency graph of the current package and opens it in the default browser')
def graph(parser, context, args):
    recipes = {
        '"all"': 'all packages',
        '"dra"': 'all packages starting with "Dra" prefix',
        '"adn"': 'all packages starting with "Adn" prefix',
        '"checked"': "all the parents of the packages checked in this workspace and are part of the target package's transitive dependencies"
    }
    recipes_description = ', '.join(map(lambda kv: '{} ({})'.format(kv[0], kv[1]), recipes.items()))
    parser.add_argument('--recipe', '-r', type=str, help='Recipe to use. Available recipes: {}'.format(recipes_description), required=True)

    args = parser.parse_args(args)
    package = Context.ws.current_package()

    dot_path = package.child_path('{}.dependencies.dot'.format(args.recipe))
    if args.recipe == 'all':
        DependencyGraph.all_packages(package).show(dot_path)
    elif args.recipe in "dra":
        DependencyGraph.filtered_by_prefix(package, "Dra").show(dot_path)
    elif args.recipe in "adn":
        DependencyGraph.filtered_by_prefix(package, "Adn").show(dot_path)
    elif args.recipe == 'checked':
        DependencyGraph.checked_packages_parents(package).show(dot_path)
    else:
        exit_with_error("No such recipe: '{}'. Available recipes: {}".format(args.recipe, recipes_description))


@subcmd(help="Runs a build target for building recursively and in parallel as much as possible while respecting the build order of all dependencies")
def bbr(parser, context, args):
    parser.add_argument('target', type=str, help='Build target to run. Examples: build, release etc (default: empty target, which defaults to build)', nargs='?', default=None)
    ArgParser.configure_multithreaded_command_parser(parser, default_num_threads=5)

    args = parser.parse_args(args)
    args.target = ' ' + args.target if args.target is not None else ''

    package = Context.ws.current_package()
    graph = DependencyGraph.all_packages(package)
    execution_utils.execute_on_dependency_graph('build{}'.format(args.target), graph, args.threads)

@subcmd('clean-logs', help='Cleans the logs older than the specified duration. Example: 1d34m represents a duration of 1 day and 34 minutes.')
def clean_logs(parser, context, args):
    parser.add_argument('--age', type=str, help='Maximum age for logs to retain', required=True)
    args = parser.parse_args(args)
    Context.ws.clean_bws_logs(args.age)


def _interactive_operation_on_unpushed_commits(parser, args, interactive_operation_factory):
    parser.add_argument('--branch', '-b', type=str, help='Name of the remote to compare against in order to detect unpushed local commits (default: mainline)', default='mainline')
    parser.add_argument('--skipCheck', help='Skip interactive check. The operation will be executed on all selected packages sequentially (default: false)', dest='skip_check', action='store_true')
    args = parser.parse_args(args)

    packages_with_changes = Context.ws.list_packages_with_unpushed_commits(args.branch)
    execution_utils.interactive_package_operation(interactive_operation_factory(args), packages_with_changes)


def _execute_on_all_packages_with_unstaged_changes(parser, cmd):
    packages_with_unstaged_changes = Context.ws.list_packages_with_unstaged_changes()

    if len(packages_with_unstaged_changes) > 0:
        Context.logs_root(force_create_new_root=True) # we had to run a command to find packages with changes, don't want to see these logs
        execution_utils.exec_for_all_packages(cmd, packages_with_unstaged_changes, DEFAULT_NUM_THREADS, log_level=LogLevel.execution_logs_only())

def main():
    try:
        arghandler = ArgumentHandler(use_subcommand_help=True)
        arghandler.run(sys.argv[1:])
    except ExecutionException as e:
        exit_if_task_failed(e.task_result)
    except (InvalidInputException, InvalidUsageException) as e:
        exit_with_error(e.message)
    except (InterruptedError, KeyboardInterrupt):
        print(bold(yellow('Interrupted by user')))
        exit(1) # exit with error to interrupt any && bash sequence


if __name__ == '__main__':
    main()
