# Epic 2: Transactions User Stories

## Epic Overview
**Epic ID:** EPIC-002
**Epic Name:** Transactions
**Story Points:** 27
**Priority:** Must Have (MVP Critical)
**Status:** Ready for Development

### Epic Description
Implement comprehensive transaction management system allowing users to create, read, update, and delete financial transactions (income and expenses). Include categorization, filtering, search, pagination, and summary features for effective personal finance tracking.

### Technical Stack
- Backend: Spring Boot REST API, Spring Data JPA
- Database: PostgreSQL (transactions, categories tables)
- Frontend: React with Material-UI forms and data grids
- Validation: Bean Validation (JSR-380), custom business rules
- Features: Filtering, pagination, sorting, CSV export

---

## User Stories

### TXN-001: Create Transaction
**Priority:** Must Have
**Story Points:** 5
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **add a new transaction with amount, type, category, date, and description**,
So that **I can track my income and expenses accurately**.

#### Acceptance Criteria

**Given** I am on the transaction creation page
**When** I enter valid transaction details (amount, type, category, date, description)
**Then** the transaction is saved and I see a success message "Transaction created successfully"

**Given** I am creating a transaction
**When** I select transaction type as "INCOME"
**Then** I can choose from income categories (Salary, Freelance, Investments, Other Income)

**Given** I am creating a transaction
**When** I select transaction type as "EXPENSE"
**Then** I can choose from expense categories (Food, Transportation, Housing, Utilities, Entertainment, Healthcare, Shopping, Other)

**Given** I am creating a transaction
**When** I enter an amount less than or equal to 0
**Then** I see an error message "Amount must be greater than 0"

**Given** I am creating a transaction
**When** I enter an amount greater than 10,000,000
**Then** I see an error message "Amount exceeds maximum allowed value"

**Given** I am creating a transaction
**When** I select a future date beyond today
**Then** I see a warning "Transaction date is in the future. Are you sure?"

**Given** I am creating a transaction
**When** I leave required fields empty (amount, type, category, date)
**Then** I see specific validation errors for each missing field

**Given** I am creating a transaction
**When** I enter a description longer than 500 characters
**Then** I see an error message "Description must be 500 characters or less"

**Given** I successfully create a transaction
**When** the transaction is saved
**Then** I am redirected to the transaction list with the new transaction visible at the top

#### Technical Requirements
- Amount: decimal(12,2), positive value required
- Type: enum (INCOME, EXPENSE)
- Category: foreign key to categories table
- Date: date field, default to today, can be past or present
- Description: optional, varchar(500)
- User ID: foreign key to authenticated user
- Created timestamp and updated timestamp auto-generated
- Validation: amount > 0, date not null, category exists

#### API Endpoint
```
POST /api/v1/transactions
Headers:
  Authorization: Bearer {accessToken}

Request Body:
{
  "amount": 150.00,
  "type": "EXPENSE",
  "categoryId": "uuid",
  "date": "2026-01-20",
  "description": "Grocery shopping at Whole Foods",
  "notes": "Weekly groceries"
}

Response: 201 Created
{
  "id": "uuid",
  "amount": 150.00,
  "type": "EXPENSE",
  "category": {
    "id": "uuid",
    "name": "Food",
    "type": "EXPENSE"
  },
  "date": "2026-01-20",
  "description": "Grocery shopping at Whole Foods",
  "notes": "Weekly groceries",
  "createdAt": "2026-01-20T14:30:00Z",
  "updatedAt": "2026-01-20T14:30:00Z"
}
```

---

### TXN-002: View Transaction List
**Priority:** Must Have
**Story Points:** 3
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **see a list of all my transactions sorted by date (newest first)**,
So that **I can review my recent financial activity**.

#### Acceptance Criteria

**Given** I navigate to the transactions page
**When** the page loads
**Then** I see my transactions sorted by date (most recent first)

**Given** I have no transactions
**When** I view the transactions page
**Then** I see a message "No transactions yet. Create your first transaction to get started."

