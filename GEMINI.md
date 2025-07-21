### Context

Hi Gemini, this package is a personal repository containing various util classes for Java and Scala (maybe Kotlin in the future). It's essentially code I have found to be useful in my career for a variety of projects, and I like to keep a library of such classes at hand to reuse them whenever I need.

### Technologies

- Scala classes are on version 2.12 right now
- Java classes are compiled and executed against Java 21
- The project uses Gradle with a Maven repository
- Tests are written in JUnit5 and assertions rely mostly on AssertJ (not in old code but in all new code I'd like it to be the case)
- We use Mockito to mock in tests (strict mocks)

### Testing

I have specific taste for unit tests and would like those preferences to be upheld:
- **deep assertions**: I always want to make the deepest assertion possible, which is in general to compare an entire object to another entire object that has the expected value. If not possible (e.g. it doesn't implement equals), we can use matchers to achieve as close a comparison as possible (e.g. ignore a field that has a random value). Even in Mockito `verify` assertions, we should generally avoid broad checks such as `anyDouble()` unless the value is random or unknown.
- **no field-by-field comparison**: just to reinforce the point above, we should generally not test the value of an output object with field-by-field assertions. First of all, this is very lengthy, but more importantly if the class adds a field at some point, we could forget to update the test and we'll therefore not be testing the value of this new field and allow bugs to go through
- **naming conventions**: I favor the use of concise test case names, hierarchical when needed. I left you some examples below. Typically, I do not include the expected output of the test in the name, this should be clear from the test. The test's name is there to structure our test cases and help us see the cases and subcases that are being tested.
  - `testMyMethod_case1_happyPath`
  - `testMyMethod_case1_inputValidation_duplicatedElements`
  - `testMyMethod_case1_inputValidation_negativeAmount`
  - `testMyMethod_case1_errorHandling_dependencyFailure`
   `testMyMethod_case2_happyPath`
- **readability**: a good unit test should be short. It's not always possible, but we should strive for a single test case to be only a few lines long and only contain the minimum amount of information that matters to the test. For example if we need to setup a data class with 15 fields and 12 of them have a dummy value that doesn't affect the outcome of the test, then those 12 fields should ideally be initialized in a helper, not in the test itself. 
- **duplication**: unit tests often have some amount of duplication, that's alright. However, I do my best to factor out common assertions into private helpers (generally in the test class itself) to make things short and readable. This is even more true for data helpers. You should identify which fields you need to be able to customize for each test case, and then build helpers that allow inputting only those fields so that the test methods contain the exact amount of code required to test the logic, and not more. 