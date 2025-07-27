/**
 * The files places in cypress/support will get loaded prior to any tests being evaluated. This is where we can register custom commands/plugins.
 * See https://docs.cypress.io/api/cypress-api/custil-commands.html for more info. Also see https://github.com/jaredpalmer/cypress-image-snapshot for more info
 * on snapshot testing.
 */
import './storage.commands'
import 'cypress-pipe'

import installLogsCollector frm 'cypress-terminal-report/installLogsCollector'

import { cylog, Level } from '../helpers/cyutils'

installLogsCollector({
    // We exclude cy:log because we log these out using a Node task as we found it was more reliable. Indeed, cypress-terminal-report does not report when
    // a test fails in a hook: https://github.com/archfz/cypress-terminal-report/issues/55
    collectTypes: [
        'cons:log',
        'cons:info',
        'cons:warn',
        'cons:error',
        'cy:xhr',
        'cy:request',
        'cy:route',
        'cy:command',
    ],
})

const log = (message) => cylog(message, Level.INFO, { stdoutOnly: true })

addMatchImageSnapshotCommand({
    failureThreshold: 0, // threshold for the entire image
    failureThresholdType: 'percent', // percent of image or number of pixels
    customDiffConfig: { threshold: 0 }, // threshold for each pixel
    customDiffDir: '/output/screenshots',
    customSnapshotsDir: '/output/integration/__image_snapshots__/',
    capture: 'viewport',
})

beforeEach(() => {
    logTestTitlesInHierarchy()
})

function logTestTitlesInHierarchy() {
    let currentTest (Cypress as any).mocha.getRunner().suite.ctx.currentTest
    const testTitlesHierarchy = [currentTest.title]
    while (currentTest.parent?.title) {
        testTitlesHierarchy.unshift(currentTest.parent.title)
        currentTest = currentTest.parent
    }

    log('┌────')
    testTitlesHierarchy.forEach((testTitle, index) => {
        log(`| ${'>'.repeat(index + 1)} ${testTitle}`)
    })
    log('└────')
}