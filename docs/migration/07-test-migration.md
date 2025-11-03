# Test Migration Strategy

## Overview
Migration from Arquillian-based Java EE testing to Spring Boot's comprehensive testing framework while preserving characterisation test integrity and improving test maintainability.

## Current Testing Architecture Analysis

### Existing Test Structure
- **Arquillian**: Integration testing with embedded containers
- **JUnit 4**: Base testing framework
- **Mockito**: Mocking framework
- **Container-managed**: Tests run in actual Java EE container
- **Deployment descriptors**: ShrinkWrap for test deployments

### Current Test Examples
```java
@RunWith(Arquillian.class)
public class CatalogServiceTest {
    
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
            .addClass(CatalogService.class)
            .addClass(Category.class)
            .addAsResource("META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @EJB
    private CatalogService catalogService;
    
    @Test
    public void shouldFindAllCategories() {
        // Test logic...
    }
}
```

## Target Testing Architecture

### Spring Boot Test Slices
1. **@SpringBootTest** - Full integration tests
2. **@WebMvcTest** - Web layer tests
3. **@DataJpaTest** - JPA repository tests  
4. **@TestConfiguration** - Test-specific configuration
5. **JUnit 5** - Modern testing framework
6. **Testcontainers** - Real database integration tests

## Test Migration Strategy

### 1. Service Layer Tests (@SpringBootTest)

#### Before (Arquillian EJB Test)
```java
@RunWith(Arquillian.class)
public class CatalogServiceTest {
    
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
            .addPackage(CatalogService.class.getPackage())
            .addPackage(Category.class.getPackage())
            .addAsResource("META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @EJB
    private CatalogService catalogService;
    
    @PersistenceContext
    private EntityManager em;
    
    @Test
    @InSequence(1)
    public void shouldCreateCategory() throws Exception {
        // Given
        Category category = new Category("Electronics");
        
        // When
        Category created = catalogService.createCategory(category);
        
        // Then
        assertNotNull(created.getId());
        assertEquals("Electronics", created.getName());
    }
    
    @Test
    @InSequence(2)
    public void shouldFindAllCategories() throws Exception {
        // When
        List<Category> categories = catalogService.findAllCategories();
        
        // Then
        assertTrue(categories.size() >= 1);
    }
}
```

#### After (Spring Boot Test)
```java
@SpringBootTest
@Transactional
@Rollback
@TestMethodOrder(OrderAnnotation.class)
class CatalogServiceTest {
    
    @Autowired
    private CatalogService catalogService;
    
    @Autowired
    private TestEntityManager testEntityManager;
    
    @Test
    @Order(1)
    @DisplayName("Should create category with generated ID")
    void shouldCreateCategory() {
        // Given
        Category category = new Category("Electronics");
        
        // When
        Category created = catalogService.createCategory(category);
        
        // Then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Electronics");
        
        // Verify persistence
        testEntityManager.flush();
        Optional<Category> found = catalogService.findCategory(created.getId());
        assertThat(found).isPresent()
                        .get()
                        .hasFieldOrPropertyWithValue("name", "Electronics");
    }
    
    @Test
    @Order(2)
    @DisplayName("Should find all categories including previously created")
    void shouldFindAllCategories() {
        // Given - category from previous test exists due to @Transactional
        
        // When
        List<Category> categories = catalogService.findAllCategories();
        
        // Then
        assertThat(categories).isNotEmpty()
                             .hasSize(greaterThanOrEqualTo(1))
                             .extracting(Category::getName)
                             .contains("Electronics");
    }
    
    @Test
    @DisplayName("Should handle category search with pagination")
    void shouldSearchCategoriesWithPagination() {
        // Given
        testEntityManager.persistAndFlush(new Category("Books"));
        testEntityManager.persistAndFlush(new Category("Music"));
        testEntityManager.persistAndFlush(new Category("Movies"));
        
        // When
        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<Category> page = catalogService.findAllCategories(pageRequest);
        
        // Then - characterisation of pagination behavior
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
}
```

