import errno
import json
import os
import subprocess as sp
from os import path
from os.path import expanduser

import execution_utils
from cwgrep_logging import Logging
from midway import get_midway_cookie
from program_utils import *
from time_utils import *

ACCESS_KEY_ID = 'AccessKeyId'
SECRET_ACCESS_KEY = 'SecretAccessKey'
SESSION_TOKEN = 'SessionToken'
EXPIRATION = 'Expiration'

STATUS = 'status'
ERROR = 'error'

CACHE_FILE = expanduser('~/.cwgrep/sts_credentials_store.json')

class STSCredentialsStore:
    def __init__(self):
        pass

    def get(self, account):
        role = account.access_role
        self.__ensure_path_exists(CACHE_FILE)
        cache = {} if not path.exists(CACHE_FILE) else self.__load_cache()
        cache_key = self._get_cache_key(account, role)

        if not self.__has_fresh_entry(account, role, cache):
            credentials = self.__fetch_credentials(account, role)
            cache[cache_key] = credentials
            self.__update_cache(cache)

        return cache[cache_key]

    def __load_cache(self):
        with open(CACHE_FILE, 'r') as cache_file:
            return json.load(cache_file)

    def __update_cache(self, cache):
        with open(CACHE_FILE, 'w') as cache_file:
            cache_file.write(json.dumps(cache, indent=4))

    def __has_fresh_entry(self, account, role, cache):
        cache_key = self._get_cache_key(account, role)

        if cache_key not in cache:
            return False

        entry = cache[cache_key]
        expiration = parse_iso_string(entry[EXPIRATION])
        if utc_now() > expiration:
            Logging.debug('Expired token for role {} in account {}. Will refresh.'.format(role, account.name))
            return False

        return True

    def _get_cache_key(self, account, role):
        return f'{account.name}:{role}'

    def __fetch_credentials(self, account, role):
        Logging.debug(yellow('Fetching credentials for role {} in account {}...'.format(role, account.name)))
        try:
            with open(Logging.execution_log_file(), 'a') as stderr:
                credentials = execution_utils.exec_cmd(
                    f'credentials print --provider={account.type} --role={role} --account={account.name}',
                    stdout=sp.PIPE, stderr=stderr, raise_on_failure=True
                ).result()

            return json.loads(credentials)
        except Exception as e:
            msg = 'Failed fetching credentials for account {} and role {}'.format(account.name, role)
            Logging.debug(red('{} with error: {}'.format(msg, e)))
            exit_with_error(f'ERROR: {msg}. may not have permissions to this account and role. '
                            f'The logs above or in {Logging.execution_log_file()} should help you root-causing this.')

    def __ensure_path_exists(self, file):
        if not path.exists(path.dirname(file)):
            try:
                os.makedirs(path.dirname(file))
            except OSError as exc: # Guard against race condition
                if exc.errno != errno.EEXIST:
                    raise
