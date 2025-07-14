import json
import os
import pathlib

from cloudwatch import LogGroup
from utils import InvalidInputException

# top-level and shared schema
SERVICES = 'services'
ACCOUNTS = 'accounts'
NAME = 'name'

# service schema
ALIAS = 'alias'
DEFAULT_LOG_GROUP = 'defaultLogGroup'

# account schema
ACCESS_ROLE = 'accessRole'
HOSTED_SERVICES = 'hostedServices'
ACCOUNT_TYPE = 'accountType'

# log group schema
LOG_GROUPS = 'logGroups'
MAPPINGS = 'mappings'

class Config:
    _CONF = None

    @staticmethod
    def get():
        if Config._CONF is None:
            root = pathlib.Path(__file__).parent.absolute()
            Config._CONF = Config(os.path.join(root, 'cwgrep-config.json'))
        return Config._CONF

    def __init__(self, path):
        if not os.path.exists(path):
            raise InvalidInputException(f'Cannot find configuration file at: {path}')

        with open(path, 'r') as f:
            config = json.load(f)

        self._services = {}
        self._default_log_group_by_service = {}
        for raw_service in config[SERVICES]:
            self._add_service(raw_service)

        self._accounts = {}
        for account in config[ACCOUNTS]:
            self._add_account(account)

        self._log_groups = []
        self._log_group_mappings = {}
        for log_group in config[LOG_GROUPS]:
            self._add_log_group(log_group)

    def _add_service(self, raw_service):
        namespace = 'service'
        name = _get_required(namespace, raw_service, NAME)
        alias = _get_required(namespace, raw_service, ALIAS)

        service = Service(name, alias)
        _check_not_exists(self._services, name, 'service name')
        _check_not_exists(self._services, alias, 'service alias')

        self._services[name] = service
        self._services[alias] = service
        self._default_log_group_by_service[alias] = raw_service.get(DEFAULT_LOG_GROUP, None)

    def _add_account(self, account):
        namespace = 'account'

        name = _get_required(namespace, account, NAME)
        _check_not_exists([ acc.name for acc in self._accounts.values()], name, 'account name')

        for service_key in _get_required(namespace, account, HOSTED_SERVICES):
            _, _, region = _parse_service_key(service_key, self._services)
            access_role = _get_required(namespace, account, ACCESS_ROLE)
            self._accounts[service_key] = AWSAccount(name, region, access_role, account.get(ACCOUNT_TYPE, AccountType.CONDUIT))

    def _add_log_group(self, log_group):
        namespace = 'logGroup'
        name = _get_required(namespace, log_group, NAME)

        _check_not_exists(self._log_groups, name, 'log group')
        self._log_groups.append(name)

        for key, value in _get_required(namespace, log_group, MAPPINGS).items():
            _check_not_exists(self._log_group_mappings, key, 'log group mapping')
            self._log_group_mappings[key] = value

    def all_services(self):
        return self._services.keys()

    def all_log_groups(self):
        return self._log_groups

    def get_service(self, name_or_alias):
        if name_or_alias not in self._services:
            raise InvalidInputException("could not find any service with name or alias'{}'".format(name_or_alias))

        return self._services[name_or_alias]

    def get_account(self, service, stage, region):
        key = '{}-{}-{}'.format(service.alias, stage, region)

        if key not in self._accounts:
            raise InvalidInputException('could not find any suitable AWS account for (service, stage, region) = ({}, {}, {})'.format(service.alias, stage, region))

        return self._accounts[key]

    def get_log_group(self, short_name, service, stage, region):
        mappings = self._log_group_mappings
        tuple_key = (service.alias, short_name, stage, region)
        key = '{}-{}-{}-{}'.format(*tuple_key)
        if key not in mappings:
            raise InvalidInputException('could not find any suitable log group for (service, log group, stage, region) = ({}, {}, {}, {}).\nAvailable mappings are: {}'
                                        .format(*tuple_key, list(mappings.keys())))

        return LogGroup(short_name, mappings[key])

    def default_log_group(self, service):
        if service.alias not in self._default_log_group_by_service:
            raise InvalidInputException(f'service {service.name} has no default log group, so you must specify one explicitly')
        return self._default_log_group_by_service[service.alias]

def _parse_service_key(service_key, services):
    parts = service_key.split('-')
    if len(parts) != 3:
        raise InvalidInputException(f'expected format "alias-stage-region" but was: {service_key}')

    alias, stage, region = parts
    if alias not in services:
        raise InvalidInputException(f'unrecognized service alias: {alias}')

    Stages.validate(stage)
    Regions.validate(region)
    return alias, stage, region

def _get_required(namespace, obj, field):
    if field not in obj:
        raise InvalidInputException(f'expected field {namespace}.{field} to be present but was missing in {obj}')
    return obj[field]

def _check_not_exists(collection, key, description):
    if key in collection:
        raise InvalidInputException(f'invalid config: duplicate {description} {key}')

class Stages:
    DEVO = 'devo'
    ALPHA = 'alpha'
    BETA = 'beta'
    GAMMA = 'gamma'
    PROD = 'prod'

    ALL = [DEVO, ALPHA, BETA, GAMMA, PROD]

    @staticmethod
    def validate(stage):
        if stage not in Stages.ALL:
            raise InvalidInputException(f'unknown stage: {stage}')
        return stage

class Regions:
    IAD = 'iad'
    PDX = 'pdx'
    DUB = 'dub'

    ALL = [IAD, PDX, DUB]

    @staticmethod
    def to_aws_region(region):
        return {
            Regions.IAD: 'us-east-1',
            Regions.PDX: 'us-west-2',
            Regions.DUB: 'eu-west-1'
        }[Regions.validate(region)]

    @staticmethod
    def validate(region):
        if region not in Regions.ALL:
            raise InvalidInputException(f'unknown region: {region}')
        return region


class Service:
    def __init__(self, name, alias):
        self.name = name
        self.alias = alias

class AWSAccount:
    def __init__(self, name, region, access_role, type):
        self.name = name
        self.region = Regions.to_aws_region(region)
        self.access_role = access_role
        self.type = AccountType.validate(type)

    def __eq__(self, other):
        return self.name == other.name # name is unique, so sufficient here

    def __hash__(self):
        return hash(self.name) # name is unique, so sufficient here

    def __repr__(self):
        return self.name

    def __str__(self):
        return self.name

class AccountType:
    CONDUIT = 'conduit'
    ISENGARD = 'isengard'

    @staticmethod
    def validate(account_type):
        if account_type not in [AccountType.CONDUIT, AccountType.ISENGARD]:
            raise InvalidInputException(f'invalid account type: {account_type}')
        return account_type
