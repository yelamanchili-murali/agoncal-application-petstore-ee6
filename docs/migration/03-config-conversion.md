# Configuration Migration

## Overview
Migration from Java EE XML-based configuration (persistence.xml, web.xml) to Spring Boot's application.yml and Java configuration approach.

## Current State Analysis

### Existing persistence.xml
```xml
<persistence-unit name="applicationPetstorePU" transaction-type="JTA">
    <jta-data-source>java:global/jdbc/applicationPetstoreDS</jta-data-source>
    <properties>
        <!-- Properties for EclipseLink -->
        <property name="eclipselink.target-database" value="DERBY"/>
        <property name="eclipselink.ddl-generation" value="drop-and-create-tables"/>
        <property name="eclipselink.logging.level" value="INFO"/>

        <!-- Properties for Hibernate -->
        <property name="hibernate.hbm2ddl.auto" value="create-drop"/>
        <property name="hibernate.show_sql" value="true"/>
        <property name="hibernate.format_sql" value="true"/>
        <property name="hibernate.dialect" value="org.hibernate.dialect.DerbyTenSevenDialect"/>
    </properties>
</persistence-unit>
```

### Current Resource Configuration
- JNDI DataSource: `java:global/jdbc/applicationPetstoreDS`
- Container-managed transactions (JTA)
- Multiple JPA provider support

## Target State: Spring Boot Configuration

### application.yml Structure

#### Base Configuration
```yaml
# application.yml (base configuration)
spring:
  application:
    name: petstore-springboot
    
  # JPA & Hibernate Configuration
  jpa:
    hibernate:
      ddl-auto: validate  # Default - no automatic schema changes
    properties:
      hibernate:
        dialect: org.hibernate.dialect.DerbyDialect
        format_sql: true
        use_sql_comments: true
    show-sql: false  # Controlled per profile
    open-in-view: false  # Prevent lazy loading in views
    
  # HikariCP Connection Pool (Default in Spring Boot)
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      pool-name: PetstoreHikariPool
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      leak-detection-threshold: 60000
      
  # Transaction Management
  transaction:
    default-timeout: 30s
    rollback-on-commit-failure: true
    
  # Internationalization
  messages:
    basename: messages
    encoding: UTF-8
    fallback-to-system-locale: false
    
  # Web Configuration
  web:
    resources:
      static-locations: classpath:/static/
    
  # Profile Selection
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
    
# Application-specific Properties
petstore:
  database:
    init-mode: always  # always, embedded, never
  security:
    session-timeout: 30m
  cache:
    enabled: true
    ttl: 1h
```

### Profile-Specific Configurations

#### Development Profile (application-dev.yml)
```yaml
# application-dev.yml
spring:
  # Database Configuration - Embedded Derby
  datasource:
    url: jdbc:derby:memory:petstoreDB;create=true
    username: app
    password: app
    driver-class-name: org.apache.derby.jdbc.EmbeddedDriver
    
  # JPA Development Settings
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreate schema on startup
    show-sql: true
    properties:
      hibernate:
        generate_statistics: true
        
  # H2 Console (Alternative for debugging)
  h2:
    console:
      enabled: false
      
  # Logging Configuration
  logging:
    level:
      org.hibernate.SQL: DEBUG
      org.hibernate.type.descriptor.sql.BasicBinder: TRACE
      org.agoncal.application.petstore: DEBUG
      org.springframework.transaction: DEBUG
      
# Development-specific properties
petstore:
  database:
    populate-sample-data: true
  debug:
    enabled: true
```

#### Test Profile (application-test.yml)
```yaml
# application-test.yml
spring:
  # Test Database - H2 In-Memory
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
    driver-class-name: org.h2.Driver
    
  # JPA Test Settings
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        
  # Test-specific configuration
  h2:
    console:
      enabled: true
      path: /h2-console
      
# Test-specific properties
petstore:
  database:
    populate-sample-data: true
  cache:
    enabled: false  # Disable caching in tests
```

#### Production Profile (application-prod.yml)
```yaml
# application-prod.yml
spring:
  # Production Database Configuration
  datasource:
    url: ${DATABASE_URL:jdbc:derby:/opt/petstore/data/petstoreDB;create=true}
    username: ${DB_USERNAME:petstore_user}
    password: ${DB_PASSWORD}
    driver-class-name: org.apache.derby.jdbc.ClientDriver
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      
  # JPA Production Settings
  jpa:
    hibernate:
      ddl-auto: validate  # Never auto-modify production schema
    show-sql: false
    properties:
      hibernate:
        jdbc:
          batch_size: 25
        cache:
          use_second_level_cache: true
          provider_class: org.hibernate.cache.jcache.JCacheRegionFactory
          
  # Logging - Production Level
logging:
  level:
    org.hibernate.SQL: WARN
    org.agoncal.application.petstore: INFO
    root: WARN
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/petstore/application.log
    
# Production-specific properties
petstore:
  database:
    populate-sample-data: false
  security:
    session-timeout: 15m
```

## Java Configuration Classes

