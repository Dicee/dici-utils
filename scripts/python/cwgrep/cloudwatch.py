import heapq
import json
import os
import re
import subprocess as sp

import boto3
import aws

from sts import ACCESS_KEY_ID, SECRET_ACCESS_KEY, SESSION_TOKEN, STSCredentialsStore
from cwgrep_logging import Logging
from formatting_utils import *
from program_utils import *
from time_utils import *

UNESCAPED_PIPE = re.compile(r'(?<!\\)\|')
UNESCAPED_LEADING_EXCL_MARK = re.compile(r'^\s*(?<!\\)!')

# common fields
LOG_GROUP_NAME = 'logGroupName'
START_TIME = 'startTime'
END_TIME = 'endTime'
LIMIT = 'limit'

# query
MAX_QUERY_RESULTS = 10_000
TIME_FORMAT = '%Y-%m-%d %H:%M:%S.%f'

## request
QUERY_ID = 'queryId'
LOG_GROUP_NAMES = 'logGroupNames'
QUERY_STRING = 'queryString'

Q_LOG_STREAM = '@logStream'
Q_MESSAGE = '@message'
Q_TIMESTAMP = '@timestamp'

## response
STATUS = 'status'
SCHEDULED = 'Scheduled'
RUNNING = 'Running'
COMPLETE = 'Complete'

RESULTS = 'results'
STATISTICS = 'statistics'
RECORDS_SCANNED = 'recordsScanned'
RECORDS_MATCHED = 'recordsMatched'
BYTES_SCANNED = 'bytesScanned'
VALUE = 'value'

# filter-log-events
MAX_FETCH_PAGE_SIZE = 10_000

## response
LOG_STREAM_NAMES = 'logStreamNames'
FILTER_PATTERN = 'filterPattern'
EVENTS = 'events'
MESSAGE = 'message'
TIMESTAMP = 'timestamp'
NEXT_TOKEN = 'nextToken'

# describe-log-streams

## request
DESCENDING = 'descending'
ORDER_BY = 'orderBy'

## response
LOG_STREAM_NAME = 'logStreamName'
LOG_STREAMS = 'logStreams'

class CloudwatchGrep:
    def __init__(self, account):
        credentials = STSCredentialsStore().get(account)
        session = boto3.Session(
            aws_access_key_id=credentials[ACCESS_KEY_ID],
            aws_secret_access_key=credentials[SECRET_ACCESS_KEY],
            aws_session_token=credentials[SESSION_TOKEN]
        )
        self._client = session.client('logs', region_name=account.region)

    def fetch_logs(self, options):
        # we append the pages to the output while going through the pages rather than at the end to give some data to the user as soon as possible
        raw_log_files = self.__fetch_all_pages(options, lambda page_file: self._append_event_messages_from(page_file))
        _pipe_logs_to_less(Logging.logs_file())
        return raw_log_files

    def _append_event_messages_from(self, page_file):
        with open(page_file, 'r') as response_dump, open(Logging.logs_file(), 'a') as logs:
            response = json.load(response_dump)
            for event in response[EVENTS]:
                logs.write(event[MESSAGE].strip())
                logs.write('\n')

    def fetch_raw_events(self, options):
        return self.__fetch_all_pages(options)

    def __fetch_all_pages(self, options, page_file_consumer=None):
        page = -1
        next_token = None
        total_fetched = 0
        max_results = options.max_results if options.max_results else float('inf')
        raw_log_files = []

        while (page == -1 or next_token is not None) and total_fetched < max_results:
            page += 1

            page_size = min(max_results - total_fetched, self.__update_page_size(options.initial_page_size, page))
            Logging.log(yellow('Fetching at most {} raw events for page {} to local file...'.format(page_size, page)))

            page_file = Logging.page_file(page)
            tmp_page_file = page_file + '.tmp'
            raw_log_files.append(page_file)

            with open(tmp_page_file, 'w') as tmp:
                response = self._do_filter_log_events(options, next_token, page_size)
                json.dump(response, tmp)

            next_token, event_count = self.__sort_raw_events_and_get_next_token(tmp_page_file, page_file)
            total_fetched += event_count # can get less than the page size

            if page_file_consumer is not None:
                page_file_consumer(page_file)

        return raw_log_files

    def _do_filter_log_events(self, options, next_token, page_size):
        log_group_name = options.log_group.path

        request = {
            LOG_GROUP_NAME: log_group_name,
            START_TIME: options.start_epoch(),
            END_TIME: options.end_epoch(),
            FILTER_PATTERN: options.filter_pattern,
            LIMIT: page_size
        }

        has_log_streams = len(options.log_streams) > 0
        if has_log_streams:
            request[LOG_STREAM_NAMES] = options.log_streams

        if next_token:
            request[NEXT_TOKEN] = next_token

        return aws.execute_sdk_request('FilterLogEvents', request, lambda req: self._client.filter_log_events(**req))

    # flat page size for the first few pages, then exponential growth to avoid retrieving gazillions of small pages for large queries
    def __update_page_size(self, initial_page_size, page):
        cut_off_point = 2 # initiate the exponential growth (strictly) after the third page
        return min(MAX_FETCH_PAGE_SIZE, initial_page_size if page <= cut_off_point else 2 ** (page - cut_off_point) * initial_page_size)

    # Cloudwatch doesn't guarantee it returns strictly sorted events when scanning multiple log streams (e.g. EC2 instances running in parallel)
    def __sort_raw_events_and_get_next_token(self, tmp_page_file, page_file):
        with open(tmp_page_file, 'r') as tmp, open(page_file, 'w') as output:
            response = json.load(tmp)
            events = response[EVENTS]
            events.sort(key=lambda event: event[TIMESTAMP])
            json.dump(response, output)

            next_token = None if NEXT_TOKEN not in response else response[NEXT_TOKEN]

        os.remove(tmp_page_file)
        return next_token, len(events)

    def get_latest_log_stream(self, log_group):
        request = {
            LOG_GROUP_NAME: log_group.path,
            ORDER_BY: 'LastEventTime',
            DESCENDING: True,
            LIMIT: 1
        }

        response = aws.execute_sdk_request('DescribeLogStreams', request, lambda req: self._client.describe_log_streams(**req))
        if LOG_STREAMS not in response or len(response[LOG_STREAMS]) == 0:
            exit_with_error('Could not find any log stream for log group {}'.format(log_group))
        return response[LOG_STREAMS][0][LOG_STREAM_NAME]

    def start_query(self, options):
        request = {
            LOG_GROUP_NAMES: options.log_groups,
            START_TIME: options.start_epoch(),
            END_TIME: options.end_epoch(),
            QUERY_STRING: options.query,
        }
        return aws.execute_sdk_request('StartQuery', request, lambda req: self._client.start_query(**req))

    # https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_GetQueryResults.html
    def get_query_results(self, running_query):
        request = {QUERY_ID: running_query[QUERY_ID]}
        return aws.execute_sdk_request('GetQueryResults', request, lambda req: self._client.get_query_results(**req))