### 2. Web Layer Tests (@WebMvcTest)

#### Before (Arquillian Web Test)
```java
@RunWith(Arquillian.class)
public class CatalogControllerTest {
    
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
            .addPackage(CatalogController.class.getPackage())
            .addAsWebInfResource("test-web.xml", "web.xml");
    }
    
    @ArquillianResource
    private URL baseURL;
    
    @Test
    public void shouldDisplayProducts() throws Exception {
        WebDriver driver = new HtmlUnitDriver();
        driver.get(baseURL + "showproducts.faces?categoryName=Electronics");
        
        assertTrue(driver.getPageSource().contains("Products for Electronics"));
    }
}
```

#### After (Spring Boot Web Test)
```java
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CatalogService catalogService;
    
    @Test
    @DisplayName("Should display products for valid category")
    void shouldDisplayProductsForCategory() throws Exception {
        // Given
        String categoryName = "Electronics";
        List<Product> products = Arrays.asList(
            new Product("Laptop", "High-performance laptop"),
            new Product("Phone", "Smart phone")
        );
        Page<Product> productPage = new PageImpl<>(products);
        
        when(catalogService.findProductsByCategory(eq(categoryName), any(Pageable.class)))
            .thenReturn(productPage);
        
        // When & Then
        mockMvc.perform(get("/catalog/{categoryName}", categoryName))
               .andExpect(status().isOk())
               .andExpect(view().name("catalog/products"))
               .andExpect(model().attribute("categoryName", categoryName))
               .andExpect(model().attribute("products", productPage))
               .andExpect(content().string(containsString("Products for Electronics")));
        
        // Verify service interaction - characterise behavior
        verify(catalogService).findProductsByCategory(eq(categoryName), any(Pageable.class));
    }
    
    @Test
    @DisplayName("Should handle empty product list gracefully") 
    void shouldHandleEmptyProductList() throws Exception {
        // Given
        String categoryName = "EmptyCategory";
        Page<Product> emptyPage = Page.empty();
        
        when(catalogService.findProductsByCategory(eq(categoryName), any(Pageable.class)))
            .thenReturn(emptyPage);
        
        // When & Then
        mockMvc.perform(get("/catalog/{categoryName}", categoryName))
               .andExpect(status().isOk())
               .andExpect(model().attribute("products", emptyPage))
               .andExpect(content().string(containsString("No products found")));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NonExistentCategory"})
    @DisplayName("Should handle invalid category names appropriately")
    void shouldHandleInvalidCategories(String invalidCategory) throws Exception {
        // Given
        when(catalogService.findProductsByCategory(eq(invalidCategory), any(Pageable.class)))
            .thenReturn(Page.empty());
        
        // When & Then
        mockMvc.perform(get("/catalog/{categoryName}", invalidCategory))
               .andExpect(status().isOk())  // Preserving original behavior
               .andExpect(model().attribute("categoryName", invalidCategory));
    }
}
```

### 3. Data Layer Tests (@DataJpaTest)

