# ADR-002: Transaction Architecture for PFWA

## Status

**Accepted**

Date: 2026-01-23

## Context

The Personal Finance Web Application (PFWA) requires a robust transaction management system to track user income and expenses. This system is the core financial data model that other features (budgets, reports, insights) will depend on.

### Requirements

1. **CRUD Operations**: Full create, read, update, delete functionality for transactions
2. **Categorization**: Each transaction must belong to a category matching its type
3. **Filtering & Search**: Support complex filtering by date, type, category, amount, and text search
4. **Pagination & Sorting**: Efficient handling of large transaction sets
5. **Summary Statistics**: Real-time calculations for totals, breakdowns, and comparisons
6. **Security**: Users can only access their own transactions
7. **Data Integrity**: Enforce business rules at application and database levels
8. **Performance**: Sub-500ms response times for list operations with 1000+ transactions

### Options Considered

#### Option 1: Simple CRUD with In-Memory Filtering

Fetch all user transactions and filter/sort in application memory.

**Pros:**
- Simple implementation
- Flexible filtering logic
- No complex SQL queries

**Cons:**
- Poor performance with large datasets
- High memory usage
- Pagination breaks with in-memory filtering
- Not scalable

#### Option 2: Spring Data JPA Specifications with Database Filtering

Use Spring Data JPA Specifications for dynamic query building with database-level filtering.

**Pros:**
- Filtering done at database level (efficient)
- Type-safe query building
- Integrates well with Spring Data pagination
- Good performance with proper indexes

**Cons:**
- Moderate complexity for dynamic filters
- Need to manage query optimization
- Specifications can become verbose

#### Option 3: QueryDSL with Spring Data JPA

Use QueryDSL for type-safe, fluent dynamic query building.

**Pros:**
- Highly readable, fluent API
- Type-safe with generated Q-classes
- Excellent IDE support
- Powerful predicate composition

**Cons:**
- Additional build-time code generation
- Extra dependency
- Learning curve for team

## Decision

We will implement **Spring Data JPA Specifications with database-level filtering** (Option 2), with the possibility of migrating to QueryDSL in the future if complexity warrants it.

### Architecture Overview

```
+----------------+     +------------------+     +-------------------+
|   React SPA    |     |   Spring Boot    |     |    PostgreSQL     |
|                |     |                  |     |                   |
|  Transaction   |     |  Transaction     |     |  transactions     |
|  Components    +---->+  Controller      +---->+  table            |
|                |     |       |          |     |                   |
|  Filter Panel  |     |       v          |     |  categories       |
|                |     |  Transaction     |     |  table            |
|  Data Grid     |     |  Service         |     |                   |
|                |     |       |          |     |  indexes          |
+----------------+     |       v          |     |                   |
                       |  Transaction     |     +-------------------+
                       |  Repository      |
                       |  (JPA + Specs)   |
                       +------------------+
```

### Layered Architecture

#### Controller Layer

The `TransactionController` handles HTTP requests and delegates to the service layer.

```java
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<TransactionListResponse> listTransactions(
            @Valid TransactionFilterRequest filter,
            @PageableDefault(size = 20, sort = "date", direction = DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal user) {

        return ResponseEntity.ok(
            transactionService.findTransactions(user.getId(), filter, pageable)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return transactionService.createTransaction(user.getId(), request);
    }

    @PutMapping("/{id}")
    public TransactionResponse updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return transactionService.updateTransaction(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {

        transactionService.deleteTransaction(user.getId(), id);
    }
}
```

#### Service Layer

