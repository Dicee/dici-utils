# Cypress example project

This folder contains an example of project structure to implement UI tests using Cypress. It also contains a number of utilities that
are useful in the development of such tests.

## Debugging

### Interactive mode

You can open Cypress in interactive mode and selectively run test suites in a browser running on your machine, and debug the test as it 
executes in front of you. You can use the `cypress:open` command to this effect.

## Best practices and developer tips for writing tests

### Working with asynchronous code

Cypress is asynchronous in nature, but not in the same way that `async/await` normally works in Typescript. Indeed, a Typescript `Promise` 
runs as soon as possible whereas in Cypress, commands are queued up and run sequentially. You do not control *when* the command will execute,
but you have a guarantee that they will execute in the same order as your code scheduled them.

Because of these differences, you have to be careful about two things:
- **never use** `async/await` or `Promise` in any test cod. We have created a method called `cyasync` which properly wraps your asynchronous code
  (when it's necessary to write some, e.g. to make an API call) in a Cypress command so that it gets queued with the rest. If you disregard this 
  advice, be prepared to enter a world of pain with a lot of obscure issues.
- be careful when you mix synchronous code with Cypress commands. The Cypress commands will run in sequence, but they won't run at the same time as
  your synchronous code, which will run immediately. Cypress has its own promise-like framework (see below), so use that when you have synchronous code
  that needs to access the result of a Cypress command. Doing that, you can ensure the synchronous code will run after the asynchronous Cypress command 
  has completed

```typescript
// wrap an async function into a Cypress command to make it safe to use in tests involving Cypress commands
cyasync('Call API', async () => await this.callApi())

// run the synchronous code assignment of a value to the notification variable) in a "then" block to run it only when
// the user creation, wrapped in a Cypress command, has completed
user.create().then((user) => {
    notification = new UserCreationNotification(user)
})
```

For more information about the Cypress command execution model, you can read their doc from [this section](https://docs.cypress.io/guides/core-concepts/introduction-to-cypress.html#Commands-Are-Asynchronous).

### prepare/use/destroy model

We recommend a pattern for external resource management (generally involving asynchronous calls) consisting in a class that encapsulates the entire lifecycle, and
potentially some high-level methods that make writing tests more concise and convenient. For example,
if we need to create users for testing purpose, we could create a class named `TestUser` with the following methods:
- `create` (lifecycle)
- `get` (to access during tests)
- `logIn` (convenience method to use during tests, to have the user navigate through the log-in page and enter their credentials)
- `destroy` (lifecycle)

These methods encapsulate the lifecycle of a resource from its creation to its deletion. Doing this has many advantages:
- code sharing across all tests, making it easy to safely and consistently handle these resources (logging, error handling etc)
- hiding asynchronous operations and only expose safe methods wrapping the async code in Cypress commands
- hold a resource state across multiple tests or even test suites. You can sometimes use singleton resources if a state needs to be accessed by all or many tests and
  is not being mutated. This will help to make tests faster by caching some pre-computed values and make fewer API calls.

Example usage of our hypothetical `TestUser`:

```typescript
describe('User login flow', () => {
    const loggedOutUser = userManager.newLoggedOutUser(userDetails)
    
    before(() => {
        loggedOutUser.create()
    })
    
    after(() => {
        loggedOutUser.destroy()
    })
    
    it('is able to log into our application', () => {
        user.logIn()
        new HomePage().assertLoaded()
    })
})
```

### Miscellaneous

- beware: when running Cypress tests, you are in the browser. This means things will behave differently as in Node. For example, the AWS SDK
  has a browser version which does not have disk access, that's why if you need disk access you'll need to do it in `plugins/index.js`, which code executes while
  you're still in Node. Additionally, `console.log` during tests will log into the browser, not in stdout. We have configured a Cypress plugin to copy these logs to
  stdout (well, most of the time... The rest of the time it mysteriously doesn't), but this is perhaps another example of behaviour you may find surprising at first, 
  stemming from the fact that tests are running in the browser and not in Node.
- use `cylog` to log rather than `console.log` to log both in the console and as Cypress messages (which appear in the Cypress UI, very useful). When writing async/await
  code, **do not** use `cylog` because it relies on Cypress commands, which are not compatible with Typescript promises as we discussed. Instead, use `Async.log`, which is
  syntactic sugar for the good old `console.info/warn/error`.