def sorted_raw_events_merge_from_multi_logs(multi_logs):
    log_lines_heap = []
    for source, pages in multi_logs.items():
        continuation = _continuation(_lazy_raw_messages_with_timestamp(pages, source))
        if continuation is not None:
            log_lines_heap.append(continuation)

    heapq.heapify(log_lines_heap)

    logs_file = Logging.multi_logs_file()
    with open(logs_file, 'w') as output:
        while len(log_lines_heap) > 0:
            _, line, remaining_lines = heapq.heappop(log_lines_heap)

            output.write(line)
            output.write('\n')

            continuation = _continuation(remaining_lines)
            if continuation is not None:
                heapq.heappush(log_lines_heap, continuation)

    _pipe_logs_to_less(logs_file)

# returns a tuple of the first element of a lazy generator and the generator itself, or None if the generator is empty.
# This is conceptually equivalent to a buffered iterator with a buffer size of 1. More on the concept of continuation:
# https://en.wikipedia.org/wiki/Continuation#First-class_continuations
def _continuation(lazy_gen):
    head = next(lazy_gen, None)
    return (*head, lazy_gen) if head is not None else None

# returning the timestamp of log events as the first item of a tuple allows using this item to order elements in a Python heap
def _lazy_raw_messages_with_timestamp(pages, source):
    # open the ordered pages one at a time to avoid blowing our memory up (single JSON for all the events in a page, so can't stream
    # through the JSON). We could instead have files that contain the events as JSON lines, that's a future improvement.
    for file in pages:
        with open(file, 'r') as f:
            for event in json.load(f)[EVENTS]:
                timestamp = parse_epoch_seconds(event[TIMESTAMP])
                message = event[MESSAGE]
                yield (timestamp, (cyan('[{}]'.format(source)) + ' ' + message))

def query_multiple_accounts(options):
    multi_logs = {}

    for account in options.log_groups_by_account.keys():
        cw = CloudwatchGrep(account)
        query_options = options._for_single_account(account)
        running_query = cw.start_query(query_options)

        result_file = _synchronous_get_query_results(account, cw, query_options, running_query)
        multi_logs[account] = [result_file]

    sorted_raw_events_merge_from_multi_logs(multi_logs)

