import re
import functools
import operator

from arghandler import *
import cloudwatch
from cloudwatch import CloudwatchGrep, FetchLogOptions, QueryLogOptions, MAX_QUERY_RESULTS, MAX_FETCH_PAGE_SIZE
from cwgrep_logging import Logging
from program_utils import *
from time_utils import *
from formatting_utils import *
from utils import *
from cwgrep_config import Stages, Regions, Config

LOG_STREAMS = '--log-streams'
LATEST_STREAM = '--latest-stream'

DURATION_HELP = 'Example: 1d34m represents a duration of 1 day and 34 minutes.'
FILTER_PATTERN_SYNTAX_DOC = 'https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/FilterAndPatternSyntax.html'

@subcmd(help='Runs a filter query on a given log group and returns the results after writing them to a local file. This is equivalent to searching a log group in the Cloudwatch UI.')
def fetch(parser, context, args):
    config = Config.get()
    parser.add_argument('--service', type=str, help='Service name or alias', choices=config.all_services(), required=True)

    _configure_env_parser(parser)
    _configure_fetch_options_parser(parser)
    _configure_log_stream_selection_parser(parser)

    args = parser.parse_args(args)
    start, end = _parse_start_end(args.start, args.duration)

    service = config.get_service(args.service)
    log_group = args.known_log_group if args.known_log_group is not None else Config.get().default_log_group(service)

    options = __select_log_options(
        service=service,
        region=args.region,
        stage=args.stage,
        known_log_group=log_group,
        log_streams=args.log_streams,
        latest_stream=args.latest_stream,
        start=start,
        end=end,
        filter_pattern=args.filter_pattern,
        page_size=args.page_size,
        limit=args.limit,
        verbose=args.verbose,
    )

    cw = CloudwatchGrep(options.account)
    cw.fetch_logs(options)

    _log_activity('fetch', Logging.logs_file(),
                  log_goup=options.log_group.path,
                  is_specific_log_streams=len(args.log_streams) > 0,
                  is_latest_stream=args.latest_stream
    )
    Logging.tear_down()

@subcmd('fetch-multiple', help='Runs a filter query on the default log group of multiple services and aggregates the results in a single local file.')
def fetch_multiple(parser, context, args):
    _configure_env_parser(parser)
    _configure_multi_logs_selection(parser)
    _configure_fetch_options_parser(parser)

    args = parser.parse_args(args)

    region, stage, raw_log_groups = args.region, args.stage, args.log_groups
    start, end = _parse_start_end(args.start, args.duration)
    log_groups = _parse_multi_log_groups(raw_log_groups)

    services = [service for service, _ in log_groups]
    Logging.create_fetch_multi_logs_root(services, region, stage, start, end, args.verbose)

    log_files = {}
    for service, log_group in log_groups:
        key = f'{service.name}:{log_group}'
        Logging.log(f'Fetching logs for {key}...')

        options = __select_log_options(
            service=service,
            region=region,
            stage=stage,
            known_log_group=log_group,
            log_streams=None,
            latest_stream=None,
            start=start,
            end=end,
            filter_pattern=args.filter_pattern,
            page_size=args.page_size,
            limit=args.limit,
            verbose=args.verbose,
        )

        cw = CloudwatchGrep(options.account)
        log_files[key] = cw.fetch_raw_events(options)

    cloudwatch.sorted_raw_events_merge_from_multi_logs(log_files)

    _log_activity('fetch-multiple', Logging.multi_logs_file(), services_count=len(services))
    Logging.tear_down()

@subcmd(
    help='Allows running a filter query equivalent to the Cloudwatch Insights UI. Only returns 1000 results (Cloudwatch limitation) at most, but is '
         'useful for its friendly syntax for OR queries'
)
def query(parser, context, args):
    _configure_env_parser(parser)
    _configure_time_range_parser(parser)
    _configure_multi_logs_selection(parser)
    _configure_query_options_parser(parser)

    args = parser.parse_args(args)

    region, stage, log_groups = args.region, args.stage, args.log_groups
    start, end = _parse_start_end(args.start, args.duration)

    if args.limit > MAX_QUERY_RESULTS:
        raise InvalidInputException(f'Maximum limit is {MAX_QUERY_RESULTS} but {args.limit} was provided')

    log_groups_by_account = _group_log_groups_by_account(log_groups, region, stage)
    options = QueryLogOptions(log_groups_by_account, start, end, args.filter, args.limit)

    Logging.create_cross_service_query_logs_root(args.log_groups, region, stage, start, end, args.verbose)
    cloudwatch.query_multiple_accounts(options)

    _log_activity('query', Logging.multi_logs_file(),
                  accounts_count=len(log_groups_by_account.keys()),
                  log_groups_count=len(functools.reduce(operator.iconcat, log_groups_by_account.values(), [])),
                  has_or=' or @message' in options.query,
                  has_and=len(args.filter) > 0,
                  has_not=' @message not' in options.query
    )
    Logging.tear_down()

