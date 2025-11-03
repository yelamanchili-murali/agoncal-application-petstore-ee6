package org.agoncal.application.petstore.fixtures;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.Map;

/**
 * H2 in-memory database configuration for JPA integration tests.
 * 
 * This configuration provides a clean, isolated H2 database instance for each test
 * that mimics the production JPA setup while avoiding external dependencies.
 * 
 * Key Features:
 * - H2 in-memory database with create-drop schema generation
 * - Hibernate as JPA provider with logging enabled  
 * - No JNDI dependencies (uses resource-local transactions)
 * - SQL logging enabled for debugging test queries
 */
public class H2TestConfiguration {

    private static final String TEST_PERSISTENCE_UNIT = "testPetstorePU";
    
    /**
     * Creates a test EntityManagerFactory configured for H2 in-memory database.
     * 
     * This factory overrides production settings to use:
     * - H2 database URL with unique name per test run
     * - create-drop schema generation for clean test state
     * - SQL logging for debugging
     * - Resource-local transactions (no JTA required)
     * 
     * @return EntityManagerFactory configured for testing
     */
    public static EntityManagerFactory createTestEntityManagerFactory() {
        Map<String, String> properties = new HashMap<>();
        
        // H2 Database Configuration
        properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("javax.persistence.jdbc.url", 
            "jdbc:h2:mem:testdb_" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        properties.put("javax.persistence.jdbc.user", "sa");
        properties.put("javax.persistence.jdbc.password", "");
        
        // Hibernate Configuration
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.use_sql_comments", "true");
        
        // JPA Configuration  
        properties.put("javax.persistence.transactionType", "RESOURCE_LOCAL");
        
        return Persistence.createEntityManagerFactory("applicationPetstorePU", properties);
    }
    
    /**
     * Creates a test EntityManager from the test EntityManagerFactory.
     * 
     * @return EntityManager ready for testing
     */
    public static EntityManager createTestEntityManager() {
        EntityManagerFactory factory = createTestEntityManagerFactory();
        return factory.createEntityManager();
    }
    
    /**
     * Executes a test operation within a transaction, ensuring proper cleanup.
     * 
     * @param em the EntityManager to use
     * @param operation the test operation to execute
     */
    public static void executeInTransaction(EntityManager em, TestOperation operation) {
        try {
            em.getTransaction().begin();
            operation.execute(em);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Test transaction failed", e);
        }
    }
    
    /**
     * Functional interface for test operations that need database access.
     */
    @FunctionalInterface
    public interface TestOperation {
        void execute(EntityManager em) throws Exception;
    }
}