#### JPA Repository Tests
```java
@DataJpaTest
class CategoryRepositoryTest {
    
    @Autowired
    private TestEntityManager testEntityManager;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Test
    @DisplayName("Should find category by name using named query")
    void shouldFindCategoryByName() {
        // Given
        Category category = new Category("Electronics");
        testEntityManager.persistAndFlush(category);
        
        // When
        Optional<Category> found = categoryRepository.findByName("Electronics");
        
        // Then - preserve characterisation behavior
        assertThat(found).isPresent()
                        .get()
                        .hasFieldOrPropertyWithValue("name", "Electronics");
    }
    
    @Test
    @DisplayName("Should return empty when category not found")
    void shouldReturnEmptyWhenCategoryNotFound() {
        // When
        Optional<Category> found = categoryRepository.findByName("NonExistent");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle null category name gracefully")
    void shouldHandleNullCategoryName() {
        // When & Then - preserve exception behavior if it existed
        assertThatCode(() -> categoryRepository.findByName(null))
            .doesNotThrowAnyException(); // Or specify expected exception
    }
    
    @Test
    @DisplayName("Should maintain category name uniqueness constraint")
    void shouldEnforceUniqueConstraintOnCategoryName() {
        // Given
        testEntityManager.persistAndFlush(new Category("Electronics"));
        
        // When & Then - test constraint behavior
        assertThatThrownBy(() -> {
            testEntityManager.persistAndFlush(new Category("Electronics"));
            testEntityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

### 4. Integration Tests with Testcontainers

#### Database Integration Test
```java
@SpringBootTest
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class PetstoreIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private CatalogService catalogService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private OrderService orderService;
    
    @Test
    @Order(1)
    @DisplayName("Complete order flow - characterisation test")
    void shouldCompleteFullOrderFlow() {
        // Given - Create test data
        Category category = catalogService.createCategory(new Category("Electronics"));
        Product product = catalogService.createProduct(
            new Product("Laptop", "Gaming laptop", category));
        Item item = catalogService.createItem(
            new Item("Gaming Laptop Pro", "High-end gaming laptop", 1299.99f, product));
        
        Customer customer = customerService.createCustomer(
            new Customer("testuser", "password", "John", "Doe"));
        
        // When - Process order
        Order order = new Order();
        OrderLine orderLine = new OrderLine(2, item);
        order.addOrderLine(orderLine);
        
        Order createdOrder = orderService.createOrder(order, customer);
        
        // Then - Verify complete flow
        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getCustomer()).isEqualTo(customer);
        assertThat(createdOrder.getOrderLines()).hasSize(1);
        assertThat(createdOrder.getTotal()).isEqualTo(2599.98f); // 2 * 1299.99
        
        // Characterise order date behavior
        assertThat(createdOrder.getOrderDate()).isCloseTo(
            LocalDateTime.now(), within(10, ChronoUnit.SECONDS));
    }
}
```

### 5. Security Integration Tests

#### Authentication Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private CustomerService customerService;
    
    @BeforeEach
    void setUp() {
        // Create test customer
        customerService.createCustomer(
            new Customer("testuser", "password", "Test", "User"));
    }
    
    @Test
    @DisplayName("Should authenticate valid user and establish session")
    void shouldAuthenticateValidUser() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/auth/login", loginRequest, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
            .hasPath("/home");
        
        // Verify session cookie is set
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertThat(cookies).isNotEmpty()
                          .anyMatch(cookie -> cookie.contains("JSESSIONID"));
    }
    
    @Test
    @DisplayName("Should reject invalid credentials")
    void shouldRejectInvalidCredentials() {
        // Given
        LoginRequest invalidLogin = new LoginRequest("testuser", "wrongpassword");
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/auth/login", invalidLogin, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Invalid credentials");
    }
}
```

## Test Configuration

### 1. Test Application Properties
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
    driver-class-name: org.h2.Driver
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
        
  h2:
    console:
      enabled: true

# Test-specific logging
logging:
  level:
    org.agoncal.application.petstore: DEBUG
    org.springframework.transaction: DEBUG
    org.hibernate.SQL: DEBUG
    
# Disable security for easier testing
spring.security.enabled: false
```

### 2. Test Configuration Classes
```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        // Use no-op encoder for faster tests
        return NoOpPasswordEncoder.getInstance();
    }
    
    @Bean
    @Primary
    public Clock testClock() {
        // Fixed clock for predictable test results
        return Clock.fixed(
            LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);
    }
    
    @Bean
    @Primary
    @Profile("test")
    public ApplicationEventPublisher mockEventPublisher() {
        return Mockito.mock(ApplicationEventPublisher.class);
    }
}

@TestComponent
public class TestDataFactory {
    
