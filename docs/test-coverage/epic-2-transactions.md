# Test Coverage: Epic 2 - Transactions

## Overview

This document outlines the test coverage for Epic 2 (Transactions) of the Personal Finance Web App. Tests cover user stories #3-#12 including transaction CRUD operations, filtering, pagination, and summary statistics.

## Test Pyramid Distribution

| Layer | Target | Current | Files |
|-------|--------|---------|-------|
| Unit Tests | 70% | - | 6 files |
| Integration Tests | 20% | - | 1 file |
| E2E Tests | 10% | - | Planned |

## Backend Tests

### Controller Tests

#### TransactionControllerTest.java

Location: `backend/src/test/java/com/pfwa/controller/TransactionControllerTest.java`

| Test Class | Test Cases | Coverage |
|------------|------------|----------|
| CreateTransactionTests | 12 tests | Create transaction scenarios |
| GetTransactionsTests | 9 tests | List and filter transactions |
| GetTransactionByIdTests | 3 tests | Get single transaction |
| UpdateTransactionTests | 4 tests | Update transaction scenarios |
| DeleteTransactionTests | 3 tests | Delete transaction scenarios |
| GetTransactionSummaryTests | 2 tests | Summary statistics |

**Test Scenarios:**
- Create transaction with valid data (201 Created)
- Validation errors (missing amount, zero amount, negative amount, exceeds max)
- Missing required fields (type, categoryId, date)
- Description/notes exceeding max length
- Category not found (404)
- Category type mismatch with transaction type
- Null optional fields
- Income and expense transaction types

#### CategoryControllerTest.java

Location: `backend/src/test/java/com/pfwa/controller/CategoryControllerTest.java`

| Test Class | Test Cases | Coverage |
|------------|------------|----------|
| GetCategoriesTests | 9 tests | Category listing |

**Test Scenarios:**
- Get all categories grouped by type
- Filter by INCOME type
- Filter by EXPENSE type
- Empty category lists
- Category response details
- Cache-Control header
- Invalid type parameter
- Display order preservation

### Service Tests

#### TransactionServiceTest.java

Location: `backend/src/test/java/com/pfwa/service/TransactionServiceTest.java`

| Test Class | Test Cases | Coverage |
|------------|------------|----------|
| CreateTransactionTests | 4 tests | Business logic for creation |
| GetTransactionTests | 3 tests | Single transaction retrieval |
| GetTransactionsTests | 3 tests | List with pagination |
| UpdateTransactionTests | 4 tests | Update logic |
| DeleteTransactionTests | 3 tests | Delete logic |
| GetTransactionSummaryTests | 3 tests | Summary calculations |

**Test Scenarios:**
- Create with valid data
- Category type mismatch exception
- Transaction not found exception
- User isolation (access control)
- Summary calculation accuracy
- Default date range handling

#### CategoryServiceTest.java

Location: `backend/src/test/java/com/pfwa/service/CategoryServiceTest.java`

| Test Class | Test Cases | Coverage |
|------------|------------|----------|
| GetAllCategoriesTests | 4 tests | All categories |
| GetCategoriesByTypeTests | 4 tests | Filtered by type |
| GetCategoryByIdTests | 3 tests | Single category |
| ValidateCategoryForTypeTests | 3 tests | Type validation |
| CategoryResponseMappingTests | 2 tests | DTO mapping |

### Integration Tests

#### TransactionControllerIntegrationTest.java

Location: `backend/src/test/java/com/pfwa/controller/TransactionControllerIntegrationTest.java`

Uses TestContainers for PostgreSQL database testing.

| Test Class | Test Cases | Coverage |
|------------|------------|----------|
| CreateTransactionIntegrationTests | 4 tests | End-to-end creation |
| GetTransactionsIntegrationTests | 10 tests | Full filtering |
| GetTransactionByIdIntegrationTests | 3 tests | Single transaction |
| UpdateTransactionIntegrationTests | 3 tests | Update flow |
| DeleteTransactionIntegrationTests | 3 tests | Delete flow |
| GetSummaryIntegrationTests | 2 tests | Summary endpoint |

