// shameless copied from https://stackoverflow.com/questions/50471047/preserve-cookies-localstorage-session-across-tests-in-cypress
const LOCAL_STORAGE_MEMORY = {};

Cypress.Commands.add("saveLocalStorage", () => {
  Object.keys(localStorage).forEach(key => {
    LOCAL_STORAGE_MEMORY[key] = localStorage[key]
  })
  return undefined
})

Cypress.Commands.add("restoreLocalStorage", () => {
  Object.keys(LOCAL_STORAGE_MEMORY).forEach(key => {
    localStorage.setItem(key, LOCAL_STORAGE_MEMORY[key])
  })
  return undefined
})

Cypress.Commands.add('removeAllCookies', () => {
    Cypress.Cookies.defaults({ preserve: null })
    cy.clearCookies({ domain: null })
    cy.getCookies().then((cookies) => {
        for (const cookie of cookies) {
            cy.clearCookie(cookie.name)
        }
        Cypress.Cookies.defaults({ preserve: /.*/ })
    })
    return undefined
})