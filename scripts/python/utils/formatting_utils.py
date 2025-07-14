#!/usr/bin/env python3

from sty import fg, ef, rs

def bold(text):
    return ef.bold + text + rs.bold_dim


def italic(text):
    return ef.italic + text + rs.italic


def underlined(text):
    return ef.underl + text + rs.underl


def red(text):
    return fg.red + text + rs.fg


def cyan(text):
    return fg.cyan + text + rs.fg


def green(text):
    return fg.green + text + rs.fg


def yellow(text):
    return fg.yellow + text + rs.fg


class LogLevel:
    @staticmethod
    def normal():
        return LogLevel()


    @staticmethod
    def verbose():
        return LogLevel(should_print_logs=True)


    @staticmethod
    def quiet():
        return LogLevel(should_print_execution_details=False, should_print_successes=False)


    @staticmethod
    def execution_logs_only():
        return LogLevel(should_print_execution_details=False, should_print_successes=False, should_print_logs=True)


    def __init__(self, should_print_execution_details=True, should_print_successes=True, should_print_logs=False):
        self.should_print_execution_details = should_print_execution_details
        self.should_print_successes = should_print_successes
        self.should_print_logs = should_print_logs
