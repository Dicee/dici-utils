import datetime as dt
import getpass
import json

import boto3

import aws
from credentials import ACCESS_KEY_ID, SECRET_ACCESS_KEY, SESSION_TOKEN, STSCredentialsStore
from cwgrep_logging import Logging
from time_utils import to_iso_string

class FirehoseClient:
    def __init__(self, account, role):
        credentials = STSCredentialsStore().get(account, role)
        session = boto3.Session(
            aws_access_key_id=credentials[ACCESS_KEY_ID],
            aws_secret_access_key=credentials[SECRET_ACCESS_KEY],
            aws_session_token=credentials[SESSION_TOKEN]
        )
        self._client = session.client('firehose', region_name=account.region)

    def put_record(self, stream_name, record):
        request = {
            'DeliveryStreamName': stream_name,
            'Record': {
                'Data': str.encode(record)
            }
        }
        return aws.execute_sdk_request('PutRecord', request, lambda req: self._client.put_record(**req), exit_on_error=False)


class UserBehaviorLogger:
    def __init__(self, account):
        self._firehose = FirehoseClient(account, 'none')

    def log_activity(self, command_name, full_command, **kwargs):
        record = {
            'timestamp': to_iso_string(dt.datetime.now()),
            'user': getpass.getuser(),
            'command_name': command_name,
            'full_command': full_command,
            **kwargs
        }

        try:
            self._firehose.put_record('cwgrep-user-behavior', json.dumps(record) + '\n')
        except aws.SdkException as e:
            Logging.debug(f'Failed to publish user activity\n {e.msg}')
