# EJB to Spring Services Migration

## Overview
Migration from Java EE Stateless Session Beans to Spring's service layer architecture with dependency injection and declarative transaction management.

## Current EJB Architecture Analysis

### Key EJB Services
1. **CatalogService** - Product catalog management
2. **CustomerService** - Customer account operations
3. **OrderService** - Order processing
4. **DBPopulator** - Sample data initialization

### EJB Characteristics
- `@Stateless` lifecycle management
- Container-managed transactions
- `@Inject` dependency injection
- `@TransactionAttribute` annotations
- Container-managed EntityManager

## Spring Services Target Architecture

### Annotation Mapping Table

| Java EE EJB | Spring Boot Equivalent | Purpose |
|-------------|----------------------|---------|
| `@Stateless` | `@Service` | Business service component |
| `@Inject` | `@Autowired` / Constructor Injection | Dependency injection |
| `@EJB` | `@Autowired` | Service injection |
| `@Resource` | `@Value` / `@Autowired` | Resource injection |
| `@TransactionAttribute` | `@Transactional` | Transaction boundaries |
| Container-managed EM | Spring-managed EM | Persistence context |

## Migration Examples

### 1. CatalogService Migration

#### Before (Java EE EJB)
```java
@Stateless
@Loggable
public class CatalogService implements Serializable {

    @Inject
    private EntityManager em;

    public Category findCategory(Long categoryId) {
        if (categoryId == null)
            throw new ValidationException("Invalid category id");
        return em.find(Category.class, categoryId);
    }

    public Category createCategory(Category category) {
        if (category == null)
            throw new ValidationException("Category object is null");
        em.persist(category);
        return category;
    }
}
```

#### After (Spring Service)
```java
@Service
@Transactional(readOnly = true)  // Default read-only for all methods
@Slf4j
public class CatalogService {

    private final EntityManager entityManager;

    // Constructor injection (recommended over field injection)
    public CatalogService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Category> findCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ValidationException("Invalid category id");
        }
        return Optional.ofNullable(entityManager.find(Category.class, categoryId));
    }

    @Transactional  // Override read-only for write operations
    public Category createCategory(Category category) {
        if (category == null) {
            throw new ValidationException("Category object is null");
        }
        entityManager.persist(category);
        return category;
    }

    // Additional Spring-style methods
    public List<Category> findAllCategories() {
        return entityManager.createNamedQuery(Category.FIND_ALL, Category.class)
                           .getResultList();
    }

    public List<Product> findProductsByCategory(String categoryName) {
        return entityManager.createNamedQuery(Product.FIND_BY_CATEGORY_NAME, Product.class)
                           .setParameter("pname", categoryName)
                           .getResultList();
    }
}
```

### 2. CustomerService Migration

#### Before (Java EE EJB)
```java
@Stateless
@Loggable
public class CustomerService implements Serializable {

    @Inject
    private EntityManager em;

    public boolean doesLoginAlreadyExist(final String login) {
        if (login == null)
            throw new ValidationException("Login cannot be null");

        TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class);
        typedQuery.setParameter("login", login);
        try {
            typedQuery.getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    public Customer findCustomer(final String login, final String password) {
        // ... validation logic
        TypedQuery<Customer> typedQuery = em.createNamedQuery(Customer.FIND_BY_LOGIN_PASSWORD, Customer.class);
        typedQuery.setParameter("login", login);
        typedQuery.setParameter("password", password);
        return typedQuery.getSingleResult();
    }
}
```

#### After (Spring Service)
```java
@Service
@Transactional(readOnly = true)
@Slf4j
public class CustomerService {

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder; // Spring Security integration

    public CustomerService(EntityManager entityManager, PasswordEncoder passwordEncoder) {
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean doesLoginAlreadyExist(String login) {
        if (login == null) {
            throw new ValidationException("Login cannot be null");
        }

        return entityManager.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class)
                           .setParameter("login", login)
                           .getResultStream()
                           .findFirst()
                           .isPresent();
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customer == null) {
            throw new ValidationException("Customer object is null");
        }
        
        // Hash password before persisting (security improvement)
        if (customer.getPassword() != null) {
            customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        }
        
        entityManager.persist(customer);
        return customer;
    }

    public Optional<Customer> authenticateCustomer(String login, String password) {
        if (login == null || password == null) {
            throw new ValidationException("Login and password cannot be null");
        }

        Optional<Customer> customer = entityManager
            .createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class)
            .setParameter("login", login)
            .getResultStream()
            .findFirst();

        return customer.filter(c -> passwordEncoder.matches(password, c.getPassword()));
    }
}
```

