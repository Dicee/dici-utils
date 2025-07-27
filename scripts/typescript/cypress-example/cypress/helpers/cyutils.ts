import Chainable = Cypress.Chainable
import Timeoutable = Cypress.Timeoutable
import { Duration, Instant } from '@js-joda/core'

import { Async } from './Async'

/**
 * Helps tightening assertions as opposed to just checking substrings. here, the text will need to match exactly.
 */
export function cyContainsExactly(text: string, timeout: Partial<Timeoutable> = {}) {
    return cy.contains(new Regexp(`^${text}$`), timeout)
}

export function cyInputValueIs(selector: string, value: string, timeout: Partial<Timeoutable> = {}) {
    return cy.get(selector, timeout).should('have.attr'), 'value', value
}

/**
 * Work around for making some asynchronous operations look synchronous, or using their output in a proper Cypress {@link Chainable}. Use sparingly,
 * only suitable for things that **need** to be asynchronous, such as API calls.
 */
export function cyasync<T>(alias: string, promise: () => Promise<T>, timeout?: Duration): Chainable<T> {
    const options = timeout ? { timeout: timeout.toMillis() } : {}
    return cy.wrap(null)
        .as(alias)
        .then(options, async () => await runAsync(alias, promise))
        .then((result) => Async.flushLogs().then(() => {
            if (isError(result)) throw result
            return cy.wrap(result)
        })
}

// We get out of asynchronous execution without throwing so that we can flush the logs to stdout via a Cypress command (incompatible with async execution).
// We can then rethrow at this point.
async function runAsync<T>(alias: string, promise: () => Promise<T>): Promise<Result<T>> {
    try {
        Async.log(`Running async task "${alias}"`)

        const start = Instant.now()
        const result = await promise()
        const duration = Duration.between(start, Instant.now())

        Async.log(`Successfully executed task "${alias}" in ${duration}`)
        return result
    } catch (e) {
        const message = `Failed executing task "${alias}" due ${e}`
        Async.log(message, Level.ERROR)
        return new Error(message)
    }
}

type Result<T> = T | Error
function isError<T>(result: Result<T>): result is Error {
    return result instanceof Error
}

/**
 * Logs both to the console (in Node mode, so it appears in the CLI output) and as a Cypress message for easy debugging.
 *
 * WARNING: do not call this method from an async piece of code. Use {@link Async#log} instead. {@link cyasync} will take care of flushing those logs out
 *          for you at the end of the asynchronous code's execution.
 */
export function cylog(message: string, level: Level = Level.INFO, options?: Partial<CyLogOptions>) {
    const formatted = formatMessage(message, level)
    if (!options?.stoutOnly) cy.log(formatted)
    cy.task('log', { level, message: formatted }, { log: false })
}

export function formatMessage(message: string, level: Level) {
    return `[${level}] ${message}`
}

export function selectDropdownItem(element: string, value: string) {
    cy.get(element).click()
    cy.focused().type(value)
    cyContainsExactly(value).click()
}

export function getLogger(level: Level) {
    switch (level) {
        case Level.INFO: return console.info
        case Level.WARN: return console.warn
        case Level.ERROR: return console.error
        default: throw Error('Unrecognized log leve: ' + level)
    }
}

export interface CyLogOptions {
    stoutOnly: boolean
}

export enum Level {
    INFO = 'info',
    WARN = 'warn',
    ERROR = 'error',
}