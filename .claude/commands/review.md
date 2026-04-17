Review the changed files.

## Review Criteria (by priority)

1. **Security**: SQL injection, XSS, missing auth/authz, token/password exposure
2. **Bugs**: NPE, boundary values, concurrency, transaction scope
3. **Performance**: N+1 queries, unnecessary DB calls, missing indexes, uncached hot paths
4. **Layer violations**: Controller↔Storage direct dependency, entity exposure in responses
5. **Convention violations**: CLAUDE.md / clean-code.md rule breaches
6. **Missing tests**: New features or bugfixes without test coverage
7. **Error handling**: Swallowed errors, incorrect ErrorType usage

## Output Format

Per finding:
- `file:line` — description of the issue
- Severity: **Critical** / **Major** / **Minor**
- Suggestion: specific fix

End with an overall summary (pass/fail + key improvements needed).