**Given** I have transactions
**When** I view the transaction list
**Then** each transaction displays:
- Amount (formatted with currency symbol)
- Type indicator (income in green, expense in red)
- Category name with icon
- Date (formatted as MMM DD, YYYY)
- Description (truncated if longer than 50 chars)
- Actions (Edit, Delete buttons)

**Given** I have more than 20 transactions
**When** I view the transaction list
**Then** I see 20 transactions per page with pagination controls

**Given** I am viewing the transaction list
**When** I click on a transaction row
**Then** I see an expanded view with full details including notes and timestamps

**Given** I am viewing the transaction list
**When** I have both income and expenses
**Then** I see a summary at the top showing total income, total expenses, and net balance for the current view

#### Technical Requirements
- Default sort: date DESC, createdAt DESC
- Pagination: 20 items per page
- Response includes total count for pagination
- Include category details in response
- Amount formatted with 2 decimal places
- User can only see their own transactions (security filter)

#### API Endpoint
```
GET /api/v1/transactions?page=0&size=20&sort=date,desc
Headers:
  Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [
    {
      "id": "uuid",
      "amount": 150.00,
      "type": "EXPENSE",
      "category": {
        "id": "uuid",
        "name": "Food",
        "type": "EXPENSE",
        "icon": "restaurant"
      },
      "date": "2026-01-20",
      "description": "Grocery shopping",
      "createdAt": "2026-01-20T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 125,
  "totalPages": 7,
  "summary": {
    "totalIncome": 5000.00,
    "totalExpenses": 3250.50,
    "netBalance": 1749.50
  }
}
```

---

### TXN-003: Edit Transaction
**Priority:** Must Have
**Story Points:** 3
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **edit an existing transaction's details**,
So that **I can correct mistakes or update information**.

#### Acceptance Criteria

**Given** I am viewing a transaction
**When** I click the "Edit" button
**Then** I see a form pre-filled with the current transaction details

**Given** I am editing a transaction
**When** I modify any field (amount, category, date, description) and save
**Then** the transaction is updated with the new values and I see "Transaction updated successfully"

**Given** I am editing a transaction
**When** I change the amount to an invalid value (negative or zero)
**Then** I see the same validation errors as transaction creation

**Given** I am editing a transaction
**When** I change the type from INCOME to EXPENSE
**Then** the category dropdown updates to show expense categories only

**Given** I am editing a transaction
**When** I click "Cancel"
**Then** no changes are saved and I return to the transaction list

**Given** I try to edit a transaction
**When** the transaction belongs to another user
**Then** I receive a 403 Forbidden error

**Given** I successfully update a transaction
**When** the save completes
**Then** the updatedAt timestamp reflects the current time

#### Technical Requirements
- Load current transaction data for edit form
- Validate user owns the transaction before allowing update
- Same validation rules as creation
- Update updatedAt timestamp automatically
- Return updated transaction in response
- Optimistic locking to prevent concurrent update conflicts (optional)

#### API Endpoint
```
PUT /api/v1/transactions/{id}
Headers:
  Authorization: Bearer {accessToken}

Request Body:
{
  "amount": 175.00,
  "type": "EXPENSE",
  "categoryId": "uuid",
  "date": "2026-01-20",
  "description": "Updated description",
  "notes": "Updated notes"
}

Response: 200 OK
{
  "id": "uuid",
  "amount": 175.00,
  "type": "EXPENSE",
  "category": {
    "id": "uuid",
    "name": "Food",
    "type": "EXPENSE"
  },
  "date": "2026-01-20",
  "description": "Updated description",
  "notes": "Updated notes",
  "createdAt": "2026-01-20T14:30:00Z",
  "updatedAt": "2026-01-20T15:45:00Z"
}
```

---

### TXN-004: Delete Transaction
**Priority:** Must Have
**Story Points:** 2
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **delete a transaction**,
So that **I can remove incorrect or duplicate entries**.

#### Acceptance Criteria

**Given** I am viewing a transaction
**When** I click the "Delete" button
**Then** I see a confirmation dialog "Are you sure you want to delete this transaction? This action cannot be undone."

**Given** I see the delete confirmation dialog
**When** I click "Confirm"
**Then** the transaction is permanently deleted and I see "Transaction deleted successfully"

