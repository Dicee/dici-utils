import calendar
import datetime as dt
import time

from pytimeparse.timeparse import timeparse

from utils import InvalidInputException

ISO_FORMAT = '%Y-%m-%dT%H:%M:%SZ'
PAST_DURATION_SUFFIX = ' ago'

def to_epoch_millis(date_time):
    return to_epoch_seconds(date_time) * 1000

def parse_past_duration_or_iso_string(now, s):
    if s.endswith(PAST_DURATION_SUFFIX):
        return now - parse_duration(s[:-len(PAST_DURATION_SUFFIX)])

    return dt.datetime.fromtimestamp(to_epoch_seconds(time.strptime(s, ISO_FORMAT)), tz=dt.timezone.utc)

def parse_epoch_seconds(epoch_seconds):
    return dt.datetime.fromtimestamp(epoch_seconds / 1000.0, tz=dt.timezone.utc)

def parse_iso_string(s):
    return parse_time_string(s, ISO_FORMAT)

def parse_time_string(s, formatter):
    return dt.datetime.fromtimestamp(to_epoch_seconds(time.strptime(s, formatter)), tz=dt.timezone.utc)

def to_iso_string(date_time):
    return date_time.strftime(ISO_FORMAT)

def to_epoch_seconds(date_time):
    if isinstance(date_time, time.struct_time):
        return calendar.timegm(date_time)

    if isinstance(date_time, dt.datetime):
        return to_epoch_seconds(date_time.utctimetuple())

    raise Exception('Unsupported date time object type {} with value {}', type(date_time), date_time)

def utc_now():
    return dt.datetime.now(tz=dt.timezone.utc)

def parse_duration(s):
    duration = timeparse(s)
    if duration is None:
        raise InvalidInputException("couldn't parse age '{}'. Ignoring.".format(s))
    return dt.timedelta(seconds=round(duration))

def to_pretty_duration(duration):
    res = []
    num_years = int(duration.days / 365)
    if num_years > 0:
        duration -= dt.timedelta(days=num_years * 365)
        res.append('{}y'.format(num_years))

    if duration.days > 0:
        res.append('{}d'.format(duration.days))

    num_hours = int(duration.seconds / 3600)
    if num_hours > 0:
        duration -= dt.timedelta(hours=num_hours)
        res.append('{}h'.format(num_hours))

    num_minutes = int(duration.seconds / 60)
    if num_minutes > 0:
        res.append('{}m'.format(num_minutes))

    num_seconds = int(duration.seconds) % 60
    if num_seconds > 0:
        res.append('{}s'.format(num_seconds))

    return ''.join(res)

