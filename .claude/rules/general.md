# General Development Rules

## Git Workflow
1. Create feature branch from main
2. Make atomic commits with conventional format
3. Create PR with description and issue reference
4. Request review before merge
5. Delete branch after merge

## Commit Format
```
<type>(<scope>): <description>

[optional body]

Refs: #<issue-number>
```

Types: feat, fix, docs, refactor, test, chore, ci, perf

## Code Quality
- No commented-out code
- No console.log/System.out.println in production
- No magic numbers (use constants)
- Meaningful variable/function names
- DRY - Don't Repeat Yourself

## Documentation
- README in each major directory
- JSDoc/Javadoc for public APIs
- ADRs for architectural decisions
- Keep docs up-to-date with code

## Security
- Never commit secrets or credentials
- Use environment variables for config
- Validate all user input
- Use parameterized queries (no SQL injection)
- HTTPS everywhere

## Performance
- Lazy load when possible
- Paginate large lists
- Index database queries
- Minimize API calls
- Cache when appropriate
