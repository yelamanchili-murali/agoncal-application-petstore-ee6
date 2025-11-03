# Application Entry & Bootstrapping Migration

## Overview
Migration from Java EE container-managed lifecycle to Spring Boot's auto-configuration and component scanning approach.

## Current State Analysis

### Java EE Bootstrap
- **Container-managed**: Application server handles lifecycle
- **web.xml**: Servlet configuration and JSF setup
- **CDI/EJB**: Bean discovery via `beans.xml`
- **JNDI**: Resource lookup and injection

### Current web.xml Structure
```xml
<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee">
    <!-- JSF Servlet Configuration -->
    <servlet>
        <servlet-name>Faces Servlet</servlet-name>
        <servlet-class>javax.faces.webapp.FacesServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>Faces Servlet</servlet-name>
        <url-pattern>*.xhtml</url-pattern>
    </servlet-mapping>

    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
    </welcome-file-list>
</web-app>
```

## Target State

### Spring Boot Main Application Class
```java
package org.agoncal.application.petstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot main application class for the Petstore application.
 * 
 * Migrated from Java EE 6 container-managed lifecycle to Spring Boot
 * auto-configuration and embedded server approach.
 */
@SpringBootApplication(scanBasePackages = {
    "org.agoncal.application.petstore"
})
@EntityScan(basePackages = {
    "org.agoncal.application.petstore.domain"
})
@EnableJpaRepositories(basePackages = {
    "org.agoncal.application.petstore.repository"
})
@EnableTransactionManagement
public class PetstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetstoreApplication.class, args);
    }
}
```

### Component Scan Configuration

#### Package Structure Mapping
```
org.agoncal.application.petstore/
├── PetstoreApplication.java          // @SpringBootApplication
├── config/                           // @Configuration classes
│   ├── WebConfig.java
│   ├── DatabaseConfig.java
│   └── ThymeleafConfig.java
├── controller/                       // @Controller (former JSF controllers)
│   ├── CatalogController.java
│   ├── AccountController.java
│   └── ShoppingCartController.java
├── service/                          // @Service (former @Stateless EJBs)
│   ├── CatalogService.java
│   ├── CustomerService.java
│   └── OrderService.java
├── repository/                       // @Repository (JPA repositories)
│   ├── CategoryRepository.java
│   ├── ProductRepository.java
│   └── ItemRepository.java
├── domain/                           // @Entity classes (unchanged)
└── util/                            // @Component utilities
```

### Web Configuration (Replaces web.xml)

#### WebConfig.java
```java
package org.agoncal.application.petstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure static resource handling (CSS, JS, images)
     * Replaces default servlet mapping from web.xml
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("classpath:/static/resources/");
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/resources/css/");
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/resources/images/");
        
        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/resources/icons/");
    }

    /**
     * Configure view controllers for simple page mappings
     * Replaces welcome-file-list from web.xml
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/index").setViewName("forward:/index.html");
    }
}
```

#### Filter Configuration (if needed)
```java
package org.agoncal.application.petstore.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

@Configuration
public class FilterConfig {

    /**
     * Character encoding filter (replaces web.xml filter configuration)
     */
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilter() {
        FilterRegistrationBean<CharacterEncodingFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        
        registrationBean.setFilter(characterEncodingFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        
        return registrationBean;
    }
}
```

### Exception Handler Configuration
```java
package org.agoncal.application.petstore.config;

import org.agoncal.application.petstore.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Global exception handler replacing JSF exception handling
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleValidationException(ValidationException ex) {
        ModelAndView mav = new ModelAndView("error/validation");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGenericException(Exception ex) {
        ModelAndView mav = new ModelAndView("error/generic");
        mav.addObject("errorMessage", "An unexpected error occurred");
        return mav;
    }
}
```

## Bean Discovery & Component Scanning

### EJB → Spring Service Mapping

#### Original EJB Service
```java
@Stateless
@Loggable
public class CatalogService implements Serializable {
    @Inject
    private EntityManager em;
    // ... business methods
}
```

#### Spring Boot Service
```java
@Service
@Transactional
@Slf4j
public class CatalogService {
    private final EntityManager entityManager;
    
    public CatalogService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    // ... business methods (same logic)
}
```

### CDI → Spring DI Migration

#### Injection Mapping
| Java EE | Spring Boot |
|---------|-------------|
| `@Inject` | `@Autowired` or Constructor Injection |
| `@EJB` | `@Autowired` |
| `@Resource` | `@Value` or `@Autowired` |
| `@Named` | `@Component` |
| `@Stateless` | `@Service` |

## Application Properties

### Profile-based Configuration
```yaml
# application.yml
spring:
  application:
    name: petstore-springboot
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

---
# Dev Profile
spring:
  config:
    activate:
      on-profile: dev
  h2:
    console:
      enabled: true
  
---
# Test Profile  
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:h2:mem:testdb
    
---
# Production Profile
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DATABASE_URL:jdbc:derby:petstoreDB;create=true}
```

## Removed Files & Configurations

### Files to Remove
- `src/main/webapp/WEB-INF/web.xml` ❌
- `src/main/resources/META-INF/beans.xml` ❌ (Spring handles bean discovery)

### Configurations Replaced
| Java EE Feature | Spring Boot Replacement |
|-----------------|-------------------------|
| web.xml servlets | @Controller + auto-config |
| web.xml filters | FilterRegistrationBean |
| web.xml listeners | ApplicationListener beans |
| beans.xml scanning | @ComponentScan |
| JNDI resources | application.properties |

## Startup & Lifecycle

### Development Startup
```bash
mvn spring-boot:run
# OR
mvn clean package && java -jar target/petstore-springboot.jar
```

### Production Deployment
```bash
java -Dspring.profiles.active=prod -jar petstore-springboot.jar
```

### Docker Deployment
```dockerfile
FROM openjdk:17-jre-slim
COPY target/petstore-springboot.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Component Scan Boundaries

### Explicit Configuration
```java
@SpringBootApplication(
    scanBasePackages = {
        "org.agoncal.application.petstore.service",     // Former EJBs
        "org.agoncal.application.petstore.controller",   // Web controllers
        "org.agoncal.application.petstore.repository",   // Data access
        "org.agoncal.application.petstore.config",       // Configuration
        "org.agoncal.application.petstore.util"          // Utilities
    },
    exclude = {
        // Exclude auto-configurations if needed
    }
)
```

## Testing Configuration

### Test Application Context
```java
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}
```

## Migration Checklist

- [ ] Create `PetstoreApplication.java` main class
- [ ] Configure component scanning boundaries
- [ ] Create `WebConfig.java` to replace web.xml
- [ ] Set up exception handling with `@ControllerAdvice`
- [ ] Configure profiles in `application.yml`
- [ ] Remove `web.xml` and `beans.xml`
- [ ] Test application startup
- [ ] Verify bean injection works
- [ ] Test resource mapping

## Next Steps

1. Create the main application class
2. Configure component scanning
3. Test basic application startup
4. Proceed to configuration migration (persistence.xml → application.yml)