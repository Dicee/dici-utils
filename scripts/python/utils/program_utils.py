from formatting_utils import bold, yellow, red
import sys

def get_user_input(valid_answers, message):
     answer = None
     while answer == None or answer.lower() not in valid_answers:
         if answer != None:
             print(bold(yellow('Not a valid answer: {}'.format(answer))))
    
         answer = input(bold('{} ({}) '.format(message, '/'.join(valid_answers))))
     return answer.lower()


def exit_if_task_failed(task_result, additional_message=None):
    if task_result.is_success():
        return task_result

    print(task_result, file=sys.stderr)

    if additional_message is None:
        exit(1)

    exit_with_error(additional_message)


def exit_with_error(message):
    print(red(message), file=sys.stderr)
    exit(1)