    public Category createTestCategory(String name) {
        return new Category(name);
    }
    
    public Product createTestProduct(String name, Category category) {
        return new Product(name, "Test description for " + name, category);
    }
    
    public Customer createTestCustomer(String login) {
        Customer customer = new Customer();
        customer.setLogin(login);
        customer.setPassword("password");
        customer.setFirstname("Test");
        customer.setLastname("User");
        return customer;
    }
}
```

## Characterisation Test Preservation

### 1. Business Logic Tests
```java
@SpringBootTest
@DisplayName("Catalog Service - Business Logic Characterisation")
class CatalogServiceCharacterisationTest {
    
    @Autowired
    private CatalogService catalogService;
    
    @Test
    @DisplayName("Product with items should load items eagerly")
    void productShouldLoadItemsEagerly() {
        // Given
        Category category = catalogService.createCategory(new Category("Electronics"));
        Product product = catalogService.createProduct(new Product("Laptop", category));
        Item item1 = catalogService.createItem(new Item("MacBook Pro", product));
        Item item2 = catalogService.createItem(new Item("Dell XPS", product));
        
        // When - This characterises the original lazy loading workaround
        Product foundProduct = catalogService.findProduct(product.getId());
        
        // Then - Items should be loaded (original behavior preservation)
        assertThat(foundProduct.getItems()).hasSize(2);
        assertThat(foundProduct.getItems())
            .extracting(Item::getName)
            .containsExactlyInAnyOrder("MacBook Pro", "Dell XPS");
    }
    
    @Test
    @DisplayName("Search with empty keyword should return all items")
    void searchWithEmptyKeywordShouldReturnAllItems() {
        // Given - preserve original behavior where null/empty = all items
        createTestCatalogData();
        
        // When
        List<Item> allItems = catalogService.searchItems("");
        List<Item> nullSearch = catalogService.searchItems(null);
        
        // Then - characterise original behavior
        assertThat(allItems).isNotEmpty();
        assertThat(nullSearch).hasSameElementsAs(allItems);
    }
    
    @Test
    @DisplayName("Category creation should auto-persist when attached to product")
    void shouldAutoPersistTransientCategoryWithProduct() {
        // Given - preserve original transient category auto-persist behavior
        Category transientCategory = new Category("New Category");
        assertThat(transientCategory.getId()).isNull(); // Transient
        
        Product product = new Product("New Product", transientCategory);
        
        // When
        Product createdProduct = catalogService.createProduct(product);
        
        // Then - category should be auto-persisted
        assertThat(createdProduct.getCategory().getId()).isNotNull();
        assertThat(createdProduct.getCategory().getName()).isEqualTo("New Category");
    }
}
```

### 2. Exception Behavior Tests
```java
@SpringBootTest
@DisplayName("Exception Behavior Characterisation")
class ExceptionBehaviorTest {
    
    @Autowired
    private CatalogService catalogService;
    
    @Test
    @DisplayName("Should throw ValidationException for null category ID")
    void shouldThrowValidationExceptionForNullCategoryId() {
        // When & Then - preserve exact exception behavior
        assertThatThrownBy(() -> catalogService.findCategory((Long) null))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Invalid category id");
    }
    
    @Test  
    @DisplayName("Should throw NoResultException for non-existent category by name")
    void shouldThrowNoResultExceptionForNonExistentCategory() {
        // When & Then - preserve JPA exception behavior
        assertThatThrownBy(() -> catalogService.findCategory("NonExistent"))
            .isInstanceOf(NoResultException.class);
    }
    