**Given** I see the delete confirmation dialog
**When** I click "Cancel"
**Then** the transaction is not deleted and the dialog closes

**Given** I try to delete a transaction
**When** the transaction belongs to another user
**Then** I receive a 403 Forbidden error

**Given** I delete a transaction
**When** the transaction is part of budget tracking
**Then** budget progress is recalculated automatically to reflect the deletion

**Given** I delete a transaction
**When** the deletion is successful
**Then** the transaction is removed from the list immediately (optimistic UI update)

#### Technical Requirements
- Soft delete option for audit trail (mark as deleted, don't remove from DB)
- Hard delete for MVP (actually remove from database)
- Validate user owns the transaction before deletion
- Cascade considerations: update budget totals if applicable
- Return 204 No Content on successful deletion
- Transaction deletion triggers budget recalculation job

#### API Endpoint
```
DELETE /api/v1/transactions/{id}
Headers:
  Authorization: Bearer {accessToken}

Response: 204 No Content
```

---

### TXN-005: Filter Transactions
**Priority:** Must Have
**Story Points:** 5
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **filter my transactions by date range, category, type, and amount range**,
So that **I can find specific transactions and analyze spending patterns**.

#### Acceptance Criteria

**Given** I am on the transactions page
**When** I open the filter panel
**Then** I see filter options for: date range, type, category, and amount range

**Given** I apply a date range filter
**When** I select "Last 30 days"
**Then** I see only transactions from the past 30 days

**Given** I apply a type filter
**When** I select "EXPENSE"
**Then** I see only expense transactions (no income transactions)

**Given** I apply a category filter
**When** I select "Food"
**Then** I see only transactions in the Food category

**Given** I apply an amount range filter
**When** I set minimum 50 and maximum 200
**Then** I see only transactions with amounts between 50 and 200 (inclusive)

**Given** I apply multiple filters
**When** I select date range "Last 7 days", type "EXPENSE", and category "Food"
**Then** I see only food expense transactions from the last 7 days (AND logic)

**Given** I have applied filters
**When** I click "Clear Filters"
**Then** all filters are removed and I see all my transactions again

**Given** I have applied filters
**When** I view the transaction summary
**Then** the summary (total income, expenses, net) reflects only the filtered transactions

**Given** I apply filters
**When** I navigate away and return
**Then** the filters persist in the URL query parameters and are reapplied

#### Technical Requirements
- Date range: predefined options (Today, Last 7 days, Last 30 days, This Month, Custom)
- Custom date range: calendar picker for start and end dates
- Type: radio buttons (All, Income, Expense)
- Category: multi-select dropdown (can select multiple categories)
- Amount range: two number inputs (min and max)
- Filters combined with AND logic
- URL query parameters for shareable/bookmarkable filtered views
- Filter state persists in browser session storage
- Pagination resets to page 0 when filters change

#### API Endpoint
```
GET /api/v1/transactions?startDate=2026-01-01&endDate=2026-01-31&type=EXPENSE&categoryIds=uuid1,uuid2&minAmount=50&maxAmount=200&page=0&size=20&sort=date,desc
Headers:
  Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3,
  "summary": {
    "totalIncome": 0.00,
    "totalExpenses": 2250.00,
    "netBalance": -2250.00
  },
  "appliedFilters": {
    "startDate": "2026-01-01",
    "endDate": "2026-01-31",
    "type": "EXPENSE",
    "categoryIds": ["uuid1", "uuid2"],
    "minAmount": 50,
    "maxAmount": 200
  }
}
```

---

### TXN-006: Search Transactions
**Priority:** Should Have
**Story Points:** 3
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **search for transactions by description or notes**,
So that **I can quickly find a specific transaction**.

#### Acceptance Criteria

**Given** I am on the transactions page
**When** I enter a search term in the search box
**Then** I see transactions that contain the search term in description or notes (case-insensitive)

**Given** I enter "grocery" in the search box
**When** the search executes
**Then** I see all transactions with "grocery", "Grocery", or "GROCERY" in the description or notes

**Given** I have a search term entered
**When** I clear the search box
**Then** all transactions are displayed again

**Given** I enter a search term
**When** no transactions match
**Then** I see "No transactions found matching your search"

**Given** I have both filters and a search term applied
**When** I view the results
**Then** I see transactions matching both the search term AND the filters

**Given** I enter a search term
**When** I wait 300ms after typing stops (debounce)
**Then** the search executes automatically

**Given** I search for a partial word like "groc"
**When** the search executes
**Then** I see transactions containing words starting with "groc" (e.g., "grocery", "groceries")

#### Technical Requirements
- Search fields: description, notes
- Case-insensitive search
- Partial match support (use ILIKE in PostgreSQL)
- Debounce search input: 300ms delay after typing stops
- Combine with filters using AND logic
- Highlight search terms in results (optional, frontend)
- Search indexed for performance (database index on description)
- Minimum search length: 2 characters

#### API Endpoint
```
GET /api/v1/transactions?search=grocery&page=0&size=20
Headers:
  Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [
    {
      "id": "uuid",
      "amount": 150.00,
      "type": "EXPENSE",
      "category": {
        "id": "uuid",
        "name": "Food",
        "type": "EXPENSE"
      },
      "date": "2026-01-20",
      "description": "Grocery shopping at Whole Foods",
      "notes": "Weekly groceries",
      "createdAt": "2026-01-20T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 8,
  "totalPages": 1,
  "summary": {
    "totalIncome": 0.00,
    "totalExpenses": 1250.00,
    "netBalance": -1250.00
  }
}
```

---

### TXN-007: Transaction Categories Management
**Priority:** Must Have
**Story Points:** 3
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **view and select from predefined transaction categories**,
So that **I can consistently categorize my income and expenses**.

#### Acceptance Criteria

**Given** I am creating or editing a transaction
**When** I select transaction type "INCOME"
**Then** I see income categories: Salary, Freelance, Investments, Gifts, Refunds, Other Income

**Given** I am creating or editing a transaction
**When** I select transaction type "EXPENSE"
**Then** I see expense categories: Food & Dining, Transportation, Housing, Utilities, Entertainment, Healthcare, Shopping, Education, Travel, Personal Care, Subscriptions, Other

**Given** I am viewing the categories dropdown
**When** the dropdown opens
**Then** categories are displayed with icons and organized alphabetically

**Given** I am on the transactions page
**When** I view transactions
**Then** each transaction displays its category name and icon

**Given** I am viewing transaction statistics
**When** I group by category
**Then** uncategorized transactions appear under "Uncategorized" category

#### Technical Requirements
- Categories table with predefined seed data
- Category fields: id, name, type (INCOME/EXPENSE), icon, color, display order
- Categories are system-defined (not user-created in MVP)
- Categories loaded once and cached on frontend
- Each transaction must have exactly one category
- Category soft-delete support (hide but don't remove if transactions exist)

#### API Endpoint
```
GET /api/v1/categories
Headers:
  Authorization: Bearer {accessToken}

Response: 200 OK
{
  "income": [
    {
      "id": "uuid",
      "name": "Salary",
      "type": "INCOME",
      "icon": "payments",
      "color": "#4CAF50"
    },
    {
      "id": "uuid",
      "name": "Freelance",
      "type": "INCOME",
      "icon": "work",
      "color": "#8BC34A"
    }
  ],
  "expense": [
    {
      "id": "uuid",
      "name": "Food & Dining",
      "type": "EXPENSE",
      "icon": "restaurant",
      "color": "#F44336"
    },
    {
      "id": "uuid",
      "name": "Transportation",
      "type": "EXPENSE",
      "icon": "directions_car",
      "color": "#E91E63"
    }
  ]
}
```

---

### TXN-008: Transaction Pagination and Sorting
**Priority:** Must Have
**Story Points:** 2
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **navigate through my transactions with pagination and sort by different columns**,
So that **I can efficiently browse large lists of transactions**.

#### Acceptance Criteria

**Given** I have more than 20 transactions
**When** I view the transactions page
**Then** I see pagination controls showing current page, total pages, and navigation buttons

**Given** I am on page 1 of transactions
**When** I click "Next"
**Then** I see page 2 with the next 20 transactions

**Given** I am viewing transactions
**When** I click the "Date" column header
**Then** transactions are sorted by date in ascending order, and clicking again sorts descending

**Given** I am viewing transactions
**When** I click the "Amount" column header
**Then** transactions are sorted by amount (lowest to highest), and clicking again reverses the order

**Given** I am viewing transactions
**When** I click the "Category" column header
**Then** transactions are sorted alphabetically by category name

**Given** I am on page 3 of transactions
**When** I apply a filter
**Then** pagination resets to page 1 with filtered results

**Given** I am viewing transactions
**When** I change the page size to 50 items per page
**Then** I see up to 50 transactions per page and pagination updates accordingly

**Given** I navigate to a specific page
**When** I refresh the browser
**Then** I remain on the same page with the same sort order (persisted in URL)

#### Technical Requirements
- Default pagination: page 0, size 20
- Supported page sizes: 10, 20, 50, 100
- Sortable columns: date, amount, category, createdAt
- Sort direction: asc, desc
- URL parameters: page, size, sort
- Show "Showing X-Y of Z transactions"
- Disable "Previous" on first page, "Next" on last page
- Jump to page input for quick navigation (optional)

#### API Endpoint
```
GET /api/v1/transactions?page=2&size=20&sort=amount,desc
Headers:
  Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [...],
  "page": 2,
  "size": 20,
  "totalElements": 125,
  "totalPages": 7,
  "first": false,
  "last": false,
  "summary": {
    "totalIncome": 5000.00,
    "totalExpenses": 3250.50,
    "netBalance": 1749.50
  }
}
```

---

### TXN-009: Transaction Summary and Statistics
**Priority:** Should Have
**Story Points:** 3
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **see summary statistics of my transactions including totals by type and category breakdown**,
So that **I can understand my spending patterns at a glance**.

#### Acceptance Criteria

**Given** I am on the transactions page
**When** the page loads
**Then** I see a summary card displaying total income, total expenses, and net balance

**Given** I have filtered transactions
**When** I view the summary
**Then** the summary reflects only the filtered transactions

**Given** I am viewing the transaction summary
**When** I click "View Category Breakdown"
**Then** I see a breakdown showing total amount and percentage for each category

**Given** I am viewing the category breakdown
**When** I have both income and expenses
**Then** I see separate breakdowns for income categories and expense categories

**Given** I am viewing the category breakdown
**When** I click on a category
**Then** the transaction list filters to show only transactions in that category

**Given** I select a date range
**When** I view the summary
**Then** I see a comparison with the previous period (e.g., "15% increase from last month")

**Given** I am viewing statistics
**When** I have no transactions
**Then** I see "No transactions yet" with a call-to-action to create the first transaction

#### Technical Requirements
- Calculate totals: SUM(amount) WHERE type = 'INCOME', SUM(amount) WHERE type = 'EXPENSE'
- Net balance: total income - total expenses
- Category breakdown: GROUP BY category, calculate percentage of total
- Comparison calculations: compare current period with previous equal-length period
- Format currency with proper symbols and separators
- Color coding: green for positive, red for negative, gray for zero
- Cache summary calculations for performance (optional)

#### API Endpoint
```
GET /api/v1/transactions/summary?startDate=2026-01-01&endDate=2026-01-31
Headers:
  Authorization: Bearer {accessToken}

Response: 200 OK
{
  "period": {
    "startDate": "2026-01-01",
    "endDate": "2026-01-31"
  },
  "totals": {
    "income": 5000.00,
    "expenses": 3250.50,
    "net": 1749.50,
    "transactionCount": 87
  },
  "categoryBreakdown": {
    "income": [
      {
        "category": {
          "id": "uuid",
          "name": "Salary",
          "icon": "payments"
        },
        "total": 4500.00,
        "percentage": 90.0,
        "transactionCount": 2
      }
    ],
    "expense": [
      {
        "category": {
          "id": "uuid",
          "name": "Food & Dining",
          "icon": "restaurant"
        },
        "total": 850.00,
        "percentage": 26.15,
        "transactionCount": 18
      }
    ]
  },
  "comparison": {
    "previousPeriod": {
      "income": 4500.00,
      "expenses": 2800.00,
      "net": 1700.00
    },
    "change": {
      "income": "+11.1%",
      "expenses": "+16.1%",
      "net": "+2.9%"
    }
  }
}
```

---

### TXN-010: Bulk Operations
**Priority:** Could Have
**Story Points:** 5
**Status:** Backlog

#### User Story
As a **logged-in user**,
I want to **select multiple transactions and perform bulk actions (delete, change category)**,
So that **I can efficiently manage many transactions at once**.

#### Acceptance Criteria

**Given** I am viewing the transaction list
**When** I enable bulk selection mode
**Then** I see checkboxes next to each transaction

**Given** I am in bulk selection mode
**When** I check multiple transaction checkboxes
**Then** I see a bulk actions toolbar with "Delete Selected" and "Change Category"

**Given** I have selected 10 transactions
**When** I click "Delete Selected"
**Then** I see a confirmation "Are you sure you want to delete 10 transactions?"

**Given** I confirm bulk delete
**When** the deletion completes
**Then** all selected transactions are deleted and I see "10 transactions deleted successfully"

**Given** I have selected transactions
**When** I click "Change Category" and select a new category
**Then** all selected transactions are updated with the new category

**Given** I select transactions of different types (income and expense)
**When** I click "Change Category"
**Then** I see "Cannot change category for transactions of mixed types. Please select transactions of the same type."

**Given** I select all transactions on a page
**When** I click "Select All" checkbox in the header
**Then** all visible transactions are selected

#### Technical Requirements
- Bulk delete: single API call with array of transaction IDs
- Bulk update: transaction validation (same type for category change)
- Maximum bulk operation size: 100 transactions at once
- Optimistic UI updates with rollback on error
- Progress indicator for large bulk operations
- Audit log for bulk operations
- Permissions check: user owns all selected transactions

#### API Endpoint
```
DELETE /api/v1/transactions/bulk
Headers:
  Authorization: Bearer {accessToken}

Request Body:
{
  "transactionIds": ["uuid1", "uuid2", "uuid3"]
}

Response: 200 OK
{
  "deletedCount": 3,
  "message": "3 transactions deleted successfully"
}

PATCH /api/v1/transactions/bulk
Request Body:
{
  "transactionIds": ["uuid1", "uuid2"],
  "categoryId": "new-category-uuid"
}

Response: 200 OK
{
  "updatedCount": 2,
  "message": "2 transactions updated successfully"
}
```

---

## Story Summary

| Story ID | Title | Priority | Story Points | Dependencies |
|----------|-------|----------|--------------|--------------|
| TXN-001 | Create Transaction | Must Have | 5 | TXN-007 |
| TXN-002 | View Transaction List | Must Have | 3 | TXN-001 |
| TXN-003 | Edit Transaction | Must Have | 3 | TXN-001, TXN-002 |
| TXN-004 | Delete Transaction | Must Have | 2 | TXN-002 |
| TXN-005 | Filter Transactions | Must Have | 5 | TXN-002 |
| TXN-006 | Search Transactions | Should Have | 3 | TXN-002 |
| TXN-007 | Transaction Categories | Must Have | 3 | None |
| TXN-008 | Pagination and Sorting | Must Have | 2 | TXN-002 |
| TXN-009 | Summary and Statistics | Should Have | 3 | TXN-002 |
| TXN-010 | Bulk Operations | Could Have | 5 | TXN-002 |

**Total Story Points:** 34 (MVP Must-Have: 23, Should-Have: 6, Could-Have: 5)

---

## MoSCoW Prioritization

### Must Have (MVP Critical)
- TXN-001: Create Transaction
- TXN-002: View Transaction List
- TXN-003: Edit Transaction
- TXN-004: Delete Transaction
- TXN-005: Filter Transactions
- TXN-007: Transaction Categories Management
- TXN-008: Pagination and Sorting

### Should Have (Important for Usability)
- TXN-006: Search Transactions
- TXN-009: Transaction Summary and Statistics

### Could Have (Post-MVP)
- TXN-010: Bulk Operations
- Transaction attachments (receipts)
- Recurring transactions
- Transaction tags
- Advanced analytics

### Won't Have (Not in Scope)
- Bank account integration / auto-import
- OCR receipt scanning
- Multi-currency support
- Split transactions

---

## Business Rules

### Transaction Validation
- Amount must be greater than 0
- Amount maximum: 10,000,000
- Date cannot be more than 1 year in the past
- Date can be in the future (with warning)
- Description maximum: 500 characters
- Notes maximum: 1000 characters
- Category must match transaction type

### Data Integrity
- User can only see/edit/delete their own transactions
- Deleting a transaction updates related budget calculations
- Changing transaction amount/category updates budget progress
- Transaction type cannot be changed if transaction is linked to budget
- Soft delete option for audit trail (configurable)

### Performance
- Default page size: 20 transactions
- Maximum page size: 100 transactions
- Database indexes on: user_id, date, category_id, type
- Full-text search index on description field
- Cache category list (rarely changes)

---

## Database Schema

### transactions table
```sql
CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
  type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
  category_id UUID NOT NULL REFERENCES categories(id),
  date DATE NOT NULL,
  description VARCHAR(500),
  notes VARCHAR(1000),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(date);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_user_date ON transactions(user_id, date DESC);

-- Full-text search index
CREATE INDEX idx_transactions_description ON transactions USING gin(to_tsvector('english', description));
```

### categories table
```sql
CREATE TABLE categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
  icon VARCHAR(50),
  color VARCHAR(7),
  display_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_type ON categories(type);

-- Seed data
INSERT INTO categories (name, type, icon, color, display_order) VALUES
('Salary', 'INCOME', 'payments', '#4CAF50', 1),
('Freelance', 'INCOME', 'work', '#8BC34A', 2),
('Investments', 'INCOME', 'trending_up', '#66BB6A', 3),
('Gifts', 'INCOME', 'card_giftcard', '#81C784', 4),
('Refunds', 'INCOME', 'receipt', '#9CCC65', 5),
('Other Income', 'INCOME', 'attach_money', '#AED581', 6),
('Food & Dining', 'EXPENSE', 'restaurant', '#F44336', 1),
('Transportation', 'EXPENSE', 'directions_car', '#E91E63', 2),
('Housing', 'EXPENSE', 'home', '#9C27B0', 3),
('Utilities', 'EXPENSE', 'bolt', '#673AB7', 4),
('Entertainment', 'EXPENSE', 'movie', '#3F51B5', 5),
('Healthcare', 'EXPENSE', 'local_hospital', '#2196F3', 6),
('Shopping', 'EXPENSE', 'shopping_bag', '#03A9F4', 7),
('Education', 'EXPENSE', 'school', '#00BCD4', 8),
('Travel', 'EXPENSE', 'flight', '#009688', 9),
('Personal Care', 'EXPENSE', 'spa', '#4CAF50', 10),
('Subscriptions', 'EXPENSE', 'subscriptions', '#8BC34A', 11),
('Other', 'EXPENSE', 'more_horiz', '#CDDC39', 12);
```

---

## Testing Requirements

### Unit Tests
- Transaction amount validation (positive, max value)
- Transaction type and category compatibility
- Date validation (past, present, future)
- Description and notes length validation
- Filter logic (multiple conditions)
- Search functionality (case-insensitive, partial match)
- Pagination calculations
- Summary calculations (totals, percentages)

### Integration Tests
- Create transaction with valid/invalid data
- Retrieve transactions with pagination
- Update transaction and verify changes
- Delete transaction and verify removal
- Filter transactions by multiple criteria
- Search transactions by description/notes
- Category assignment and retrieval
- Budget recalculation after transaction changes

### E2E Tests (Cypress)
- User can create a new transaction
- User can view list of transactions with pagination
- User can edit an existing transaction
- User can delete a transaction with confirmation
- User can filter transactions by date and category
- User can search for transactions
- User can see transaction summary and statistics
- Validation errors display correctly

### Performance Tests
- Load 1000+ transactions with acceptable response time (<500ms)
- Pagination with large datasets
- Filter and search performance with indexes
- Summary calculation performance

---

## Definition of Done

Each story is considered complete when:

1. **Code Complete**
   - Backend API endpoints implemented with validation
   - Frontend forms and list views implemented
   - All CRUD operations functional

2. **Tests Pass**
   - Unit tests: >80% coverage
   - Integration tests: all happy and sad paths
   - E2E tests: critical user flows

3. **Validation**
   - All business rules enforced
   - Error messages are clear and helpful
   - Edge cases handled gracefully

4. **UI/UX**
   - Forms are intuitive and accessible
   - Loading states and error states implemented
   - Responsive design works on mobile and desktop

5. **Documentation**
   - API endpoints documented in OpenAPI/Swagger
   - Frontend component documentation
   - Database schema documented

6. **Code Review**
   - PR approved by at least one team member
   - No critical issues from static analysis
   - Follows coding standards

7. **Acceptance Criteria Met**
   - All Given/When/Then scenarios pass
   - Product Owner accepts the story

---

## Sprint Planning Recommendation

### Sprint 1 (Foundation - 11 points)
- TXN-007: Transaction Categories Management (3)
- TXN-001: Create Transaction (5)
- TXN-002: View Transaction List (3)

### Sprint 2 (CRUD Completion - 10 points)
- TXN-003: Edit Transaction (3)
- TXN-004: Delete Transaction (2)
- TXN-005: Filter Transactions (5)

### Sprint 3 (Enhanced Features - 8 points)
- TXN-008: Pagination and Sorting (2)
- TXN-006: Search Transactions (3)
- TXN-009: Summary and Statistics (3)

### Sprint 4 (Optional Enhancements - 5 points)
- TXN-010: Bulk Operations (5)

---

## Notes for Development Team

1. **Environment Variables Required:**
   - `DEFAULT_PAGE_SIZE` - Default pagination size (20)
   - `MAX_PAGE_SIZE` - Maximum pagination size (100)
   - `MAX_TRANSACTION_AMOUNT` - Maximum transaction amount (10000000)

2. **Backend Libraries:**
   - Spring Data JPA (database access)
   - Bean Validation (JSR-380)
   - MapStruct (entity-DTO mapping)
   - QueryDSL (dynamic filtering)

3. **Frontend Libraries:**
   - React Hook Form (form validation)
   - Material-UI Data Grid (transaction list)
   - Date-fns (date formatting and manipulation)
   - React Query (API state management and caching)

4. **Performance Considerations:**
   - Index all foreign keys and frequently filtered columns
   - Use pagination for all list endpoints
   - Implement database-level filtering (don't filter in memory)
   - Cache category list on frontend
   - Consider Redis for summary calculations cache

5. **Security:**
   - Always filter transactions by authenticated user ID
   - Validate user ownership before update/delete
   - Sanitize search input to prevent SQL injection
   - Rate limit transaction creation (prevent spam)

6. **Accessibility:**
   - Form labels and ARIA attributes
   - Keyboard navigation for transaction list
   - Screen reader announcements for success/error messages
   - Focus management for modals

---

## API Response Examples

### Error Responses

```json
// 400 Bad Request - Validation Error
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "amount",
      "message": "Amount must be greater than 0"
    },
    {
      "field": "categoryId",
      "message": "Category is required"
    }
  ],
  "timestamp": "2026-01-23T10:30:00Z"
}

// 403 Forbidden - Not Owner
{
  "status": 403,
  "message": "You do not have permission to access this transaction",
  "timestamp": "2026-01-23T10:30:00Z"
}

// 404 Not Found
{
  "status": 404,
  "message": "Transaction not found",
  "timestamp": "2026-01-23T10:30:00Z"
}
```

---

## References

- Personal Finance App PRD
- Epic 1: Authentication User Stories
- Database Design Documentation
- API Design Guidelines (RESTful best practices)
- Material-UI Data Grid Documentation