def _group_log_groups_by_account(raw_log_groups, region, stage):
    config = Config.get()
    log_groups = _parse_multi_log_groups(raw_log_groups)
    groups_by_account = {}

    for service, known_log_group in log_groups:
        account = config.get_account(service, stage, region)
        if account not in groups_by_account:
            groups_by_account[account] = []

        log_group = config.get_log_group(known_log_group, service, stage, region)
        groups_by_account[account].append(log_group.path)

    return groups_by_account

def _parse_multi_log_groups(raw_log_groups):
    config = Config.get()
    log_groups = []

    for raw_groups in raw_log_groups:
        flattened = re.split(r'[:,]', raw_groups)
        service = config.get_service(flattened[0])

        if len(flattened) == 1:
            flattened.append(config.default_log_group(service))

        log_groups += [(service, log_group) for log_group in flattened[1:]]

    return log_groups

def _configure_env_parser(parser):
    parser.add_argument('--region', type=str, help='Region in which the application is running', choices=Regions.ALL, required=True)
    parser.add_argument('--stage', type=str, help='Application stage', choices=Stages.ALL, required=True)

def _configure_log_stream_selection_parser(parser):
    parser.add_argument('--known-log-group', '-klg', type=str, help='[optional] Known log group to query for, referred to by its short name. Defaults to the service\'s default log group, if any.', default=None, choices=Config.get().all_log_groups())
    parser.add_argument(LOG_STREAMS, nargs='+', help='[optional] Space-separated list of log streams to search through. If not specified, will default to all streams. Mutually exclusive with %s.' % LATEST_STREAM, default=[])
    parser.add_argument(LATEST_STREAM, help='[optional] If set to true, will only fetch data for the stream with the latest event log. Mutually exclusive with %s.' % LOG_STREAMS, action='store_true')

def _configure_fetch_options_parser(parser):
    _configure_time_range_parser(parser)
    parser.add_argument('--filter-pattern', '-fp', type=str, help=(
        '[optional] Filter pattern that will be executed by Cloudwatch. If none is supplied, all log events '
        f' within the selected time range will be fetched. For the pattern syntax, see {FILTER_PATTERN_SYNTAX_DOC}.'
        ' IMPORTANT: to prevent bash from interpreting double-quotes, you should add simple quotes around it: -fp \'"my-uu-id"\'.'
    ), default='')
    parser.add_argument('--page-size', '-ps', help='[optional] Sets the initial page size. The page size might be increased automatically as the number of pages grows in order to retrieve all the data faster. Will never exceed %s.' % MAX_FETCH_PAGE_SIZE, type=int, default=500)
    parser.add_argument('--limit', '-l', help='[optional] Sets the maximum number of records to retrieve. If unspecified, all matching records will be fetched. Note that regardless of the limit, Cloudwatch will paginate anytime the size of a page exceeds 1 MB.', type=int, default=None)
    _configure_verbosity(parser)

def _configure_query_options_parser(parser):
    parser.add_argument('--filter', '-f', action='append', help=(
        '[optional] Simplified filter expression that will be converted into a format CloudWatch understand. Use | to separate OR patterns, ! for '
        'excluding a given pattern, and add multiple filters to implement an AND operation. Example: -f \'foo|bar\' -f !baz will keep all lines '
        'not containing baz and containing one of foo or bar. Defaults to selecting everything when no filter is provided.'
    ), default=[])
    parser.add_argument('--limit', '-l', type=int, default=MAX_QUERY_RESULTS, help=(
        f'[optional] Sets the maximum number of records to retrieve (maximum {MAX_QUERY_RESULTS}). If unspecified, '
        f'up to {MAX_QUERY_RESULTS} all matching records will be fetched.'
    ))
    _configure_verbosity(parser)

def _configure_time_range_parser(parser):
    parser.add_argument('--start', '-s', type=str, help='Start of the time range to fetch, as an ISO timestamp (e.g. \'2020-01-06T11:30:00Z\') or as a duration from the current time (e.g. \'2h ago\').', required=True)
    parser.add_argument('--duration', '-d', type=str, help=('[optional] Duration to fetch. If not specified, all logs until current time will be fetched. %s' % DURATION_HELP), required=False)

def _configure_multi_logs_selection(parser):
    config = Config.get()
    parser.add_argument('--log-groups', '-lg', nargs='+', required=True, help=(
            'Space-separated list of log group selector. The format for a selector is [service]:[log group 1],[log group 2],etc. '
            '[service] is also a valid selector, it is equivalent to [service]:[defaultLogGroup], if a default exists. '
            'Available services: {}. '.format(', '.join(config.all_services())) +
            'Available groups: {}. '.format(', '.join(config.all_log_groups())) +
            'Example: -lg conf hp:test-notif,test-notif-audit'
    ))

