# Characterization Test Tree Proposal

## Overview

This document outlines the proposed structure for characterization tests that capture the current behavior of the Java EE 6 Petstore application before migration to Spring Boot 3. Tests are organized by architectural layer to ensure comprehensive coverage of existing functionality.

## Test Package Structure

```
src/test/java/org/agoncal/application/petstore/
├── ejb/                              # EJB Service Layer Tests
│   ├── CatalogServiceTest.java       # Catalog management operations
│   ├── CustomerServiceTest.java      # Customer account and authentication
│   └── OrderServiceTest.java         # Order processing and lifecycle
├── repository/                       # JPA Repository Integration Tests  
│   ├── CustomerRepositoryTest.java   # Customer entity persistence
│   ├── CatalogRepositoryTest.java    # Category/Product/Item persistence
│   ├── OrderRepositoryTest.java      # Order entity persistence
│   └── NamedQueriesTest.java         # All named query validations
├── webcontracts/                     # JSF Navigation Contract Tests
│   ├── SignonNavigationTest.java     # Login/registration flow
│   ├── CatalogNavigationTest.java    # Product browsing flow
│   ├── CartNavigationTest.java       # Shopping cart flow
│   ├── OrderNavigationTest.java      # Order confirmation flow
│   └── AccountNavigationTest.java    # Account management flow
└── fixtures/                         # Test Data Builders and Utilities
    ├── TestDataBuilder.java          # Fluent test data creation
    ├── H2TestConfiguration.java      # H2 database setup
    └── MockitoExtension.java         # Mockito JUnit 5 integration
```

## Test Categories and Scope

### 1. EJB Service Layer Tests (Unit Tests)

**Target Classes:**
- `CatalogService` (@Stateless)
- `CustomerService` (@Stateless) 
- `OrderService` (@Stateless)

**Test Approach:**
- Mock `EntityManager` using Mockito
- Test all public methods with positive/negative/edge cases
- Document transactional expectations (method-level comments)
- Validate business invariants and exception handling
- Verify interaction patterns with collaborators

**Key Behaviors to Capture:**
- Input validation patterns (null checks, business rules)
- Entity lifecycle management (persist, merge, remove)
- Query execution patterns and parameter binding
- Exception handling and propagation
- Cross-service dependencies and calls

### 2. Repository Integration Tests (JPA Layer)

**Target Entities:**
- `Customer` (authentication, unique constraints)
- `Category`, `Product`, `Item` (catalog hierarchy)
- `Order`, `OrderLine` (order processing)
- All named queries defined in entities

**Test Approach:**
- Use H2 in-memory database with JPA configuration
- Bootstrap minimal persistence context (no full EE container)
- Validate entity mappings and relationships
- Test named queries with realistic data scenarios
- Verify constraint validations and cascade behaviors

**Key Behaviors to Capture:**
- Entity relationship mappings (OneToMany, ManyToOne)
- Named query result sets and parameter binding
- Unique constraints and validation rules
- Cascade operations and orphan removal
- Database schema generation from annotations

### 3. Web Navigation Contract Tests (JSF Layer)

**Target Views:**
- `signon.xhtml` (login/registration)
- `main.xhtml` (home/search)
- `showproducts.xhtml`, `showitems.xhtml` (catalog browsing)
- `showcart.xhtml`, `confirmorder.xhtml` (purchase flow)
- `showaccount.xhtml`, `updateaccount.xhtml` (account management)

**Test Approach:**
- Parse `faces-config.xml` for explicit navigation rules
- Extract EL expressions from XHTML templates
- Create contract assertions (no JSF container required)
- Document expected navigation outcomes and model dependencies
- Validate backing bean method references

**Key Behaviors to Capture:**
- Navigation rule mappings (view-id → outcome → destination)
- EL expression patterns and backing bean coupling
- Required request parameters and model attributes
- Form submission targets and validation requirements
- Template inheritance and component structure

## Test Dependencies and Configuration

### Required Dependencies (Test Scope)

```xml
<!-- Core Testing -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.9.2</version>
    <scope>test</scope>
</dependency>

<!-- Mocking Framework -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>

<!-- JPA Testing -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.1.214</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-entitymanager</artifactId>
    <version>5.6.15.Final</version>
    <scope>test</scope>
</dependency>

<!-- Bean Validation -->
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>6.2.5.Final</version>
    <scope>test</scope>
</dependency>

<!-- XML Parsing for JSF Contracts -->
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <version>2.3.1</version>
    <scope>test</scope>
</dependency>
```

### H2 Test Configuration

Create test-specific `persistence.xml` that overrides production settings:
- H2 in-memory database URL
- Hibernate with create-drop schema generation  
- SQL logging enabled for test debugging
- No JNDI datasource dependency

## Success Criteria

### Compilation Requirements
- All tests compile successfully against current codebase
- No runtime container dependencies (GlassFish, WildFly, etc.)
- Tests run in isolation using Maven/Gradle test lifecycle

### Test Quality Requirements
- **Deterministic**: No time-based or random dependencies
- **Fast**: Unit tests < 100ms, integration tests < 1s each
- **Isolated**: Each test manages its own data and mocks
- **Expressive**: Test names and assertions clearly document expected behavior

### Documentation Requirements
- Each test class has header comment explaining what behavior is captured
- Complex business rules documented with inline comments
- Navigation contracts captured in markdown tables
- EL expression inventory documented per view

## Migration Preparation Benefits

These characterization tests will provide:

1. **Behavior Documentation**: Clear specification of current functionality
2. **Regression Detection**: Immediate feedback when migration breaks existing features  
3. **Refactoring Confidence**: Safe transformation with test coverage
4. **Architecture Understanding**: Deep insight into current coupling and dependencies
5. **Spring Boot Mapping**: Clear requirements for equivalent Spring Boot implementations

## Test Execution Strategy

### Phase 1: Foundation (Compilation)
- Establish test infrastructure and build configuration
- Create test data builders and H2 setup utilities
- Validate that test compilation succeeds

### Phase 2: Service Layer Coverage  
- Implement all EJB service characterization tests
- Document transactional boundaries and business invariants
- Establish mocking patterns for EntityManager

### Phase 3: Persistence Layer Coverage
- Implement JPA entity and named query tests
- Validate relationship mappings and constraints
- Establish H2 test database patterns

### Phase 4: Navigation Contracts
- Parse JSF configuration and XHTML templates
- Document navigation flows and EL dependencies  
- Create contract assertion framework

This structured approach ensures comprehensive coverage of existing behavior while maintaining focus on the eventual Spring Boot 3 migration goal.