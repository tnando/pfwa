---
name: commit
description: Commit changes with conventional commit format
allowed-tools: Bash
---

# Commit Changes

Create a git commit following the conventional commit format for this project.

## Steps

1. Run `git status` to see changed files
2. Run `git diff --staged` to see staged changes (if any)
3. Run `git diff` to see unstaged changes
4. If nothing is staged, stage the relevant files with `git add <files>`
5. Analyze the changes and determine:
   - Type: feat, fix, docs, refactor, test, chore, ci, perf
   - Scope: auth, transactions, budgets, dashboard, ui, api, db, docker
6. Create commit with format:
   ```
   <type>(<scope>): <short description>

   <optional body>

   $ARGUMENTS
   ```

## Commit Types
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation only
- `refactor` - Code refactoring
- `test` - Adding/updating tests
- `chore` - Maintenance tasks
- `ci` - CI/CD changes
- `perf` - Performance improvements

## Example
```bash
git commit -m "feat(transactions): add transaction form component

Implements the TransactionForm with validation using React Hook Form.

Refs: #42"
```
