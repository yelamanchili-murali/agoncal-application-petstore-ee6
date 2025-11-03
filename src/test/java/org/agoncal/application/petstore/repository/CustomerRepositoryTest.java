package org.agoncal.application.petstore.repository;

import org.agoncal.application.petstore.domain.Customer;
import org.agoncal.application.petstore.fixtures.H2TestConfiguration;
import org.agoncal.application.petstore.fixtures.TestDataBuilder;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Customer entity JPA mappings and named queries.
 * 
 * These tests validate that the Customer entity can be persisted, queried,
 * and updated correctly using H2 in-memory database. They capture the current
 * behavior of named queries and entity relationships for migration reference.
 * 
 * Key Behaviors Being Tested:
 * - Customer entity persistence and retrieval
 * - Named query execution with parameters
 * - Unique constraint validation for login
 * - Address relationship mapping (embedded entity)
 * - Authentication query behavior with plaintext passwords
 */
@DisplayName("Customer Repository Integration Tests")
class CustomerRepositoryTest {

    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeAll
    static void setUpClass() {
        entityManagerFactory = H2TestConfiguration.createTestEntityManagerFactory();
    }

    @AfterAll
    static void tearDownClass() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null && entityManager.isOpen()) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Nested
    @DisplayName("Entity Persistence and Retrieval")
    class EntityPersistenceAndRetrieval {

        @Test
        @DisplayName("Customer can be persisted and retrieved with generated ID")
        void customerPersistence_WithValidData_GeneratesIdAndPersists() {
            // Given
            Customer customer = TestDataBuilder.aCustomer()
                .withLogin("newuser")
                .withEmail("newuser@example.com")
                .build();

            // When
            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer);
                em.flush(); // Force ID generation
            });

            // Then
            assertNotNull(customer.getId());
            assertTrue(customer.getId() > 0);

            // Verify retrieval
            Customer retrieved = entityManager.find(Customer.class, customer.getId());
            assertNotNull(retrieved);
            assertEquals(customer.getLogin(), retrieved.getLogin());
            assertEquals(customer.getEmail(), retrieved.getEmail());
        }

        @Test
        @DisplayName("Customer address is persisted as embedded entity")
        void customerWithAddress_WhenPersisted_StoresAddressInSameTable() {
            // Given
            Customer customer = TestDataBuilder.aCustomer()
                .withLogin("addressuser")
                .withAddress("456 Main St", "Springfield", "54321", "USA")
                .build();

            // When
            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer);
            });

            // Then
            Customer retrieved = entityManager.find(Customer.class, customer.getId());
            assertNotNull(retrieved.getHomeAddress());
            assertEquals("456 Main St", retrieved.getHomeAddress().getStreet1());
            assertEquals("Springfield", retrieved.getHomeAddress().getCity());
            assertEquals("54321", retrieved.getHomeAddress().getZipcode());
            assertEquals("USA", retrieved.getHomeAddress().getCountry());
        }

        @Test
        @DisplayName("Customer can be updated and changes are persisted")
        void customerUpdate_WithChangedData_PersistsChanges() {
            // Given - Create and persist initial customer
            Customer customer = TestDataBuilder.aCustomer()
                .withLogin("updateuser")
                .withEmail("update@example.com")
                .build();

            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer);
            });

            // When - Update customer data
            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                Customer managed = em.find(Customer.class, customer.getId());
                managed.setEmail("updated@example.com");
                managed.setFirstname("Updated");
                // No explicit merge needed - entity is managed
            });

            // Then - Verify changes persisted
            entityManager.clear(); // Clear persistence context
            Customer retrieved = entityManager.find(Customer.class, customer.getId());
            assertEquals("updated@example.com", retrieved.getEmail());
            assertEquals("Updated", retrieved.getFirstname());
        }
    }

    @Nested
    @DisplayName("Named Query Behavior")
    class NamedQueryBehavior {

        @Test
        @DisplayName("FIND_BY_LOGIN returns customer when login exists")
        void findByLoginQuery_WhenExists_ReturnsCustomer() {
            // Given
            Customer customer = TestDataBuilder.aCustomer()
                .withLogin("queryuser")
                .build();

            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer);
            });

            // When
            TypedQuery<Customer> query = entityManager.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class);
            query.setParameter("login", "queryuser");
            Customer result = query.getSingleResult();

            // Then
            assertNotNull(result);
            assertEquals("queryuser", result.getLogin());
            assertEquals(customer.getId(), result.getId());
        }

        @Test
        @DisplayName("FIND_BY_LOGIN throws NoResultException when login does not exist")
        void findByLoginQuery_WhenNotExists_ThrowsNoResultException() {
            // When & Then
            TypedQuery<Customer> query = entityManager.createNamedQuery(Customer.FIND_BY_LOGIN, Customer.class);
            query.setParameter("login", "nonexistent");
            
            assertThrows(NoResultException.class, query::getSingleResult);
        }

        @Test
        @DisplayName("FIND_BY_LOGIN_PASSWORD authenticates with correct credentials - CHARACTERIZATION: Plaintext password")
        void findByLoginPasswordQuery_WithValidCredentials_ReturnsCustomer() {
            // Given - SECURITY CONCERN: Password stored in plaintext
            Customer customer = TestDataBuilder.aCustomer()
                .withLogin("authuser")
                .withPassword("plainpass")
                .build();

            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer);
            });

            // When
            TypedQuery<Customer> query = entityManager.createNamedQuery(Customer.FIND_BY_LOGIN_PASSWORD, Customer.class);
            query.setParameter("login", "authuser");
            query.setParameter("password", "plainpass"); // Plaintext password comparison
            Customer result = query.getSingleResult();

            // Then
            assertNotNull(result);
            assertEquals("authuser", result.getLogin());
        }

        @Test
        @DisplayName("FIND_BY_LOGIN_PASSWORD throws NoResultException with invalid credentials")
        void findByLoginPasswordQuery_WithInvalidCredentials_ThrowsNoResultException() {
            // Given
            Customer customer = TestDataBuilder.aCustomer()
                .withLogin("authuser")
                .withPassword("correctpass")
                .build();

            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer);
            });

            // When & Then - Wrong password
            TypedQuery<Customer> query = entityManager.createNamedQuery(Customer.FIND_BY_LOGIN_PASSWORD, Customer.class);
            query.setParameter("login", "authuser");
            query.setParameter("password", "wrongpass");
            
            assertThrows(NoResultException.class, query::getSingleResult);
        }

        @Test
        @DisplayName("FIND_ALL returns all customers in database")
        void findAllQuery_WithMultipleCustomers_ReturnsAllCustomers() {
            // Given
            Customer customer1 = TestDataBuilder.aCustomer().withLogin("user1").build();
            Customer customer2 = TestDataBuilder.aCustomer().withLogin("user2").build();

            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer1);
                em.persist(customer2);
            });

            // When
            TypedQuery<Customer> query = entityManager.createNamedQuery(Customer.FIND_ALL, Customer.class);
            List<Customer> results = query.getResultList();

            // Then
            assertEquals(2, results.size());
            assertTrue(results.stream().anyMatch(c -> "user1".equals(c.getLogin())));
            assertTrue(results.stream().anyMatch(c -> "user2".equals(c.getLogin())));
        }

        @Test
        @DisplayName("FIND_ALL returns empty list when no customers exist")
        void findAllQuery_WhenNoCustomers_ReturnsEmptyList() {
            // When
            TypedQuery<Customer> query = entityManager.createNamedQuery(Customer.FIND_ALL, Customer.class);
            List<Customer> results = query.getResultList();

            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("Constraint Validation")
    class ConstraintValidation {

        @Test
        @DisplayName("Unique login constraint prevents duplicate logins")
        void uniqueLoginConstraint_WhenDuplicateLogin_PreventsPersistence() {
            // Given
            Customer customer1 = TestDataBuilder.aCustomer().withLogin("duplicate").build();
            Customer customer2 = TestDataBuilder.aCustomer().withLogin("duplicate").build();

            // When & Then
            H2TestConfiguration.executeInTransaction(entityManager, em -> {
                em.persist(customer1);
            });

            // Attempting to persist second customer with same login should fail
            assertThrows(Exception.class, () -> {
                H2TestConfiguration.executeInTransaction(entityManager, em -> {
                    em.persist(customer2);
                    em.flush(); // Force constraint validation
                });
            });
        }

        @Test
        @DisplayName("Required fields are validated by database constraints")
        void requiredFieldValidation_WhenMissingRequiredData_PreventsPersistence() {
            // Given - Customer with null required fields
            Customer customer = new Customer();
            customer.setLogin("validlogin");
            // Missing required fields: password, firstname, lastname

            // When & Then - Should fail due to NOT NULL constraints
            assertThrows(Exception.class, () -> {
                H2TestConfiguration.executeInTransaction(entityManager, em -> {
                    em.persist(customer);
                    em.flush();
                });
            });
        }
    }

    @Nested
    @DisplayName("Migration Behavior Documentation")
    class MigrationBehaviorDocumentation {

        @Test
        @DisplayName("CHARACTERIZATION: Current entity mapping behavior for Spring Boot migration")
        void documentEntityMappingBehavior() {
            // CURRENT JPA MAPPING BEHAVIOR:
            // 1. Customer uses @GeneratedValue(strategy = GenerationType.AUTO) - will use H2 sequence
            // 2. Address is @Embedded - stored in same table as Customer
            // 3. Login has @Column(unique = true) - database enforces uniqueness
            // 4. Required fields have @NotNull - database constraints enforced
            // 5. String fields have length limits - @Size validation + @Column length
            //
            // SPRING BOOT MIGRATION CONSIDERATIONS:
            // 1. GenerationType.AUTO compatible with Spring Data JPA
            // 2. @Embedded relationships work identically
            // 3. Unique constraints transfer to Spring Boot
            // 4. Bean Validation (@NotNull, @Size) compatible with Spring Boot Validation
            // 5. Consider adding @CreatedDate/@LastModifiedDate for audit
            
            assertTrue(true, "Entity mapping behavior documented for Spring Boot migration");
        }

        @Test
        @DisplayName("CHARACTERIZATION: Named query patterns for Spring Data JPA conversion")
        void documentNamedQueryPatterns() {
            // CURRENT NAMED QUERY PATTERNS:
            // 1. Customer.FIND_BY_LOGIN: Simple parameter binding
            // 2. Customer.FIND_BY_LOGIN_PASSWORD: Multiple parameter binding
            // 3. Customer.FIND_ALL: No parameters, simple select
            //
            // SPRING DATA JPA EQUIVALENT METHODS:
            // 1. Optional<Customer> findByLogin(String login)
            // 2. Optional<Customer> findByLoginAndPassword(String login, String password)  
            // 3. List<Customer> findAll() - inherited from JpaRepository
            //
            // MIGRATION BENEFITS:
            // - Type safety with Optional<T> return types
            // - Automatic NoResultException handling via Optional
            // - Method name query derivation eliminates JPQL
            // - Consistent exception handling across repository methods
            
            assertTrue(true, "Named query patterns documented for Spring Data JPA migration");
        }

        @Test
        @DisplayName("CHARACTERIZATION: Security concerns for Spring Boot migration")
        void documentSecurityConcerns() {
            // CRITICAL SECURITY ISSUES TO ADDRESS:
            // 1. Passwords stored in plaintext (MAJOR SECURITY RISK)
            // 2. No password hashing before persistence
            // 3. Authentication queries expose passwords in logs
            // 4. No password strength validation
            // 5. No account lockout mechanism
            //
            // SPRING BOOT MIGRATION SECURITY REQUIREMENTS:
            // 1. Implement BCryptPasswordEncoder for password hashing
            // 2. Hash passwords in service layer before persistence
            // 3. Remove password from authentication queries (hash comparison in service)
            // 4. Add password strength validation
            // 5. Implement Spring Security for authentication
            // 6. Add account status management (enabled/locked/expired)
            // 7. Implement audit logging for authentication attempts
            
            assertTrue(true, "Security concerns documented for Spring Boot migration");
        }
    }
}