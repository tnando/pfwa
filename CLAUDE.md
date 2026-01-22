# Personal Finance Web App (PFWA)

## Project Overview
A personal finance web application for tracking income, expenses, budgets, and financial insights.

## Tech Stack
- **Frontend:** React 18+, TypeScript, Vite, Material-UI
- **Backend:** Java 17+, Spring Boot 3.2+, Spring Security (JWT)
- **Database:** PostgreSQL 15+, Flyway migrations
- **Testing:** JUnit 5, Vitest, Cypress

## Project Structure
```
pfwa/
├── frontend/              # React/TypeScript application
├── backend/               # Spring Boot API
├── docker/                # Docker Compose, Dockerfiles
├── docs/                  # Architecture, ADRs, requirements
└── .claude/               # Claude Code configuration
    ├── agents/            # Specialized AI agents
    ├── skills/            # Custom slash commands
    └── rules/             # Coding standards
```

## Development Workflow

### Branch Naming
```
feature/<description>    # feature/add-transaction-form
bugfix/<description>     # bugfix/fix-amount-validation
hotfix/<description>     # hotfix/security-patch
```

### Commit Format
```
<type>(<scope>): <description>

Refs: #<issue-number>
```
Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

## MVP Features

### Epic 1: Authentication
- User registration and login (JWT)
- Password reset via email

### Epic 2: Transactions
- Manual transaction entry (income/expenses)
- Categorization, filtering, pagination
- Full CRUD operations

### Epic 3: Budgets
- Monthly budgets by category
- Progress tracking with alerts at 80%/100%
- Email notifications

### Epic 4: Dashboard
- Key metrics (income, expenses, savings)
- Charts: spending by category, trends
- CSV export

## Agents
Use `@agent-name` to invoke specialized agents:
- `@architect` - System design, API contracts, ADRs
- `@backend` - Spring Boot APIs, services, security
- `@frontend` - React components, pages, state management
- `@database` - Schema design, migrations, JPA entities
- `@devops` - Docker, CI/CD, deployment
- `@qa` - Testing strategy, unit/integration/E2E tests
- `@pm` - Requirements, user stories, acceptance criteria

## Quick Commands
- `/commit` - Commit with conventional format
- `/test` - Run test suite
- `/build` - Build frontend and backend