### Database Configuration
```java
package org.agoncal.application.petstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    /**
     * H2 DataSource for testing
     */
    @Bean
    @Profile("test")
    public DataSource h2DataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .build();
    }
    
    /**
     * Configuration properties for database settings
     */
    @ConfigurationProperties(prefix = "petstore.database")
    public static class DatabaseProperties {
        private String initMode = "always";
        private boolean populateSampleData = true;
        
        // Getters and setters...
    }
}
```

### JPA Configuration
```java
package org.agoncal.application.petstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(
    basePackages = "org.agoncal.application.petstore.repository",
    enableDefaultTransactions = true
)
@EnableTransactionManagement
public class JpaConfig {
    
    /**
     * Custom transaction manager if needed
     */
    // @Bean
    // public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
    //     return new JpaTransactionManager(emf);
    // }
}
```

## View Technology Migration Strategy

### Phase 1: Thymeleaf Migration (Recommended)

#### Thymeleaf Configuration
```java
package org.agoncal.application.petstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCacheable(false); // Dev mode
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setOrder(1);
        return viewResolver;
    }
}
```

### Page-by-Page Migration Plan

| JSF Page | Thymeleaf Template | Controller Method |
|----------|-------------------|------------------|
| `index.html` | `templates/index.html` | `HomeController.index()` |
| `showproducts.xhtml` | `templates/catalog/products.html` | `CatalogController.showProducts()` |
| `showitems.xhtml` | `templates/catalog/items.html` | `CatalogController.showItems()` |
| `showitem.xhtml` | `templates/catalog/item-detail.html` | `CatalogController.showItem()` |
| `showcart.xhtml` | `templates/cart/show.html` | `CartController.showCart()` |
| `signon.xhtml` | `templates/auth/login.html` | `AuthController.login()` |
| `createaccount.xhtml` | `templates/account/register.html` | `AccountController.register()` |
| `confirmorder.xhtml` | `templates/order/confirm.html` | `OrderController.confirm()` |

### Alternative: Keep JSF Temporarily
```yaml
# If keeping JSF initially
spring:
  mvc:
    view:
      suffix: .xhtml
    servlet:
      path: /faces

# Add JSF dependencies to pom.xml
```

## JNDI to Properties Migration

### Before (JNDI)
```xml
<jta-data-source>java:global/jdbc/applicationPetstoreDS</jta-data-source>
```

### After (Spring Boot Properties)
```yaml
spring:
  datasource:
    jndi-name: false  # Disable JNDI lookup
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## Resource Bundle Configuration

### Internationalization Setup
```yaml
spring:
  messages:
    basename: messages
    encoding: UTF-8
    fallback-to-system-locale: false
```

### Message Files Location
```
src/main/resources/
├── messages.properties (default)
├── messages_en.properties
├── messages_fr.properties
└── messages_es.properties
```

## Environment Variables & External Configuration

### Environment-Based Configuration
```bash
# Development
export SPRING_PROFILES_ACTIVE=dev
export DATABASE_URL=jdbc:derby:memory:petstoreDB;create=true

# Production
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://localhost:5432/petstore
export DB_USERNAME=petstore_user
export DB_PASSWORD=secure_password
```

### External Configuration Files
```
/etc/petstore/
├── application.yml          # Override default settings
├── application-prod.yml     # Production-specific
└── logback-spring.xml       # Logging configuration
```

## Migration Mapping Summary

| Java EE Configuration | Spring Boot Equivalent |
|----------------------|------------------------|
| `persistence.xml` | `application.yml` + `@EnableJpa*` |
| `web.xml` servlets | `@Controller` + auto-config |
| `web.xml` filters | `FilterRegistrationBean` |
| `faces-config.xml` | `ThymeleafConfig` + templates |
| JNDI resources | `application.yml` properties |
| Container transactions | `@Transactional` + Spring TX |
| Multiple JPA providers | Single Hibernate provider |

## Validation & Testing

### Configuration Validation
```bash
# Validate configuration
mvn spring-boot:run -Dspring.profiles.active=dev
curl http://localhost:8080/actuator/configprops
curl http://localhost:8080/actuator/env
```

### Profile Testing
```bash
# Test each profile
mvn test -Dspring.profiles.active=test
mvn spring-boot:run -Dspring.profiles.active=dev
java -jar target/petstore.jar --spring.profiles.active=prod
```

## Migration Checklist

- [ ] Create `application.yml` base configuration
- [ ] Create profile-specific configurations (dev, test, prod)
- [ ] Set up database configuration with HikariCP
- [ ] Configure JPA/Hibernate properties
- [ ] Set up internationalization
- [ ] Configure Thymeleaf (or JSF transition)
- [ ] Remove `persistence.xml`
- [ ] Test database connectivity
- [ ] Validate transaction management
- [ ] Test profile switching

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| JTA → Spring TX differences | High | Comprehensive transaction testing |
| Multi-provider → Hibernate only | Medium | Validate Hibernate compatibility |
| JNDI dependencies | Medium | External configuration management |
| View technology change | High | Gradual JSF → Thymeleaf migration |

## Next Steps

1. Create application.yml configurations
2. Set up profile-specific properties
3. Configure database connectivity
4. Test transaction management
5. Proceed to EJB → Spring Services migration