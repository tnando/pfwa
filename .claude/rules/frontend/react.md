---
paths:
  - "frontend/**/*.tsx"
  - "frontend/**/*.ts"
---

# React/TypeScript Standards

## Component Guidelines
- Use functional components with hooks
- Maximum 300 lines per component
- Single Responsibility Principle
- Props interface defined with TypeScript
- Handle loading, error, and empty states

## File Organization
- One component per file
- Component name matches filename
- Group related components in folders

## Naming Conventions
- Components: PascalCase (`TransactionForm.tsx`)
- Hooks: camelCase with `use` prefix (`useAuth.ts`)
- Utils: camelCase (`formatCurrency.ts`)
- Types: PascalCase (`Transaction.ts`)

## State Management
- Use local state when possible
- Lift state up when shared
- Context for global state (auth, theme)
- Custom hooks for reusable logic

## TypeScript Rules
- Strict mode enabled
- No `any` types without justification
- Define interfaces for all props
- Use type inference where obvious

## Testing
- Test file: `ComponentName.test.tsx`
- Test user interactions
- Test loading and error states
- Use React Testing Library
