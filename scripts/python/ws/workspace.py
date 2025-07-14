import json
import json
import os
import os.path
import re
import subprocess as sp
from collections import OrderedDict

import execution_utils
import file_utils
from formatting_utils import bold, yellow, green, red, LogLevel
from program_utils import exit_with_error
from time_utils import parse_duration
from utils import InvalidInputException, ExecutionException, InvalidUsageException, DEFAULT_NUM_THREADS


class Package:
    _RELATE_DEBUG_ADVICE = "Your packages were pushed but not related. You can try relating them manually using 'ws relate'."
    _GIT_REF_PATTERN = re.compile('HEAD(~(?P<ref>\d+))?')

    @staticmethod
    def relate_package_heads(packages):
        commits = [{'package': package.name, 'branch': package.branch(), 'commit_id': package.head()} for package in packages]
        Package.relate_commits(commits)

    @staticmethod
    def relate_commits(commits):
        Package._relate_commits(Package._parse_commits(commits))

    def __init__(self, name):
        self.name = name

    def open_stdout(self):
        return open(self.stdout_path(), 'a')

    def open_stderr(self):
        return open(self.stderr_path(), 'a')

    def stdout_path(self):
        return os.path.join(self.logs_root(), 'stdout')

    def stderr_path(self):
        return os.path.join(self.logs_root(), 'stderr')

    def logs_root(self):
        pkg_logs_root = os.path.join(Context.logs_root(), self.name)
        os.makedirs(pkg_logs_root, exist_ok=True)
        return pkg_logs_root

    def path(self):
        return os.path.join(Context.ws.root(), 'src', self.name)

    def child_path(self, *path):
        return os.path.join(self.path(), *path)

    def exists(self):
        return os.path.exists(self.path())

    def head(self):
        return self.commit_id('HEAD')

    def commit_id(self, ref, branch=None):
        expected_format = Package._GIT_REF_PATTERN
        match = re.fullmatch(expected_format, ref)
        if match is None:
            raise InvalidInputException('Commit reference {} should have the following format: {}'.format(ref, expected_format.pattern))

        if branch is not None:
            ref = ref.replace('HEAD', branch)

        task_result = execution_utils.exec_cmd('get_commit_id.sh {}'.format(ref), stdout=sp.PIPE, cwd=self.path(), raise_on_failure=True)
        return task_result.result().strip()

    def branch(self):
        task_result = execution_utils.exec_cmd('get_branch.sh', stdout=sp.PIPE, cwd=self.path(), raise_on_failure=True)
        return task_result.result().strip()

    def _check_valid_commit_id(self, commit_id, branch):
        task_result = execution_utils.exec_cmd('find_branch_containing_commit.sh {}'.format(commit_id), stdout=sp.PIPE, cwd=self.path(), raise_on_failure=True)
        branch_with_commit = task_result.result().strip()
        if branch_with_commit != branch:
            raise InvalidInputException('Commit {} exists on branch {} but not on {}', commit_id, branch_with_commit, branch)

    @staticmethod
    def _parse_commits(commits):
        if type(commits) == str:
            commits_parts = commits.split(',')
            if any(len(commit) == 0 for commit in commits_parts):
                raise InvalidInputException('"{}" should not have any empty commit part (comma-separated)'.format(commits))

            parsed = []
            for commit in commits_parts:
                parts = commit.split(':')
                if len(parts) > 3:
                    raise InvalidInputException('unexpected number of parts in {} (colon-separated)'.format(commit))

                package = Package(parts[0])
                branch = parts[2] if len(parts) > 2 else package.branch()
                commit_id = Package._resolve_commit(package, parts[1], branch) if len(parts) > 1 else package.head()
                parsed.append({'package': package.name, 'commit_id': commit_id, 'branch': branch})

            return parsed
        elif type(commits) == list:
            return commits
        else:
            raise Exception("Unsupported type for parameter 'commits': {}".format(type(commits)))

    @staticmethod
    def _resolve_commit(package, commit_ref, branch):
        if commit_ref.startswith('HEAD'):
            return package.commit_id(commit_ref, branch=branch)

        package._check_valid_commit_id(commit_ref, branch)
        return commit_ref

    @staticmethod
    def _relate_commits(commits):
        print(yellow('Relating commits... (Currently a noop)'))
        pass

    def __eq__(self, other):
        return self.name == other.name

    def __hash__(self):
        return hash(self.name)

    def __str__(self):
        return self.name

    def __repr__(self):
        return self.name