def _synchronous_get_query_results(account, cw, query_options, running_query):
    status = SCHEDULED
    while status in (RUNNING, SCHEDULED):
        response = cw.get_query_results(running_query)
        status = response[STATUS]
        if status == RUNNING:
            time.sleep(0.15)
        elif status == COMPLETE:
            stats = response[STATISTICS]

            Logging.log(
                '\n{} {}\n{} {}\n{}\n{}\n{} records scanned = {}, records matched = {}, bytes scanned = {}'.format(
                    bold('Account:'), account,
                    bold('Log groups:'), query_options.log_groups,
                    bold('Query:'), query_options.query,
                    bold('Stats:'), stats[RECORDS_SCANNED], stats[RECORDS_MATCHED], stats[BYTES_SCANNED])
            )

            return _write_query_results(account, query_options, response)
        elif status != SCHEDULED:
            exit_with_error('ERROR: query {} failed with status {}'.format(running_query[QUERY_ID], status))

def _write_query_results(account, query_options, response):
    Logging.create_query_logs_root(account, query_options.start, query_options.end)
    with open(Logging.page_file(0), 'w') as raw_logs:
        json.dump(response, raw_logs)

    output = Logging.normalized_query_results_file()
    with open(output, 'w') as normalized_logs:
        events = []
        for result in response[RESULTS]:
            timestamp, message, log_stream, _ = result
            # respects the same order as Cloudwatch's response for FilterLogEvents
            events.append({
                LOG_STREAM_NAME: log_stream[VALUE],
                TIMESTAMP: to_epoch_millis(parse_time_string(timestamp[VALUE], TIME_FORMAT)),
                MESSAGE: message[VALUE]
            })

        json.dump({EVENTS: events}, normalized_logs)

    return output

def _pipe_logs_to_less(file):
    less = sp.Popen(['less', file], stdin=sp.PIPE)
    if less:
        less.stdin.close() # no different with or without
        less.communicate() # no different with less.wait()

class LogGroup:
    def __init__(self, short_name, path):
        self.short_name = short_name
        self.path = path

class FetchLogOptions:
    def __init__(self, account, log_group, log_streams, start, end, filter_pattern, initial_page_size, max_results):
        self.account = account
        self.log_group = log_group
        self.log_streams = log_streams
        self.start = start
        self.end = end
        self.filter_pattern = filter_pattern
        self.initial_page_size = initial_page_size
        self.max_results = max_results

    def start_epoch(self):
        return to_epoch_millis(self.start)

    def end_epoch(self):
        return to_epoch_millis(self.end)

class QueryLogOptions:
    def __init__(self, log_groups_by_account, start, end, raw_filters, max_results):
        self.log_groups_by_account = log_groups_by_account
        self.start = start
        self.end = end
        self.query = self._to_query(raw_filters)
        self.max_results = max_results

    def _to_query(self, raw_filters):
        # if you change the order of these fields, make sure to update the logic that normalizes query results too
        return f'fields {Q_TIMESTAMP}, {Q_MESSAGE}, {Q_LOG_STREAM}' + '\n' + \
               '\n'.join(map(self._parse_simple_query_filter, raw_filters)) + ('' if len(raw_filters) == 0 else '\n') + \
               f'| sort {Q_TIMESTAMP} asc'

    def _parse_simple_query_filter(self, filter_str):
        def q(s):
            return "/{}/".format(s.replace("'", "\\'"))

        def parse_query_filter(s):
            m = re.match(UNESCAPED_LEADING_EXCL_MARK, s)
            return (True, s) if m is None else (False, s.replace(m.group(), ''))

        filters = [parse_query_filter(s.replace('\\|', '|')) for s in UNESCAPED_PIPE.split(filter_str)]
        return '| filter ' + ' or '.join(['{} {}like {}'.format(Q_MESSAGE, '' if is_include else 'not ', q(expr)) for is_include, expr in filters])

    def _for_single_account(self, account):
        if account not in self.log_groups_by_account:
            raise Exception(f'Account {account} not present in {self.log_groups_by_account.keys()}')
        return _SingleAccountQueryLogOptions(self.log_groups_by_account[account], self.start, self.end, self.query, self.max_results)

class _SingleAccountQueryLogOptions:
    def __init__(self, log_groups, start, end, query, max_results):
        self.log_groups = log_groups
        self.start = start
        self.end = end
        self.query = query
        self.max_results = max_results

    def start_epoch(self):
        return to_epoch_millis(self.start)

    def end_epoch(self):
        return to_epoch_millis(self.end)
