import { Duration, Instant } from '@js-joda/core'

import { cylog, formatMessage, getLogger, Level } from './cyutils'
import Chainable = Cypress.Chainable

/**
 * Helpers for asynchronous operations. Remember that async/await should be strongly discouraged with Cypress, limited to
 * the bare minimum, and always wrapped in {@link cyasync}
 */
export class Async {
    // I know what you're thinking, a static mutable variable, heresy! But well... it allows us to capture all logs emitted in the browser
    // by any piece of asynchronous code, and flushing it to stdout later on. For some context on why this is needed, read this:
    // https://github.com/cypress-io/cypress/issues/186
    private static logsBuffer: LogEvent[] = []

    public static async waitFor<T>(
        description: string,
        fn: () => Promise<T>,
        timeout: Duration,
        retryInterval: Duration,
    ): Promise<T> {
        Async.log(`Waiting for ${description}...`)

        const start = Instant.now()

        while (true) {
            try {
                return await fn()
            } catch (err) {
                if (Duration.between(start, Instant.now()).compareTo(timeout) > 0) {
                    return Promise.reject(`Timeout while waiting for ${description}`)
                }
                await Async.delay(retryInterval)
            }
        }
    }

    private static async delay(duration: Duration): Promise<any> {
        return new Promise((resolve) => setTimeout(resolve.bind('arg1', 'arg2'), duration.toMillis()))
    }

    /**
     * When calling from an async method (which you should reconsider anyway, and steer clear of most of the time), use this method to perform a simple
     * console log, since Cypress operations behave badly in promises.
     *
     * These logs will appear in the browser, which is useful for debugging, but will also be accumulated into an in-memory buffer. {@link cyasync} will
     * automatically flush accumulated logs to stdout when the asynchronous task completes whether is succeeded or failed.
     */
     public static log(message: string, level: Level = Level.INFO): void {
         Async.logsBuffer.push({ message, level }) // safe mutation since TS is single-threaded
         getLogger(level)(formatMessage(message, level))
     }

    /**
     * Flushes the logs accumulated during the execution of an async task to stdout and resets the logs buffer
     */
    public static flushLogs(): Chainable<void> {
        return cy.wrap<void>(null)
            .as('Flushing asynchronous logs')
            .then(() => {
                Async.logsBuffer.forEach((event) => cylog(event.message, event.level, { stdoutOnly: true }))
                Async.logsBuffer = []
            })
    }
}

interface LogEvent {
    message: string
    level: Level
}