import os
import file_utils
import sys

from multiprocessing import RLock
from time_utils import *
from formatting_utils import *

OUTPUT = 'logs.txt'
NORMALIZED_QUERY_RESULTS = 'normalized-query-results.json'

class Logging:
    _lock = RLock()
    _multi_logs_root = None
    _logs_root = None
    _stdout = None
    _debug = False

    @staticmethod
    def create_cross_service_query_logs_root(raw_log_groups, region, stage, start, end, debug=False):
        Logging._create_multi_logs_root('-'.join(raw_log_groups).replace(':', '_').replace(',', '-'), region, stage, start, end, debug)

    @staticmethod
    def create_fetch_multi_logs_root(services, region, stage, start, end, debug=False):
        services_aliases = [service.alias for service in services]
        Logging._create_multi_logs_root('-'.join(services_aliases), region, stage, start, end, debug)

    @staticmethod
    def _create_multi_logs_root(prefix, region, stage, start, end, debug=False):
        Logging._lock.acquire()
        if Logging._multi_logs_root is not None:
            raise Exception('Can only create one multi-logs root, but {} was already present'.format(Logging._multi_logs_root))

        desc = '{}-{}-{}-{}'.format(prefix, region, stage, Logging._pretty_period(start, end))
        parent = os.path.join(Logging._main_root(), desc)
        Logging._multi_logs_root = file_utils.create_random_folder(parent)
        Logging._create_stdout_if_needed(debug)

        Logging._lock.release()
        Logging._log_output_files_location(Logging.multi_logs_root(), Logging.multi_logs_file())

    @staticmethod
    def create_query_logs_root(account, start, end, debug=False):
        return Logging._create_logs_root(account.name, start, end, debug)

    @staticmethod
    def create_fetch_logs_root(service, region, stage, log_group, start, end, debug=False):
        return Logging._create_logs_root('{}-{}-{}-{}' .format(service.alias, region, stage, log_group.short_name), start, end, debug)

    @staticmethod
    def _create_logs_root(prefix, start, end, debug=False):
        Logging._lock.acquire()

        file_suffix = '{}-{}' .format(prefix, Logging._pretty_period(start, end))
        parent = os.path.join(Logging._main_root(), file_suffix)
        Logging._logs_root = file_utils.create_random_folder(parent)
        Logging._create_stdout_if_needed(debug)

        # we don't merge raw logs individually per service when fetching from multiple services
        if not Logging.is_multi_logs():
            open(Logging.logs_file(), 'a').close() # create empty file so that users can start tailing the file right away

        Logging._lock.release()

        # we don't log this info to the console when fetching from multiple services because it's verbose and redundant with logs from create_multi_logs_root
        Logging._log_output_files_location(Logging.logs_root(), Logging.logs_file(), skip_stdout=Logging.is_multi_logs())

    @staticmethod
    def _pretty_period(start, end):
        formatted_start = to_iso_string(start)
        duration = to_pretty_duration(dt.timedelta(milliseconds=to_epoch_millis(end) - to_epoch_millis(start)))
        return '{}-from-{}'.format(duration, formatted_start).replace(':', '_')

    @staticmethod
    def _log_output_files_location(root, output_logs, skip_stdout=False):
        Logging.log('Created root folder for output logs, raw event logs and cwgrep execution logs:', bold(root), skip_stdout=skip_stdout)
        Logging.log('Fetched log messages will be stored in:', bold(output_logs), skip_stdout=skip_stdout)

    @staticmethod
    def log(*args, skip_stdout=False):
        if not skip_stdout:
            print(*args, file=sys.stderr)
        Logging.stdout().write(' '.join(args) + '\n')
        Logging.stdout().flush()

    @staticmethod
    def debug(*args):
        # always write debug logs to the log file but conditionally to the console
        Logging.log(*['[debug]', *args], skip_stdout=not Logging._debug)

    @staticmethod
    def multi_logs_file():
        return os.path.join(Logging.multi_logs_root(), OUTPUT)

    @staticmethod
    def logs_file():
        return os.path.join(Logging.logs_root(), OUTPUT)

    @staticmethod
    def normalized_query_results_file():
        return os.path.join(Logging.logs_root(), NORMALIZED_QUERY_RESULTS)

    @staticmethod
    def page_file(page_index):
        Logging._lock.acquire()

        raw_logs_root = os.path.join(Logging.logs_root(), 'raw')
        os.makedirs(raw_logs_root, exist_ok=True)

        Logging._lock.release()

        page_file_name = 'raw.json.{}'.format(page_index)
        return os.path.join(raw_logs_root, page_file_name)

    @staticmethod
    def execution_log_file():
        root = Logging.multi_logs_root() if Logging.is_multi_logs() else Logging.logs_root()
        return os.path.join(root, 'cwgrep.log')

    @staticmethod
    def clean_logs(max_age):
        file_utils.delete_old_files_under(Logging._main_root(), max_age)

    @staticmethod
    def multi_logs_root():
        if not Logging.is_multi_logs():
            raise Exception('No multi logs root was created')
        return Logging._multi_logs_root

    @staticmethod
    def logs_root():
        return Logging._logs_root

    @staticmethod
    def stdout():
        return Logging._stdout

    @staticmethod
    def tear_down():
        Logging._multi_logs_root = None
        return Logging.stdout().close()

    @staticmethod
    def is_multi_logs():
        return Logging._multi_logs_root is not None

    @staticmethod
    def _create_stdout_if_needed(debug):
        if Logging._stdout is None:
            Logging._stdout = open(Logging.execution_log_file(), 'w')
            Logging._debug = debug

    @staticmethod
    def _main_root():
        base = os.path.join(os.getenv("HOME"), 'cwgrep-logs')
        return os.path.join(base, Logging.multi_logs_root()) if Logging.is_multi_logs() else base