def _configure_verbosity(parser):
    parser.add_argument('--verbose', '-v', help='[optional] Activates additional debug logging.', action='store_true', default=False)

def _parse_start_end(start, duration):
    now = utc_now()
    start = parse_past_duration_or_iso_string(now, start)
    end = now if duration is None else start + parse_duration(duration)
    return start, end

def __select_log_options(service, region, stage, known_log_group, log_streams, latest_stream, start, end, filter_pattern, page_size, limit, verbose):
    config = Config.get()
    account = config.get_account(service, stage, region)
    log_group = config.get_log_group(known_log_group, service, stage, region)

    Logging.create_fetch_logs_root(service, region, stage, log_group, start, end, verbose)
    # useful to help remember exactly what was executed
    Logging.debug('Executing command: ', _get_full_command())

    if limit is not None and limit <= 0:
        raise InvalidInputException('--limit should be at least 1 but was: ' + limit)

    log_streams = __compute_required_log_streams(account, log_streams, latest_stream, log_group)
    return FetchLogOptions(account, log_group, log_streams, start, end, filter_pattern, page_size, limit)

def __compute_required_log_streams(account, log_streams, latest_stream, log_group):
    if latest_stream and log_streams:
        raise InvalidUsageException('%s and %s are mutually exclusive flags' % (LATEST_STREAM, LOG_STREAMS))

    if latest_stream:
        cw = CloudwatchGrep(account)
        return [cw.get_latest_log_stream(log_group)]

    if log_streams:
        return log_streams

    return []

@subcmd('clean-logs', help='Cleans the logs generated by this tool over time that are older than the specified duration')
def clean_logs(parser, context, args):
    parser.add_argument('--age', type=str, help='Maximum age for logs to retain. %s' % DURATION_HELP, required=True)
    args = parser.parse_args(args)
    Logging.clean_logs(parse_duration(args.age))

