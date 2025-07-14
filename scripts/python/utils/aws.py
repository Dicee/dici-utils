from cwgrep_logging import Logging

from botocore.exceptions import ClientError
from program_utils import exit_with_error
from formatting_utils import *

def execute_sdk_request(operation, request, do_execute, exit_on_error=True):
    Logging.debug('Calling {} with input {}'.format(operation, request))
    try:
        return do_execute(request)
    except ClientError as e:
        if exit_on_error:
            exit_with_error('Failed executing {}.\n{}: {}\n{}: {}'.format(operation, underlined('Request'), request, underlined('Response'), e.response))
        else:
            raise e

class SdkException(Exception):
    def __init__(self, operation, request, error):
        self.msg = 'Failed executing {}.\n{}: {}\n{}: {}'.format(operation, underlined('Request'), request, underlined('Response'), error.response)
        super(Exception, self).__init__(self.msg)
