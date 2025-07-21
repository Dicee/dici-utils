/*
This file illustrates our recommendation in terms of best practices to manage the configuration of our CDK stacks.
Essentially, we advocate for a centralized configuration file where all the region and stage-dependent configuration
is present, so that we can check the differences between our stacks in a single place rather than having to dig deep
in the code. Additionally, to make the configuration as crisp as possible, we support both computed values that depend
on a simple condition based on the stage and/or region, as well as non-computed ones which are typically values to hardcode,
different for most of the stages. Computed configurations are crisper and prevent duplication, but in some cases non-computed
ones are required.
*/

import Duration from 'aws-cdk-lib/core'

// We separate the notion of logical and infrastructure stages because a logical stage may be deployed to multiple
// destinations (e.g. OneBox logically behaves as Prod, different regions all have the same logical stage but are
// deployed independently etc)
export enum LogicalStage {
    ALPHA = 'alpha'
    BETA = 'beta'
    GAMMA = 'gamma'
    PROD = 'prod'
}

export class InfraStage {
    static readonly DEVO = new InfraStage(LogicalStage.ALPHA, 'us-west-2')
    static readonly BETA = new InfraStage(LogicalStage.BETA, 'us-west-2')
    static readonly GAMMA = new InfraStage(LogicalStage.GAMMA, 'us-east-1')
    static readonly ONE_BOX = new InfraStage(LogicalStage.PROD, 'us-east-1')
    static readonly PROD_EU = new InfraStage(LogicalStage.PROD, 'eu-west-1')
    static readonly PROD_NA = new InfraStage(LogicalStage.PROD, 'us-east-1')

    private constructor(
        public readonly logicalStage: LogicalStage,
        public readonly region: 'us-east-1' | 'us-west-2' | 'eu-west-1',
    )
}

interface Config {
    accountId: string
    region: string
    stage: LogicalStage
}

interface MonitoringStackNonComputedConfig {
    enableTicketCutting?: boolean
    highSeverityLevel: number
    lowSeverityLevel: number
}

const monitoringStageDependentConfigs: Record<LogicalStage, SoleilNonComputedConfig> = {
    [LogicalStage.DEVO]: {
        highSeverityLevel: 5,
        lowSeverityLevel: 5,
    },
    [LogicalStage.BETA]: {
        highSeverityLevel: 4,
        lowSeverityLevel: 5,
    },
    [LogicalStage.GAMMA]: {
        enableTicketCutting: true,
        highSeverityLevel: 4,
        lowSeverityLevel: 4,
    },
    [LogicalStage.PROD]: {
        enableTicketCutting: true,
        highSeverityLevel: 2,
        lowSeverityLevel: 3,
    }
}

export interface MonitoringStackConfig extends Config, MonitoringStackNonComputedConfig {}

export function getMonitoringStackConfig(stage: InfraStage, accountId: string): MonitoringStackConfig {
    const logicalStage = stage.logicalStage
    return {
        ...monitoringStageDependentConfigs[logicalStage],
        stage: logicalStage,
        accountId,
        region: stage.region,
    }
}

export interface ApplicationStackConfig {
    maxBatchSize: number
    maxBatchingWindow: number
}

export function getApplicationStackConfig(stage: InfraStage, accountId: string): ApplicationStackConfig {
    const logicalStage = stage.logicalStage
    const isProd = logicalStage === LogicalStage.PROD

    return {
        stage: logicalStage,
        accountId,
        region: stage.region,
        maxBatchSize: isProd ? 10000 : 5,
        maxBatchingWindow: isProd ? Duration.minutes(5) : Duration.seconds(5)
    }
}