### 3. OrderService Migration

#### Before (Java EE EJB)
```java
@Stateless
@Loggable
public class OrderService implements Serializable {

    @Inject
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Order createOrder(Order order, Customer customer) {
        // ... order processing logic
        em.persist(order);
        return order;
    }
}
```

#### After (Spring Service)
```java
@Service
@Transactional(readOnly = true)
@Slf4j
public class OrderService {

    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher; // Spring Events

    public OrderService(EntityManager entityManager, ApplicationEventPublisher eventPublisher) {
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        timeout = 30,
        rollbackFor = {OrderProcessingException.class}
    )
    public Order createOrder(Order order, Customer customer) {
        if (order == null || customer == null) {
            throw new ValidationException("Order and customer cannot be null");
        }

        // Validate inventory availability
        validateOrderItems(order);
        
        // Process the order
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        entityManager.persist(order);

        // Publish domain event (Spring feature)
        eventPublisher.publishEvent(new OrderCreatedEvent(order));

        log.info("Order {} created successfully for customer {}", order.getId(), customer.getLogin());
        return order;
    }

    private void validateOrderItems(Order order) {
        // Business validation logic
        order.getOrderLines().forEach(this::validateOrderLine);
    }
}
```

## Transaction Management Migration

### Container-Managed vs Spring Transactions

#### EJB Transaction Attributes Mapping
| EJB Transaction Attribute | Spring @Transactional Equivalent |
|---------------------------|-----------------------------------|
| `REQUIRED` (default) | `@Transactional` (default) |
| `REQUIRES_NEW` | `@Transactional(propagation = REQUIRES_NEW)` |
| `MANDATORY` | `@Transactional(propagation = MANDATORY)` |
| `NOT_SUPPORTED` | `@Transactional(propagation = NOT_SUPPORTED)` |
| `NEVER` | `@Transactional(propagation = NEVER)` |
| `SUPPORTS` | `@Transactional(propagation = SUPPORTS)` |

#### Advanced Transaction Configuration
```java
@Service
public class OrderService {

    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        timeout = 30,
        rollbackFor = {BusinessException.class, DataAccessException.class},
        noRollbackFor = {ValidationException.class}
    )
    public Order processOrder(OrderRequest request) {
        // Complex order processing
    }
}
```

## Dependency Injection Migration

### From CDI to Spring DI

#### Field Injection (Not Recommended)
```java
@Service
public class CatalogService {
    @Autowired
    private EntityManager entityManager; // Avoid - hard to test
}
```

#### Constructor Injection (Recommended)
```java
@Service
public class CatalogService {
    private final EntityManager entityManager;
    
    public CatalogService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
```

#### Multiple Dependencies
```java
@Service
public class OrderService {
    private final EntityManager entityManager;
    private final CatalogService catalogService;
    private final CustomerService customerService;
    private final ApplicationEventPublisher eventPublisher;
    
    public OrderService(
        EntityManager entityManager,
        CatalogService catalogService, 
        CustomerService customerService,
        ApplicationEventPublisher eventPublisher) {
        
        this.entityManager = entityManager;
        this.catalogService = catalogService;
        this.customerService = customerService;
        this.eventPublisher = eventPublisher;
    }
}
```

## Service Layer Enhancements

### 1. Repository Layer Introduction
```java
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    @Query("SELECT c FROM Category c WHERE c.description LIKE %:keyword%")
    List<Category> findByDescriptionContaining(@Param("keyword") String keyword);
}

@Service
@Transactional(readOnly = true)
public class CatalogService {
    
    private final CategoryRepository categoryRepository;
    
    public CatalogService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }
    
    public Optional<Category> findCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }
}
```

