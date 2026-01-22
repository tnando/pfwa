---
name: test
description: Run test suites for frontend and/or backend
allowed-tools: Bash
---

# Run Tests

Run the test suite(s) for the project.

## Arguments
- No args: Run all tests
- `frontend` or `fe`: Run frontend tests only
- `backend` or `be`: Run backend tests only
- `e2e`: Run E2E tests only
- `coverage`: Run with coverage report

Argument provided: $ARGUMENTS

## Commands

### Frontend Tests
```bash
cd frontend && npm test
```

### Frontend Tests with Coverage
```bash
cd frontend && npm run test:coverage
```

### Backend Tests
```bash
cd backend && ./mvnw test
```

### Backend Tests with Coverage
```bash
cd backend && ./mvnw test jacoco:report
```

### E2E Tests
```bash
cd frontend && npm run cypress:run
```

## Steps
1. Determine which tests to run based on arguments
2. Check if the relevant directory exists
3. Run the appropriate test command
4. Report results summary
