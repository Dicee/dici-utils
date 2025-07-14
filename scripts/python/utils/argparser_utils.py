#!/usr/bin/env python3

from utils import *
from workspace import Context, Package


class ArgParser:
    @staticmethod
    def parse_and_normalize_args(parser, args, is_template_command=False, working_dir=None):
        args = parser.parse_args(args)
        if args.packages == 'all':
            args.packages = Context.ws.checked_packages()
        elif args.packages is None:
            args.packages = [Context.ws.current_package()]
        elif len(args.packages) == 0:
            args.packages = []
        else:
            args.packages = [Package(package) for package in args.packages.split(',')]

        args.is_template_command = is_template_command
        args.working_dir = working_dir

        return args


    @staticmethod
    def configure_run_all_parser(parser, default_packages=None, default_num_threads=DEFAULT_NUM_THREADS):
        ArgParser.configure_multi_packages_command_parser(parser, default_packages)
        ArgParser.configure_multithreaded_command_parser(parser, default_num_threads)
        parser.add_argument('--verbose', '-v', help='If set, enables verbose logging. In particular, all logs will be dumped at the end of the parallel execution  (default: false)', action='store_true')


    @staticmethod
    def configure_multi_packages_command_parser(parser, default_packages=None, additional_help=None):
        packages_option_required = default_packages is None
        default_packages = None if packages_option_required else ','.join([pkg.name for pkg in default_packages])
        base_help = 'Comma-separated packages to which to apply the command or "all" for all checked-in packages (default: auto-detected).'
        help = '{}{}'.format(base_help, ' ' + additional_help if additional_help is not None else '')
        parser.add_argument('--packages', '-p', type=str, help=help, default=default_packages, required=packages_option_required)


    @staticmethod
    def configure_multithreaded_command_parser(parser, default_num_threads):
        parser.add_argument('--threads', '-t', type=int, help='Number of threads to use for running this command (default: {})'.format(default_num_threads), default=default_num_threads)
