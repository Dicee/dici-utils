// This file helps the IDE understand custom Cypress commands. See this link for more information: https://docs.cypress.io/guides/tooling/intelligent-code-completion.html#Set-up-in-your-Dev-Environment
/// <reference types="cypress"/>

interface ClearCookies {
    domain?: string | null
}

declare namespace Cypress {
    interface Chainable<Subject> {
        /**
         * Solution to clear cookies for all domains: https://github.com/cypress-io/cypress/issues/408#issuecomment-767873113
         */
         clearCookies(
             options?: Partial<Cypress.Loggable & Cypress.Timeoutable> | ClearCookies | undefined,
         ): Chainable<null>

        saveLocalStorage(): Chainable<void>
        restoreLocalStorage(): Chainable<void>
        removeAllCookies(): Chainable<void>

        matchImageSnapshot(name?: string, options?: any): Chainable<any>
    }
}