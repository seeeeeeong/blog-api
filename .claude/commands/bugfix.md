Fix the following bug: $ARGUMENTS

## Fix Order

1. Trace the bug path — follow controller → service → storage
2. Identify root cause (not just symptoms)
3. Apply minimal fix — do not touch unrelated code
4. Write a regression test to prevent recurrence
5. Run `./gradlew test` — all tests must pass

## Rules

- Do not refactor unrelated code alongside the fix
- Do not break existing tests
- Never swallow errors with `runCatching { }.getOrDefault()`
- Throw errors explicitly as `CoreException(ErrorType.XXX)`
- Log in English using KotlinLogging
