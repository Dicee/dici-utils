import datetime as dt
import os
import os.path
import shutil
import stat

from string_utils import random_string
from formatting_utils import *
from time_utils import to_pretty_duration

def create_time_based_folder(root_path, pattern='%Y_%m_%d_%H_%M_%S'):
    time_str = dt.datetime.now().strftime(pattern)
    base_path = os.path.join(root_path, time_str)
    return __ensure_unique_folder(base_path)

def create_random_folder(root_path, length=6):
    base_path = os.path.join(root_path, random_string(length))
    return __ensure_unique_folder(base_path)

def __ensure_unique_folder(base_path):
    logs_root = base_path

    i = 1
    while os.path.exists(logs_root):
        if i > 50:
            raise Exception("Couldn't create logs directory with prefix {} after {} attempts".format(base_path, i))
        logs_root = base_path + ('.%02d' % i)

    os.makedirs(logs_root)
    return logs_root

def delete_old_files_under(root, max_age):
    pretty_duration = to_pretty_duration(max_age)
    for f in map(lambda path: os.path.join(root, path), os.listdir(root)):
        if os.path.isdir(f):
            age = dt.datetime.now() - dt.datetime.fromtimestamp(os.stat(f)[stat.ST_MTIME])
            if age > max_age:
                print(green("Deleting {} as it is older than '{}'".format(f, pretty_duration)))
                shutil.rmtree(f)
            else:
                print(yellow("Keeping {} as it is more recent than '{}'".format(f, pretty_duration)))