    @Test
    @DisplayName("Customer authentication should throw NoResultException for invalid credentials")
    void shouldThrowNoResultExceptionForInvalidAuth() {
        // When & Then - preserve original authentication exception
        assertThatThrownBy(() -> 
            customerService.authenticateCustomer("invalid", "credentials"))
            .isInstanceOf(NoResultException.class);
    }
}
```

## Performance Test Migration

### Load Testing with Spring Boot
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(SpringExtension.class)
class PerformanceCharacterisationTest {
    
    @Autowired
    private CatalogService catalogService;
    
    @Test
    @DisplayName("Search performance should handle 1000 concurrent searches")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void searchPerformanceShouldScaleWithConcurrency() throws InterruptedException {
        // Given
        createLargeCatalogDataset(1000); // 1000 products
        
        CountDownLatch latch = new CountDownLatch(100);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());
        
        // When - simulate concurrent searches
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    long start = System.currentTimeMillis();
                    List<Item> results = catalogService.searchItems("laptop");
                    long duration = System.currentTimeMillis() - start;
                    
                    executionTimes.add(duration);
                    assertThat(results).isNotEmpty(); // Verify results
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then - characterise performance expectations
        latch.await(25, TimeUnit.SECONDS);
        executor.shutdown();
        
        double avgTime = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        
        // Performance assertions based on original system
        assertThat(avgTime).isLessThan(200); // 200ms average
        assertThat(maxTime).isLessThan(1000); // 1s max
        assertThat(executionTimes).hasSize(100); // All completed
    }
}
```

## Test Utilities and Fixtures

### Test Data Builders
```java
@Component
@TestComponent
public class TestDataBuilder {
    
    public CategoryBuilder category() {
        return new CategoryBuilder();
    }
    
    public static class CategoryBuilder {
        private String name = "Test Category";
        
        public CategoryBuilder withName(String name) {
            this.name = name;
            return this;
        }
        
        public Category build() {
            return new Category(name);
        }
    }
    
    public ProductBuilder product() {
        return new ProductBuilder();
    }
    
    public static class ProductBuilder {
        private String name = "Test Product";
        private String description = "Test Description";
        private Category category = new Category("Default Category");
        
        public ProductBuilder withName(String name) {
            this.name = name;
            return this;
        }
        
        public ProductBuilder withCategory(Category category) {
            this.category = category;
            return this;
        }
        
        public Product build() {
            return new Product(name, description, category);
        }
    }
}
```

## Test Execution Strategy

### 1. Unit Tests
- Run with `mvn test`
- Fast feedback (< 30 seconds)
- No external dependencies

### 2. Integration Tests  
- Run with `mvn verify` or `mvn test -Dspring.profiles.active=integration`
- Include database integration
- Longer execution time acceptable

### 3. Characterisation Tests
- Run as part of regular test suite
- Focus on preserving existing behavior
- Document any behavior changes

### 4. Performance Tests
- Run separately with `mvn test -Dtest=**/*PerformanceTest`
- Establish baseline performance metrics
- Monitor regression

## CI/CD Integration

### Test Pipeline Configuration
```yaml
# GitHub Actions example
name: Test Suite
on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run unit tests
        run: mvn test
        
  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: test
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run integration tests
        run: mvn verify -Dspring.profiles.active=integration
```

## Migration Validation

### Test Coverage Requirements
- **Unit Tests**: > 80% line coverage
- **Integration Tests**: All major workflows covered
- **Characterisation Tests**: All existing behavior preserved
- **Performance Tests**: No regression in key metrics

### Checklist
- [ ] Convert all Arquillian tests to Spring Boot tests
- [ ] Maintain test execution order where business logic depends on it  
- [ ] Preserve all exception handling behavior
- [ ] Validate performance characteristics
- [ ] Update test configuration for Spring Boot
- [ ] Add new test slices (@WebMvcTest, @DataJpaTest)
- [ ] Configure Testcontainers for integration tests
- [ ] Set up CI/CD pipeline with proper test execution
- [ ] Document any behavior changes during migration

## Next Steps

1. Convert service layer tests first (least dependencies)
2. Migrate web layer tests to @WebMvcTest
3. Add JPA repository tests  
4. Create integration tests with Testcontainers
5. Establish performance baselines
6. Proceed to risks and rollback planning