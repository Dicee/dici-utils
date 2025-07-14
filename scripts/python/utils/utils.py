# we don't know too much about the operation, so let's not assume it is efficient to run it with a gazillion threads
DEFAULT_NUM_THREADS = 10

# creating a workspace or adding numerous packages to a workspace at once is rare, and requires pulling quite a lot of
# data, so we're likely to hit our bandwidth's limit pretty soon
DEFAULT_NUM_THREADS_FOR_CLONING = 10

# most frequent operation, and typically not much to pull if the workspace is up-to-date
DEFAULT_NUM_THREADS_FOR_PULLING = 25


class ExecutionException(Exception):
    def __init__(self, task_result):
        super().__init__(self, str(task_result))
        self.task_result = task_result


class InvalidInputException(Exception):
    def __init__(self, message):
        super().__init__(self, message)
        self.message = 'ERROR: invalid input. Details: {}'.format(message)


class InvalidUsageException(Exception):
    def __init__(self, message):
        super().__init__(self, message)
        self.message = 'ERROR: {}'.format(message)