# SQE Assignment 1 – Unit-Testing the Library class

Submit only `TestLibrary.java` on Moodle by **12/11/2026 23:59**.

## Rules
- Use JUnit 5 + Mockito, and mock every collaborator of `Library`.
- Test names: `Given<Condition>_When<Method>_Then<Result>`
- Lay out each test as Arrange / Act / Assert, with one aspect per test and no random values.
- Change only `TestLibrary.java`. Don't touch `pom.xml` or `src/main`.

## Grader gotchas
- It compiles at **Java 8** level, so no `var` and no `List.of(...)`.
- No `@ExtendWith(MockitoExtension.class)`. Mocks are set up with `MockitoAnnotations.openMocks(this)` in `@BeforeEach`.

## Run
```bash
./check.sh   # course mini-grader, prints the grade (needs Java 17)
```

## Workflow
`git pull` → write tests in your sections → `./check.sh` → commit → `git pull --rebase && git push`