class Workspace:
    def __init__(self):
        self._root = None
        self._checked_packages = None

    def root(self):
        if self._root is None:
            self._root = self._find_workspace_root()

        return self._root

    @staticmethod
    def _find_workspace_root():
        return Workspace._find_root_containing('workspaceInfo', 'Workspace')

    def current_package(self):
        root = self._find_package_root()
        return Package(root[len(os.path.join(self.root(), 'src')) + 1:])

    def _find_package_root(self):
        return Workspace._find_root_containing('Config', 'package')

    @staticmethod
    def _find_root_containing(marker_file, description):
        original_cwd = os.getcwd()

        while not os.path.exists(marker_file) or not os.path.isfile(marker_file):
            cwd = os.getcwd()
            os.chdir(os.pardir)
            new_cwd = os.getcwd()

            if cwd == new_cwd:
                raise InvalidUsageException("Could not find {}'s root folder".format(description))

        root = os.getcwd()
        os.chdir(original_cwd)

        return root

    def checked_packages(self, force_refresh=False):
        if self._checked_packages is None or force_refresh:
            self._checked_packages = self._list_checked_packages()

        checked_packages = self._checked_packages

        return checked_packages

    def _list_checked_packages(self):
        pkg_root = os.path.join(self.root(), 'src')
        if not os.path.exists(pkg_root):
            return []

        packages = {Package(package) for package in sorted(os.listdir(pkg_root)) if os.path.exists(os.path.join(pkg_root, package, 'Config'))}
        return packages

    def list_packages_with_unpushed_commits(self, default_remote_branch):
        print('Looking for packages with unpushed local commits...')

        command = 'count_unpushed_commits.sh {}'.format(default_remote_branch)
        task_results = execution_utils.exec_for_all_packages(command, self.checked_packages(), DEFAULT_NUM_THREADS, log_level=LogLevel.quiet())

        if all(task_result.is_success() for task_result in task_results):
            packages_with_changes = [task_result.package for task_result in task_results if int(task_result.result()) > 0]

            print('Found {} package(s) with unpushed local commits'.format(len(packages_with_changes)))
            return packages_with_changes
        else:
            exit_with_error("Couldn't count changes between HEAD and {} for some packages, so stopping there. Check the logs to address the problem and run this command again.".format(remote_branch))

    def list_packages_with_unstaged_changes(self):
        print('Looking for packages with unstaged changes...')

        task_results = execution_utils.exec_for_all_packages('has_unstaged_changes.sh', self.checked_packages(), DEFAULT_NUM_THREADS, log_level=LogLevel.quiet())
        if all(task_result.is_success() for task_result in task_results):
            packages_with_changes = [task_result.package for task_result in task_results if task_result.result().strip() == 'changes']

            print('Found {} package(s) with unstaged changes'.format(len(packages_with_changes)))
            return packages_with_changes
        else:
            exit_with_error("Couldn't determine if there were local changes or not for some packages, so stopping there. Check the logs to address the problem and run this command again.")

    def sync_package_info(self):
        # refresh in case there are new packages
        self.checked_packages(force_refresh=True)
        package_info = os.path.join(self.root(), 'workspaceInfo')

        with open(package_info, 'r') as file:
            content = file.read()

        with open(package_info, 'w') as file:
            packages = "".join(map(lambda x: "\n  {}-{} = .;".format(x[0], x[1]), self._package_versions().items()))
            matches = re.findall(re.compile(r"packages = {(.*)}", re.DOTALL), content)

            if len(matches) > 1:
                raise Exception("Failed to parse package info {}".format(content))

            file.write(content.replace(matches[0], packages + "\n"))

    def _package_versions(self):
        return OrderedDict(sorted({pkg.name: self._package_version(pkg) for pkg in self.checked_packages()}.items()))

    def _package_version(self, package):
        with open(package.child_path('Config'), 'r') as config_file:
            whole_config = re.sub(r"\s", "", config_file.read())
            interfaces = re.findall(r"interfaces=\((.+)\);", whole_config)

            if len(interfaces) == 0:
                raise Exception("Could not find package version for {}".format(package))

            if len(interfaces) > 1:
                raise Exception("Found multiple package versions for {}".format(package))

            return interfaces[0]

    def clean_bws_logs(self, max_age_str):
        logs_root = os.path.join(self.root(), 'bws', 'logs')
        file_utils.delete_old_files_under(logs_root, parse_duration(max_age_str))

class Context:
    ws = Workspace()

    _logs_root = None

    @staticmethod
    def logs_root(force_create_new_root=False):
        if Context._logs_root is None or force_create_new_root:
            Context._logs_root = file_utils.create_time_based_folder(os.path.join(Context.ws.root(), 'bws', 'logs'))

        return Context._logs_root

    @staticmethod
    def package_logs_root(package):
        pkg_logs_root = os.path.join(Context.logs_root(), package)
        os.makedirs(pkg_logs_root, exist_ok=True)
        return pkg_logs_root