**Key Integration Scenarios:**
- Full request/response cycle with database
- User authentication via JWT
- User isolation (cannot access other users' transactions)
- Filter combinations (date, type, category, amount, search)
- Pagination and sorting
- Database state verification

## Frontend Tests

### Component Tests

#### TransactionList.test.tsx

Location: `frontend/src/components/transactions/TransactionList.test.tsx`

| Test Suite | Test Cases | Coverage |
|------------|------------|----------|
| Rendering | 7 tests | Table structure, data display |
| Empty State | 2 tests | No data handling |
| Loading State | 2 tests | Skeleton loading |
| Sorting | 4 tests | Sort interactions |
| Pagination | 5 tests | Page navigation |
| Actions | 4 tests | Edit/Delete buttons |
| Row Expansion | 4 tests | Expandable details |
| Description Truncation | 3 tests | Text overflow |
| Visual Indicators | 2 tests | Color coding |
| Accessibility | 4 tests | A11y compliance |

#### TransactionForm.test.tsx

Location: `frontend/src/components/transactions/TransactionForm.test.tsx`

| Test Suite | Test Cases | Coverage |
|------------|------------|----------|
| Rendering - Create Mode | 5 tests | Form fields |
| Rendering - Edit Mode | 2 tests | Pre-filled data |
| Validation | 8 tests | Input validation |
| Form Submission | 3 tests | Submit handling |
| Type Switching | 2 tests | INCOME/EXPENSE toggle |
| Loading State | 3 tests | Disabled during submit |
| Error Display | 2 tests | Error messages |
| Character Counters | 3 tests | Length limits |
| Edit Mode - No Changes | 2 tests | Dirty tracking |

#### TransactionFilter.test.tsx

Location: `frontend/src/components/transactions/TransactionFilter.test.tsx`

| Test Suite | Test Cases | Coverage |
|------------|------------|----------|
| Rendering | 5 tests | Filter panel |
| Active Filter Count | 3 tests | Badge display |
| Date Range Filter | 3 tests | Date selection |
| Type Filter | 3 tests | Type dropdown |
| Category Filter | 3 tests | Category multi-select |
| Amount Filter | 3 tests | Amount inputs |
| Clear Filters | 3 tests | Reset functionality |
| Collapse/Expand | 2 tests | Toggle behavior |
| Preserved Filter Values | 1 test | State persistence |

### Page Tests

#### TransactionsPage.test.tsx

Location: `frontend/src/pages/transactions/TransactionsPage.test.tsx`

| Test Suite | Test Cases | Coverage |
|------------|------------|----------|
| Initial Rendering | 4 tests | Page structure |
| Summary Display | 1 test | Summary cards |
| Empty State | 1 test | No transactions |
| Error Handling | 1 test | API errors |
| Navigation | 3 tests | Page routing |
| Delete Transaction | 4 tests | Delete flow |
| Filtering | 2 tests | URL parameters |
| Pagination | 1 test | Page params |
| Search | 1 test | Search param |
| User Menu | 2 tests | Account menu |

### API Tests

#### transactionApi.test.ts

Location: `frontend/src/api/transactionApi.test.ts`

| Test Suite | Test Cases | Coverage |
|------------|------------|----------|
| getTransactions | 4 tests | List endpoint |
| getTransaction | 2 tests | Single endpoint |
| createTransaction | 3 tests | Create endpoint |
| updateTransaction | 2 tests | Update endpoint |
| deleteTransaction | 2 tests | Delete endpoint |
| getSummary | 4 tests | Summary endpoint |
| Query Parameter Building | 5 tests | URL construction |

## User Story Coverage

| Issue | User Story | Backend Tests | Frontend Tests |
|-------|------------|---------------|----------------|
| #3 | Add manual transaction | Create tests | TransactionForm tests |
| #4 | View transaction list | GetTransactions tests | TransactionList tests |
| #5 | Edit transaction | Update tests | TransactionForm tests |
| #6 | Delete transaction | Delete tests | Delete dialog tests |
| #7 | Categorize transactions | Category tests | CategorySelect tests |
| #8 | Filter by date | DateRange filter tests | TransactionFilter tests |
| #9 | Filter by type | Type filter tests | TransactionFilter tests |
| #10 | Filter by category | Category filter tests | TransactionFilter tests |
| #11 | Search transactions | Search filter tests | TransactionSearch tests |
| #12 | View summary | Summary tests | TransactionSummary tests |

## Test Patterns Used

### Backend

- **AAA Pattern**: Arrange-Act-Assert structure
- **MockMvc**: Controller testing without full context
- **Mockito**: Service and repository mocking
- **TestContainers**: PostgreSQL for integration tests
- **Nested Test Classes**: Organized by endpoint/method
- **@DisplayName**: Clear test descriptions

### Frontend

- **React Testing Library**: Component testing
- **Vitest**: Test runner and assertions
- **userEvent**: User interaction simulation
- **Mock Service Worker patterns**: API mocking
- **Custom render functions**: Provider wrapping
- **waitFor**: Async operation handling

## Running Tests

### Backend

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests TransactionControllerTest

# Run with coverage
./gradlew test jacocoTestReport
```

### Frontend

```bash
# Run all tests
npm test

# Run specific test file
npm test -- TransactionList.test.tsx

# Run with coverage
npm test -- --coverage
```

## Coverage Targets

| Metric | Target | Notes |
|--------|--------|-------|
| Line Coverage | > 80% | Services, controllers |
| Branch Coverage | > 75% | Conditional logic |
| Method Coverage | > 85% | Public methods |

## Excluded from Coverage

- Entity getters/setters (JPA entities)
- Configuration classes
- Exception classes (simple wrappers)
- Main application class

## Future Improvements

1. **E2E Tests**: Add Cypress tests for critical flows
2. **Performance Tests**: Load testing for list endpoints
3. **Security Tests**: Penetration testing for auth
4. **Mutation Testing**: Verify test quality with PIT