@subcmd(help='Provides detailed help about a given sub-command of this tool, with indications as to when to use it over another one, as well as usage examples.')
def tutorial(parser, context, args):
    parser.add_argument('command', type=str, metavar='command', help='Command name', choices=['fetch', 'fetch-multiple', 'query'])
    args = parser.parse_args(args)

    command = args.command
    if command == 'query':
        print(Tutorial(
            command_name=command,
            description=f'This command is equivalent to a Cloudwatch Insights query in the AWS console for several accounts.',
            recommended_use_case=
            'This is the recommended command for typical cross-account queries or any query requiring multiple log groups per account, or using OR filter patterns. '
            f"""{yellow("Be mindful that due to Cloudwatch's limitations, this command will not return more than 10,000 log events, which makes it a bad command to use for queries yielding large amounts of log events")}""",
            features=[
                'query multiple log groups from multiple accounts at once',
                'add multiple positive and negative filters (AND relationship)',
                'filters of the form filter1 OR filter2'
            ],
            unsupported_features=[
                'query specific log streams in a given log group',
                'using different filters for each selected account. The filters must be compatible with all included log groups.',
                'returning more than 10,000 results per account. Not suitable for large queries.',
                'cross-region or cross-stage queries'
            ],
            usage_examples=[
                UsageExample('find all failure metrics from 2 example accounts except for one operation', "cwgrep query --region pdx --stage beta -lg mm:querylog pm:querylog -s '5h ago' -f Failure -f '!ExcludedOperation'"),
                UsageExample('query default log groups from several example services and a specific mission id', "cwgrep query --region pdx --stage beta -lg mm pm mr -s '1h ago' -f my-uu-id"),
                UsageExample('query multiple log groups for several example services, for a specific duration', "cwgrep query --region pdx --stage beta -lg mm:integ,apollo e2e pm:app -s '1h ago' -d 30m"),
            ]
        ))
    elif command == 'fetch-multiple':
        print(Tutorial(
            command_name=command,
            description=f'This command is equivalent to a log group search in the AWS console for several accounts. For more information about the syntax, please check {FILTER_PATTERN_SYNTAX_DOC}.',
            recommended_use_case=
            'Sufficient for most cross-account simple queries. For a single account, fetch is slightly preferable as you can start reading the logs as soon as the first page is '
            'ready instead of waiting for all pages. Suitable for retrieving large amounts of logs (as much as you have space on your disk...).',
            features=[
                'query multiple log groups from multiple accounts at once (but one query per log group instead of per account for the "query" command)',
                'add multiple positive and negative filters (AND relationship)',
                'returning any amount of log events (suitable for large queries)'
            ],
            unsupported_features=[
                'selecting which exact log streams to query',
                'using different filters for each selected account. The filters must be compatible with all included log groups.',
                'filters of the form filter1 OR filter2',
                'cross-region or cross-stage queries'
            ],
            usage_examples=[
                UsageExample('retrieve logs from two example services for a given mission and request id', "cwgrep fetch-multiple --region pdx --stage beta -lg mm pm -s '5h ago' -fp '\"my-mission-uuid\" \"my-request-uuid\"'"),
                UsageExample('fetch cross-service logs for a given class, excluding an operation', "cwgrep fetch-multiple --region pdx --stage beta -lg mm pm -s '5h ago' -fp 'CoralActivityTemplate -ExcludedOperation'"),
            ]
        ))
    elif command == 'fetch':
        print(Tutorial(
            command_name=command,
            description=f'This command is equivalent to a log group search in the AWS console for a single account . For more information about the syntax, please check {FILTER_PATTERN_SYNTAX_DOC}.',
            recommended_use_case=
            'Sufficient for most simple queries, it allows having quick feedback instead of waiting for the entire response, contrarily to other commands. Like fetch-multiple, it is well-suited '
            'for retrieving large amounts of logs. Finally, this is also the only command allowing to query the latest log stream, or a specific log stream (e.g. logs from a single host).',
            features=[
                'query all log streams from a single log group of a single account',
                'query a subset of log streams from a single log group of a single account',
                'query the latest log stream from a single log group of a single account',
                'add multiple positive and negative filters (AND relationship)',
                'returning any amount of log events (suitable for large queries)'
            ],
            unsupported_features=[
                'cross-account queries',
                'filters of the form filter1 OR filter2'
            ],
            usage_examples=[
                UsageExample('filter by multiple criteria', "cwgrep fetch --region pdx --stage beta --service mm -s '2020-01-06T13:00:00Z' -d 2h -fp '{}' '{}'".format('"filter 1"', '"filter2"')),
                UsageExample('count number of occurrences for each exception type and sort by frequency', "cwgrep fetch --region pdx --stage beta --service mm -s '5h ago' -fp Exception | sort | uniq -c | sort -t1 -nr"),
                UsageExample('fetch logs from a non-default log group', "cwgrep fetch --region pdx --stage beta --service mm -s '5h ago' -klg integ"),
                UsageExample('fetch logs from the latest log stream', "cwgrep fetch --region pdx --stage beta --service mm -s '5h ago' --latest-stream"),
                UsageExample('fetch the last log stream for end-to-end tests', "cwgrep fetch --region pdx --stage beta --service e2e -s '5h ago' --latest-stream"),
                UsageExample('fetch application logs from a specific host', "cwgrep fetch --region pdx --stage beta --service pm -s '5h ago' --log-streams ip-10-0-62-130"),
                UsageExample('fetch logs from a for a given class, excluding an operation', "cwgrep fetch-multiple --region pdx --stage beta --service mm pm -s '5h ago' -fp 'CoralActivityTemplate -ExcludedOperation'"),
            ]
        ))
    else:
        raise InvalidInputException(f'There is no tutorial entry for this command: {command}')


class Tutorial:
    def __init__(self, command_name, description, recommended_use_case, features, unsupported_features, usage_examples):
        self.command_name = command_name
        self.description = description
        self.recommended_use_case = recommended_use_case
        self.features = features
        self.unsupported_features = unsupported_features
        self.usage_examples = usage_examples

    def __str__(self):
        return (
            f'{bold("Description:")} {self.description}\n'
            f'{bold("Recommended use case:")} {self.recommended_use_case}\n'
            f'{self._format_helper_enumeration("Features", self.features)}\n'
            f'{self._format_helper_enumeration("Unsupported features", self.unsupported_features)}\n'
            f'{self._format_helper_enumeration("Examples", self.usage_examples)}\n'
            f'\nFor more details about the syntax and usage of each option, please run "cwgrep {self.command_name} -h".'
        )

    def _format_helper_enumeration(self, header, enum):
        joiner = '\n\t- '
        return (
            f'{bold(header)}:'
            f'{joiner}{joiner.join([str(item) for item in enum])}'
        )

class RecommendedUseCase:
    def __init__(self, description):
        self.description = description

    def __str__(self):
        return self.description

class UsageExample:
    def __init__(self, description, cmd):
        self.description = description
        self.cmd = cmd

    def __str__(self):
        return f'{self.description}: {cyan(self.cmd)}'

# Deactivate until I find a new way to give all the team permissions to access the account
def _log_activity(command_name, output_file, **kwargs):
    # user_behaviour = UserBehaviorLogger(Accounts.PERSONAL_COURTINO)
    # user_behaviour.log_activity(command_name, _get_full_command(), logs_size_bytes=os.path.getsize(output_file), **kwargs)
    pass

def _get_full_command():
    return f'cwgrep {" ".join(sys.argv[1:])}'

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