# SQE Assignment 1 – Unit-Testing the Library class

**Due:** 12/11/2026 23:59 on Moodle · **Deliverable:** only `TestLibrary.java` · **Weight:** 5%

## What we need to do

Write JUnit 5 + Mockito unit tests for `Library.java`, so that they:

1. **Kill mutants and cover everything.** Grading uses mutation testing and coverage. A test that only *runs* a line doesn't count; it has to **assert** something that would break if that line changed.
2. **Isolate Library.** Every collaborator is a mock: `DatabaseService`, `ReviewService`, `NotificationService`, and also `Book` and `User`.
3. **Follow the class conventions** (tirgul 1 + 2):
   - Test names: `Given<Condition>_When<Method>_Then<Result>`, e.g. `GivenNullBook_WhenAddBook_ThenThrowIllegalArgumentException`
   - AAA layout in every test: `// Arrange`, `// Act`, `// Assert`
   - One aspect per test. No random values. Cover sunny-day and rainy-day cases.
   - Use annotations correctly: `@BeforeEach` for setup, `@ParameterizedTest` + `@ValueSource`/`@CsvSource`/`@MethodSource` for many inputs to the same check
4. **Change only `TestLibrary.java`.** Don't touch `pom.xml` or anything under `src/main`.

## ⚠️ Grader gotchas (checked against the evaluator jar)

- The grader compiles at **Java 8 language level**. So no `var`, no `List.of(...)`, no text blocks, no switch expressions. Use `Arrays.asList(...)` and `Collections.emptyList()` instead.
- The grader has **no `mockito-junit-jupiter`**, so `@ExtendWith(MockitoExtension.class)` won't compile. We set up mocks in `@BeforeEach` with `MockitoAnnotations.openMocks(this)`. This is already done in the file.

## Who does what

| Section in `TestLibrary.java` | Owner |
|---|---|
| `addBook` (incl. ISBN + author validation), `registerUser`, `borrowBook` | **Partner A** |
| `returnBook`, `notifyUserWithBookReviews`, `getBookByISBN` | **Partner B** |

The shared setup (mocks, constants, `stubValidBook()`, `stubValidUser()`) is already at the top of the file. If you need a new shared helper, say so before adding it, so we don't both write the same one.

### Things in Library.java that are easy to miss

- `isISBNValid`: null, hyphens get stripped, length ≠ 13, non-digits, a wrong check digit, and the `checkDigit == 10 → 0` case: `9783161484100` is valid and has check digit 0. Use it in a passing test.
- `isAuthorValid`: null/empty, must start and end with a letter, allowed characters `- ' . space`, no `--` or `''`. Note that the loop never checks the **last** character, but the start/end check covers it.
- `registerUser` / `borrowBook`: the id must be **exactly 12 digits** (test 11 and 13 digits too).
- `notifyUserWithBookReviews`:
  - `reviewService.close()` is called in a `finally` block, so verify it's called in **both** the success and the failure paths.
  - `NoReviewsFoundException` is *not* a `ReviewException`, so it is not converted to `ReviewServiceUnavailableException`.
  - The message format is exact: `"Reviews for '<title>':\n" + reviews joined by "\n"`.
  - The retry loop runs at most **5 times**: success on attempt 1, success on attempt 5, and failure on all 5 (verify `times(5)`).
- `getBookByISBN`: it returns the book **even if** the notification throws.
- Whenever a method throws, verify the DB write did **not** happen (`verify(..., never())`). That's what kills a lot of mutants.

## How to run

```bash
# from Assignment-1-code/  (needs Java 17 + Maven, internet for first run)
mvn clean test

# official mini-grader (from the repo root). It writes TestLibrary.grade next to the test file
./check.sh
```

## Git workflow

```bash
git pull                      # always before you start
# ...edit only your sections of TestLibrary.java...
./check.sh                    # make sure it compiles + see the grade
git add Assignment-1-code/src/test/java/ac/il/bgu/qa/TestLibrary.java
git commit -m "addBook: author validation tests"
git pull --rebase && git push
```

Because we each edit different sections of the same file, conflicts should be rare. Pull often and push small commits.

**Baseline:** the starter file scores 34.7 (2/8 mutants killed) on the mini-grader.
