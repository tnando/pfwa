---
paths:
  - "backend/**/*.java"
---

# Spring Boot Standards

## Architecture
- Layered: Controller → Service → Repository
- Controllers are thin (validation, routing only)
- Business logic in Service layer
- Data access via Repository interfaces

## Package Structure
```
com.finance.app/
├── controller/    # @RestController
├── service/       # @Service
├── repository/    # @Repository
├── model/         # @Entity
├── dto/           # Request/Response objects
├── mapper/        # Entity-DTO conversion
├── security/      # JWT, auth filters
├── exception/     # @ControllerAdvice
└── config/        # @Configuration
```

## Naming Conventions
- Controllers: `*Controller`
- Services: `*Service`
- Repositories: `*Repository`
- DTOs: `*Request`, `*Response`
- Entities: Singular nouns (User, Transaction)

## REST API Standards
- Use proper HTTP methods (GET, POST, PUT, DELETE)
- Plural resource names (`/api/transactions`)
- Proper status codes (200, 201, 400, 401, 404)
- Pagination for list endpoints

## Security
- All passwords hashed with BCrypt
- JWT tokens validated on every request
- No sensitive data in logs
- Input validation on all endpoints

## Testing
- >80% coverage for services
- MockMvc for controller tests
- @MockBean for dependencies
- TestContainers for integration tests