### 2. Event-Driven Architecture
```java
// Domain Event
public class OrderCreatedEvent {
    private final Order order;
    
    public OrderCreatedEvent(Order order) {
        this.order = order;
    }
    
    public Order getOrder() { return order; }
}

// Event Publisher (in Service)
@Service
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Order createOrder(Order order) {
        // ... persist order
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
        return order;
    }
}

// Event Listener
@Component
public class OrderEventHandler {
    
    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Send email notification
        // Update inventory
        // Log metrics
    }
}
```

### 3. Validation Integration
```java
@Service
@Validated
public class CustomerService {
    
    @Transactional
    public Customer createCustomer(@Valid Customer customer) {
        // JSR-303 validation happens automatically
        return entityManager.persist(customer);
    }
    
    public Customer updateCustomer(
        @NotNull @Min(1) Long customerId, 
        @Valid CustomerUpdateRequest request) {
        // Method-level validation
    }
}
```

## Interceptor Migration to AOP

### EJB Interceptors to Spring AOP

#### Before (EJB Interceptor)
```java
@Interceptor
@Loggable
public class LoggingInterceptor {
    
    @AroundInvoke
    public Object logMethodCall(InvocationContext context) throws Exception {
        // Logging logic
        return context.proceed();
    }
}
```

#### After (Spring AOP)
```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    
    @Around("@annotation(Loggable) || @within(Loggable)")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Starting method: {}", methodName);
            Object result = joinPoint.proceed();
            log.debug("Method {} completed in {} ms", methodName, 
                     System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Method {} failed after {} ms", methodName, 
                     System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }
}
```

## Testing Migration

### EJB Testing to Spring Boot Testing

#### Before (Arquillian)
```java
@RunWith(Arquillian.class)
public class CatalogServiceTest {
    
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                        .addClass(CatalogService.class);
    }
    
    @EJB
    private CatalogService catalogService;
}
```

#### After (Spring Boot Test)
```java
@SpringBootTest
@Transactional
@Rollback
class CatalogServiceTest {
    
    @Autowired
    private CatalogService catalogService;
    
    @MockBean
    private CategoryRepository categoryRepository;
    
    @Test
    void shouldFindCategoryById() {
        // Given
        Category category = new Category("Electronics");
        when(categoryRepository.findById(1L))
            .thenReturn(Optional.of(category));
        
        // When
        Optional<Category> result = catalogService.findCategory(1L);
        
        // Then
        assertThat(result).isPresent()
                         .get()
                         .hasFieldOrPropertyWithValue("name", "Electronics");
    }
}
```

## Complete Service Migration Checklist

### For Each EJB Service:

#### Code Changes
- [ ] Replace `@Stateless` with `@Service`
- [ ] Replace `@Inject` with constructor injection
- [ ] Add `@Transactional` annotations with appropriate propagation
- [ ] Update exception handling patterns
- [ ] Add logging with SLF4J
- [ ] Implement repository pattern where beneficial

#### Testing Changes
- [ ] Replace Arquillian with Spring Boot Test
- [ ] Update test configuration
- [ ] Add integration tests with `@SpringBootTest`
- [ ] Add unit tests with `@ExtendWith(MockitoExtension.class)`

#### Configuration Updates
- [ ] Remove `beans.xml` entries
- [ ] Ensure component scanning covers service packages
- [ ] Configure transaction management
- [ ] Set up AOP for cross-cutting concerns

## Migration Timeline

1. **Phase 1**: Basic EJB → Spring Service conversion
2. **Phase 2**: Add repository layer abstraction
3. **Phase 3**: Implement event-driven features
4. **Phase 4**: Add comprehensive testing
5. **Phase 5**: Performance optimization and monitoring

## Potential Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Transaction propagation differences | EJB vs Spring TX semantics | Test thoroughly, adjust propagation |
| EntityManager lifecycle | Container vs Spring managed | Use `@PersistenceContext` correctly |
| Lazy loading issues | Session management differences | Configure Open-in-View or eager loading |
| Circular dependencies | Constructor injection | Use `@Lazy` or refactor design |

## Next Steps

1. Migrate CatalogService first (least dependencies)
2. Update CustomerService with security improvements
3. Migrate OrderService with event publishing
4. Add repository layer gradually
5. Proceed to JSF → Spring MVC migration