/* esline-disable prettier/prettier */
// https://docs.cypress.io/guides/tooling/intelligent-code-completion.html#Set-up-in-your-Dev-Environment
/// <reference types="cypress"/>

function setUpLogging(on) {
    // this task executes Node code as opposed to running in the browser, which is helpful for debugging tests that ran automatically
    on('task', {
        log(event) {
            getLogger(event.level)(event.message)
            return null
        }
    })

    // best-effort attempt at logging Cypress commands and browser logs (see https://www.npmjs.com/package/cypress-terminal-report)
    require('cypress-terminal-report/src/installLogsPrinter')(on, {
        printLogsToConsole: 'onFail'
    })
}