The `TransactionService` contains business logic and coordinates between controller and repository.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    public TransactionListResponse findTransactions(
            UUID userId, TransactionFilterRequest filter, Pageable pageable) {

        Specification<Transaction> spec = TransactionSpecifications
            .forUser(userId)
            .and(TransactionSpecifications.withFilters(filter));

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        TransactionListSummary summary = calculateSummary(userId, filter);

        return transactionMapper.toListResponse(page, summary, filter);
    }

    @Transactional
    public TransactionResponse createTransaction(UUID userId, CreateTransactionRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        validateCategoryTypeMatch(request.getType(), category.getType());

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUserId(userId);
        transaction.setCategory(category);

        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Transactional
    public TransactionResponse updateTransaction(
            UUID userId, UUID transactionId, UpdateTransactionRequest request) {

        Transaction transaction = findUserTransaction(userId, transactionId);

        if (!transaction.getCategoryId().equals(request.getCategoryId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
            validateCategoryTypeMatch(request.getType(), newCategory.getType());
            transaction.setCategory(newCategory);
        }

        transactionMapper.updateEntity(request, transaction);
        Transaction updated = transactionRepository.save(transaction);
        return transactionMapper.toResponse(updated);
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = findUserTransaction(userId, transactionId);
        transactionRepository.delete(transaction);
    }

    private Transaction findUserTransaction(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    private void validateCategoryTypeMatch(TransactionType txnType, TransactionType catType) {
        if (txnType != catType) {
            throw new CategoryTypeMismatchException(txnType, catType);
        }
    }
}
```

#### Repository Layer

The `TransactionRepository` extends Spring Data JPA with custom queries.

```java
@Repository
public interface TransactionRepository extends
        JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        SELECT NEW com.pfwa.dto.TransactionSummaryDto(
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
            COUNT(t)
        )
        FROM Transaction t
        WHERE t.userId = :userId
        AND (:startDate IS NULL OR t.date >= :startDate)
        AND (:endDate IS NULL OR t.date <= :endDate)
        """)
    TransactionSummaryDto calculateSummary(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT NEW com.pfwa.dto.CategoryBreakdownDto(
            t.category.id,
            t.category.name,
            t.category.icon,
            t.category.color,
            t.type,
            SUM(t.amount),
            COUNT(t)
        )
        FROM Transaction t
        WHERE t.userId = :userId
        AND (:startDate IS NULL OR t.date >= :startDate)
        AND (:endDate IS NULL OR t.date <= :endDate)
        GROUP BY t.category.id, t.category.name, t.category.icon, t.category.color, t.type
        ORDER BY SUM(t.amount) DESC
        """)
    List<CategoryBreakdownDto> getCategoryBreakdown(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

#### Specification Layer

Dynamic query building using Spring Data JPA Specifications.

```java
public class TransactionSpecifications {

    public static Specification<Transaction> forUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Transaction> withFilters(TransactionFilterRequest filter) {
        return Specification.where(dateRange(filter.getStartDate(), filter.getEndDate()))
            .and(transactionType(filter.getType()))
            .and(categories(filter.getCategoryIds()))
            .and(amountRange(filter.getMinAmount(), filter.getMaxAmount()))
            .and(searchText(filter.getSearch()));
    }

    private static Specification<Transaction> dateRange(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<Transaction> transactionType(TransactionType type) {
        return (root, query, cb) ->
            type == null ? null : cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> categories(List<UUID> categoryIds) {
        return (root, query, cb) ->
            categoryIds == null || categoryIds.isEmpty()
                ? null
                : root.get("category").get("id").in(categoryIds);
    }

    private static Specification<Transaction> amountRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), min));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), max));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<Transaction> searchText(String search) {
        return (root, query, cb) -> {
            if (search == null || search.length() < 2) {
                return null;
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern)
            );
        };
    }
}
```

### Entity Design

```java
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_user_id", columnList = "user_id"),
    @Index(name = "idx_transactions_user_date", columnList = "user_id, date DESC"),
    @Index(name = "idx_transactions_category_id", columnList = "category_id"),
    @Index(name = "idx_transactions_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String description;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_categories_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 50)
    private String icon;

    @Column(length = 7)
    private String color;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
```

### Database Schema

```sql
-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category_id UUID NOT NULL REFERENCES categories(id),
    date DATE NOT NULL,
    description VARCHAR(500),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Performance indexes
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_user_date ON transactions(user_id, date DESC);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
CREATE INDEX idx_transactions_type ON transactions(type);

-- Full-text search index (PostgreSQL specific)
CREATE INDEX idx_transactions_description_gin ON transactions
    USING gin(to_tsvector('english', COALESCE(description, '') || ' ' || COALESCE(notes, '')));

-- Categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    icon VARCHAR(50),
    color VARCHAR(7),
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_type ON categories(type);
CREATE INDEX idx_categories_active ON categories(is_active) WHERE is_active = true;
```

### Validation Strategy

#### Request Validation (Bean Validation)

```java
public class CreateTransactionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "10000000", message = "Amount exceeds maximum allowed value")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate date;

    @Size(max = 500, message = "Description must be 500 characters or less")
    private String description;

    @Size(max = 1000, message = "Notes must be 1000 characters or less")
    private String notes;
}
```

#### Business Rule Validation

```java
@Service
public class TransactionValidator {

    private static final int MAX_PAST_DAYS = 365;

    public void validateTransaction(CreateTransactionRequest request, Category category) {
        List<String> errors = new ArrayList<>();

        // Category type must match transaction type
        if (request.getType() != category.getType()) {
            errors.add("Category type must match transaction type");
        }

        // Date cannot be more than 1 year in the past
        if (request.getDate().isBefore(LocalDate.now().minusDays(MAX_PAST_DAYS))) {
            errors.add("Transaction date cannot be more than 1 year in the past");
        }

        // Category must be active
        if (!category.isActive()) {
            errors.add("Selected category is no longer available");
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }
}
```

### Security Implementation

#### User Isolation

All transaction queries are scoped to the authenticated user:

```java
// Always filter by user ID at repository level
Specification<Transaction> spec = TransactionSpecifications.forUser(userId)
    .and(TransactionSpecifications.withFilters(filter));
```

#### Authorization Checks

```java
@PreAuthorize("isAuthenticated()")
public class TransactionController {

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new TransactionNotFoundException(id));
        return transactionMapper.toResponse(transaction);
    }
}
```

### Performance Optimizations

#### Database Indexes

```sql
-- Composite index for user + date filtering (most common query pattern)
CREATE INDEX idx_transactions_user_date ON transactions(user_id, date DESC);

-- Index for category filtering
CREATE INDEX idx_transactions_category_id ON transactions(category_id);

-- Index for type filtering
CREATE INDEX idx_transactions_type ON transactions(type);

-- Partial index for amount range queries
CREATE INDEX idx_transactions_amount ON transactions(amount)
    WHERE amount > 0;
```

#### Query Optimization

```java
// Fetch categories in batch to avoid N+1
@EntityGraph(attributePaths = {"category"})
Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

// Summary query with single database round trip
@Query("SELECT NEW ... FROM Transaction t WHERE ... GROUP BY ...")
TransactionSummaryDto calculateSummary(...);
```

#### Pagination Limits

```java
@Configuration
public class PaginationConfig {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer paginationCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(MAX_PAGE_SIZE);
            resolver.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
        };
    }
}
```

### DTO Mapping with MapStruct

```java
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(CreateTransactionRequest request);

    @Mapping(source = "category.id", target = "category.id")
    @Mapping(source = "category.name", target = "category.name")
    @Mapping(source = "category.type", target = "category.type")
    @Mapping(source = "category.icon", target = "category.icon")
    @Mapping(source = "category.color", target = "category.color")
    TransactionResponse toResponse(Transaction transaction);

    default TransactionListResponse toListResponse(
            Page<Transaction> page,
            TransactionListSummary summary,
            TransactionFilterRequest appliedFilters) {

        return TransactionListResponse.builder()
            .content(page.getContent().stream().map(this::toResponse).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .summary(summary)
            .appliedFilters(appliedFilters)
            .build();
    }

    void updateEntity(UpdateTransactionRequest request, @MappingTarget Transaction transaction);
}
```

### Rate Limiting Strategy

| Endpoint | Limit | Window |
|----------|-------|--------|
| GET /transactions | 60 requests | 1 minute |
| POST /transactions | 30 requests | 1 minute |
| PUT /transactions/{id} | 30 requests | 1 minute |
| DELETE /transactions/{id} | 30 requests | 1 minute |
| GET /transactions/summary | 30 requests | 1 minute |
| GET /categories | 60 requests | 1 minute |

### Error Handling

```java
@RestControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionNotFoundException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .error("NOT_FOUND")
                .message("Transaction not found")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(CategoryTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleCategoryTypeMismatch(
            CategoryTypeMismatchException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message("Validation failed")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .fieldErrors(List.of(
                    FieldError.builder()
                        .field("categoryId")
                        .message("Category type must match transaction type")
                        .code("category.type.mismatch")
                        .build()
                ))
                .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.builder()
                .error("FORBIDDEN")
                .message("You do not have permission to access this transaction")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build());
    }
}
```

## Consequences

### Positive

1. **Performance**: Database-level filtering with proper indexes ensures fast queries even with large datasets
2. **Security**: User isolation at repository level prevents unauthorized data access
3. **Maintainability**: Clear separation of concerns across layers
4. **Type Safety**: Compile-time checking with Spring Data JPA Specifications
5. **Flexibility**: Easy to add new filter criteria without changing core logic
6. **Testability**: Each layer can be unit tested independently
7. **Standards Compliance**: RESTful API design with proper HTTP status codes

### Negative

1. **Complexity**: More code than simple CRUD operations
2. **Learning Curve**: Team needs to understand Specifications pattern
3. **Query Optimization**: Need to monitor and tune queries as data grows
4. **Eager Loading**: Must be careful to avoid N+1 queries

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| N+1 query problems | Use `@EntityGraph` for eager loading of categories |
| Full-text search performance | PostgreSQL GIN indexes, consider Elasticsearch for future |
| Summary calculation bottleneck | Database aggregation queries, cache for frequent periods |
| Large pagination offset | Keyset pagination for deep pages (future enhancement) |
| Concurrent updates | Optimistic locking with `@Version` (future enhancement) |

## Implementation Notes

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

### Environment Variables

```properties
# Pagination
DEFAULT_PAGE_SIZE=20
MAX_PAGE_SIZE=100

# Validation
MAX_TRANSACTION_AMOUNT=10000000
MAX_DESCRIPTION_LENGTH=500
MAX_NOTES_LENGTH=1000
MAX_PAST_DAYS=365

# Rate Limiting
RATE_LIMIT_TRANSACTIONS_READ=60
RATE_LIMIT_TRANSACTIONS_WRITE=30
```

### Testing Strategy

1. **Unit Tests**: Service layer with mocked repository
2. **Integration Tests**: Repository layer with embedded PostgreSQL (Testcontainers)
3. **API Tests**: Controller layer with MockMvc
4. **E2E Tests**: Full stack tests with Cypress

Example test:

```java
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Test
    @WithMockUser
    void createTransaction_ValidRequest_ReturnsCreated() throws Exception {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .amount(new BigDecimal("150.00"))
            .type(TransactionType.EXPENSE)
            .categoryId(UUID.randomUUID())
            .date(LocalDate.now())
            .description("Test transaction")
            .build();

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.amount").value(150.00))
            .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    @WithMockUser
    void listTransactions_WithFilters_ReturnsFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                .param("type", "EXPENSE")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.summary.totalExpenses").exists());
    }
}
```

## References

- [Spring Data JPA Specifications](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications)
- [MapStruct Documentation](https://mapstruct.org/documentation/stable/reference/html/)
- [PostgreSQL Indexes](https://www.postgresql.org/docs/current/indexes.html)
- [REST API Design Best Practices](https://restfulapi.net/)
- [OWASP Data Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)

## Related ADRs

- [ADR-001: Authentication Architecture](./ADR-001-authentication-architecture.md) - JWT authentication used for API security

## Changelog

| Date | Author | Description |
|------|--------|-------------|
| 2026-01-23 | Architecture Team | Initial version |
