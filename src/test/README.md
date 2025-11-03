# Test Execution Guide

## Overview

This directory contains characterization tests for the Java EE 6 Petstore application. These tests capture the current behavior of the application components before migration to Spring Boot 3, providing a safety net for refactoring and a clear specification of existing functionality.

## Test Categories

### EJB Service Layer Tests (`src/test/java/.../ejb/`)
- **CatalogServiceTest**: Tests for catalog management (categories, products, items)
- **CustomerServiceTest**: Tests for customer account management and authentication  
- **OrderServiceTest**: Tests for order processing and lifecycle management

**Purpose**: Capture business logic behavior, input validation patterns, and transactional requirements.

### JPA Repository Tests (`src/test/java/.../repository/`)
- **CustomerRepositoryTest**: Tests for Customer entity persistence and named queries
- Additional repository tests for catalog and order entities (to be implemented)

**Purpose**: Validate entity mappings, relationship behavior, and query execution patterns.

### JSF Navigation Contract Tests (`src/test/java/.../webcontracts/`)
- **SignonNavigationTest**: Tests for authentication flow navigation contracts
- Additional navigation tests for catalog and cart flows (to be implemented)

**Purpose**: Document navigation patterns and EL expression dependencies for Spring MVC migration.

### Test Utilities (`src/test/java/.../fixtures/`)
- **H2TestConfiguration**: H2 in-memory database setup for JPA tests
- **TestDataBuilder**: Fluent API for creating test data entities

**Purpose**: Provide consistent test infrastructure and data creation patterns.

## Prerequisites

### Required Dependencies
Add these dependencies to `pom.xml` with `<scope>test</scope>`:

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
```

### Java Version
- Tests require Java 8+ (compatible with existing codebase)
- No Java EE container dependencies required

## Running the Tests

### Command Line Execution
```bash
# Run all characterization tests
mvn test

# Run specific test category
mvn test -Dtest="**/ejb/**"           # EJB service tests only
mvn test -Dtest="**/repository/**"   # Repository tests only  
mvn test -Dtest="**/webcontracts/**" # Navigation contract tests only

# Run individual test class
mvn test -Dtest="CatalogServiceTest"
mvn test -Dtest="CustomerRepositoryTest"
```

### IDE Execution
- All tests can be run directly from IDE (IntelliJ IDEA, Eclipse)
- No special configuration required
- H2 database starts automatically for each test

### Expected Results
- **Compilation**: All tests should compile successfully against current codebase
- **Execution**: Tests run in isolation without external dependencies
- **Performance**: Unit tests < 100ms each, integration tests < 1s each
- **Deterministic**: Tests produce consistent results on every run

## Test Architecture

### Unit Tests (EJB Services)
```java
@ExtendWith(MockitoExtension.class) // Mockito integration
class ServiceTest {
    @Mock
    private EntityManager entityManager; // Mock JPA dependencies
    
    @InjectMocks 
    private ServiceClass serviceUnderTest; // Inject mocks
}
```

### Integration Tests (Repository Layer)
```java
class RepositoryTest {
    private EntityManager entityManager = H2TestConfiguration.createTestEntityManager();
    
    @Test
    void testEntityPersistence() {
        H2TestConfiguration.executeInTransaction(entityManager, em -> {
            // Test logic using real H2 database
        });
    }
}
```

### Contract Tests (Navigation Layer)
```java
class NavigationTest {
    @Test
    void testNavigationContract() throws IOException {
        Path xhtmlFile = Paths.get("src/main/webapp/page.xhtml");
        String content = Files.readString(xhtmlFile);
        // Parse and validate navigation contracts
    }
}
```

## Key Behaviors Captured

### Business Logic Patterns
- **Input Validation**: Null checking patterns and ValidationException usage
- **Entity Lifecycle**: Persist, merge, remove patterns with detached entity handling
- **Query Execution**: Named query usage and parameter binding
- **Transaction Boundaries**: EJB container-managed transaction behavior

### Data Access Patterns  
- **Entity Relationships**: OneToMany, ManyToOne, and Embedded mappings
- **Named Queries**: JPQL execution and result handling
- **Constraint Validation**: Unique constraints and NOT NULL enforcement
- **Connection Management**: EntityManager lifecycle in JTA environment

### Navigation Patterns
- **Implicit Navigation**: JSF outcome-to-view mapping
- **EL Expression Binding**: Backing bean property and method references
- **Session Management**: SessionScoped and ConversationScoped bean behavior
- **Form Processing**: Data binding and validation error handling

## Migration Preparation

These tests serve as:
1. **Behavioral Specification**: Clear documentation of current functionality
2. **Regression Safety Net**: Immediate feedback when migration breaks existing behavior
3. **Refactoring Confidence**: Safe transformation with comprehensive test coverage  
4. **Architecture Understanding**: Deep insight into current coupling and dependencies

### Spring Boot Migration Mapping
- **EJB Services** → Spring @Service components with @Transactional
- **JPA Entities** → Spring Data JPA repositories with method name queries
- **JSF Navigation** → Spring MVC controllers with Thymeleaf templates
- **CDI Scopes** → Spring scopes (@SessionScope, @RequestScope)

## Troubleshooting

### Common Issues

**Tests Don't Compile**
- Verify all test dependencies added to pom.xml
- Check Java version compatibility
- Ensure test classes in correct package structure

**H2 Database Issues**  
- Tests create isolated H2 instances - no configuration needed
- Check console output for SQL logging
- Verify entities scan correctly in test persistence unit

**Mock Configuration Issues**
- Ensure @ExtendWith(MockitoExtension.class) on test classes
- Verify @Mock and @InjectMocks annotations used correctly
- Check mock setup matches actual service dependencies

**File Path Issues (Windows)**
- Navigation tests use relative paths from project root
- Verify XHTML files exist in src/main/webapp
- Check file encoding (UTF-8) for template parsing

### Debug Information
```bash
# Enable SQL logging for repository tests
mvn test -Dtest="**/repository/**" -Dhibernate.show_sql=true

# Enable Mockito verbose logging
mvn test -Dtest="**/ejb/**" -Dmockito.verbose=true
```

## Extending the Test Suite

### Adding New EJB Service Tests
1. Create test class in `src/test/java/.../ejb/`
2. Use `@ExtendWith(MockitoExtension.class)`
3. Mock `EntityManager` and related JPA queries
4. Follow existing patterns in `CatalogServiceTest`

### Adding New Repository Tests  
1. Create test class in `src/test/java/.../repository/`
2. Use `H2TestConfiguration` for database setup
3. Use `TestDataBuilder` for consistent test data
4. Follow existing patterns in `CustomerRepositoryTest`

### Adding New Navigation Tests
1. Create test class in `src/test/java/.../webcontracts/`
2. Parse XHTML files using `Files.readString()`
3. Extract EL expressions and action mappings
4. Document navigation contracts for Spring MVC migration

This test suite provides comprehensive characterization of the existing application behavior, ensuring a safe and confident migration to Spring Boot 3.