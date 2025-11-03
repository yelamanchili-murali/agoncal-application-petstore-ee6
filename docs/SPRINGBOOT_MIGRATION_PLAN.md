# Spring Boot Migration Plan - Java EE 6 JSF+EJB to Spring Boot 3.x

## Executive Summary

This document provides a comprehensive migration plan for transitioning the **Petstore Java EE 6 application** from a JSF+EJB architecture to **Spring Boot 3.x** with modern Java 17+ features, while preserving all existing business functionality captured in characterisation artifacts.

### Migration Overview
- **Source**: Java EE 6 (JSF 2.0, EJB 3.1, JPA 2.0, Java 6)
- **Target**: Spring Boot 3.x (Spring MVC, Spring Data JPA, Thymeleaf, Java 17)
- **Scope**: Complete framework migration with behavioral preservation
- **Approach**: Incremental migration with rollback capabilities

### Business Justification
- **Modernization**: Move to actively maintained, cloud-native platform
- **Performance**: Leverage embedded server and Spring Boot optimizations  
- **Maintainability**: Reduce complexity and improve developer productivity
- **Ecosystem**: Access to modern Spring ecosystem and tooling
- **Future-proofing**: Java 17+ features and long-term support

## Migration Architecture

### High-Level Architecture Transformation

```
┌─────────────────────────────────────────┐    ┌─────────────────────────────────────────┐
│              Java EE 6                  │    │            Spring Boot 3.x              │
├─────────────────────────────────────────┤    ├─────────────────────────────────────────┤
│ Presentation Layer                      │    │ Presentation Layer                      │
│ • JSF 2.0 + Facelets                    │───▶| • Spring MVC + Thymeleaf               │
│ • faces-config.xml navigation           │    │ • RESTful routing                       │
│ • @Named managed beans                  │    │ • @Controller classes                   │
├─────────────────────────────────────────┤    ├─────────────────────────────────────────┤
│ Business Layer                          │    │ Business Layer                          │
│ • @Stateless EJBs                       │───▶│ • @Service classes                     │
│ • Container-managed transactions        │    │ • @Transactional methods                │
│ • @Inject dependency injection          │    │ • Constructor injection                 │
├─────────────────────────────────────────┤    ├─────────────────────────────────────────┤
│ Persistence Layer                       │    │ Persistence Layer                       │
│ • JPA 2.0 entities                      │───▶│ • JPA 3.1 entities (jakarta.*)         │
│ • persistence.xml configuration         │    │ • Spring Data JPA repositories          │
│ • Container-managed EntityManager       │    │ • application.yml configuration         │
├─────────────────────────────────────────┤    ├─────────────────────────────────────────┤
│ Infrastructure                          │    │ Infrastructure                          │
│ • Application Server (GlassFish/JBoss)  │───▶│ • Embedded Tomcat                      │
│ • web.xml + beans.xml                   │    │ • Spring Boot auto-configuration        │
│ • JNDI resources                        │    │ • application.yml properties            │
└─────────────────────────────────────────┘    └─────────────────────────────────────────┘
```

## Migration Roadmap

The migration is structured into 8 distinct phases, each with specific deliverables and validation criteria:

### Phase 1: Build & Packaging Migration
**Duration**: 1 week  
**Deliverable**: [`01-build-and-packaging.md`](./migration/01-build-and-packaging.md)

**Objectives**:
- Migrate from WAR packaging to executable JAR
- Replace Java EE dependencies with Spring Boot starters
- Upgrade Java 6 → Java 17
- Configure Maven for Spring Boot

**Key Changes**:
```xml
<!-- Before: Java EE WAR -->
<packaging>war</packaging>
<properties>
    <version.java>1.6</version.java>
    <version.javaee>6.0</version.javaee>
</properties>

<!-- After: Spring Boot JAR -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
<packaging>jar</packaging>
<properties>
    <java.version>17</java.version>
</properties>
```

### Phase 2: Application Entry & Bootstrapping
**Duration**: 1 week  
**Deliverable**: [`02-bootstrapping.md`](./migration/02-bootstrapping.md)

**Objectives**:
- Create Spring Boot main application class
- Replace web.xml with Java configuration
- Configure component scanning
- Remove container dependencies

**Key Changes**:
```java
// New Spring Boot Application Entry Point
@SpringBootApplication(scanBasePackages = "org.agoncal.application.petstore")
@EntityScan(basePackages = "org.agoncal.application.petstore.domain")
@EnableJpaRepositories(basePackages = "org.agoncal.application.petstore.repository")
public class PetstoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetstoreApplication.class, args);
    }
}
```

### Phase 3: Configuration Migration  
**Duration**: 1 week  
**Deliverable**: [`03-config-conversion.md`](./migration/03-config-conversion.md)

**Objectives**:
- Convert persistence.xml → application.yml
- Replace JNDI with Spring properties
- Configure multiple profiles (dev, test, prod)
- Set up Thymeleaf view resolution

**Key Changes**:
```yaml
# application.yml - Replace persistence.xml + JNDI
spring:
  datasource:
    url: jdbc:derby:memory:petstoreDB;create=true
    username: app
    password: app
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

### Phase 4: EJB to Spring Services Migration
**Duration**: 2 weeks  
**Deliverable**: [`04-ejb-to-spring.md`](./migration/04-ejb-to-spring.md)

**Objectives**:
- Convert @Stateless EJBs to @Service classes
- Replace @TransactionAttribute with @Transactional
- Migrate from CDI to Spring DI
- Preserve transaction semantics

**Key Changes**:
```java
// Before: EJB
@Stateless
@Loggable  
public class CatalogService {
    @Inject
    private EntityManager em;
}

// After: Spring Service
@Service
@Transactional(readOnly = true)
@Slf4j
public class CatalogService {
    private final EntityManager entityManager;
    
    public CatalogService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
```

### Phase 5: JSF to Spring MVC Migration
**Duration**: 3 weeks  
**Deliverable**: [`05-jsf-to-springmvc.md`](./migration/05-jsf-to-springmvc.md)

**Objectives**:
- Replace JSF managed beans with Spring MVC controllers
- Convert .xhtml Facelets to Thymeleaf templates
- Implement RESTful URL patterns
- Migrate session management

**Key Changes**:
```java
// Before: JSF Managed Bean
@Named
@SessionScoped
public class CatalogController {
    public String doFindProducts() {
        products = catalogService.findProducts(categoryName);
        return "showproducts.faces";
    }
}

// After: Spring MVC Controller  
@Controller
@RequestMapping("/catalog")
public class CatalogController {
    @GetMapping("/{categoryName}")
    public String showProducts(@PathVariable String categoryName, Model model) {
        List<Product> products = catalogService.findProducts(categoryName);
        model.addAttribute("products", products);
        return "catalog/products"; // Thymeleaf template
    }
}
```

### Phase 6: Observability Implementation
**Duration**: 1 week  
**Deliverable**: [`06-observability.md`](./migration/06-observability.md)

**Objectives**:
- Add Spring Boot Actuator endpoints
- Implement health checks and metrics
- Configure structured logging
- Set up monitoring integration

**Key Changes**:
```yaml
# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: petstore-springboot
```

### Phase 7: Test Migration & Validation
**Duration**: 2 weeks  
**Deliverable**: [`07-test-migration.md`](./migration/07-test-migration.md)

**Objectives**:
- Convert Arquillian tests to Spring Boot tests
- Implement test slices (@WebMvcTest, @DataJpaTest)
- Preserve characterisation test assertions
- Add integration tests with Testcontainers

**Key Changes**:
```java
// Before: Arquillian Test
@RunWith(Arquillian.class)
public class CatalogServiceTest {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)...;
    }
    
    @EJB
    private CatalogService catalogService;
}

// After: Spring Boot Test
@SpringBootTest
@Transactional
class CatalogServiceTest {
    @Autowired
    private CatalogService catalogService;
    
    @Test
    @DisplayName("Should preserve original behavior")
    void shouldCreateCategory() {
        // Test preserves characterisation behavior
    }
}
```

### Phase 8: Risk Mitigation & Rollback Planning
**Duration**: 1 week  
**Deliverable**: [`08-risks-and-rollback.md`](./migration/08-risks-and-rollback.md)

**Objectives**:
- Document comprehensive risk assessment
- Implement rollback procedures  
- Create monitoring and alerting
- Plan parallel deployment strategy

## Complete Page Migration Map

### JSF to Spring MVC + Thymeleaf Route Mapping

| Original JSF Page | New Spring MVC Route | Controller Method | Template |
|------------------|---------------------|-------------------|----------|
| `index.html` | `GET /` | `HomeController.index()` | `index.html` |
| `signon.xhtml` | `GET /auth/login` | `AuthController.loginForm()` | `auth/login.html` |
| `signon.xhtml` | `POST /auth/login` | `AuthController.processLogin()` | redirect |
| `createaccount.xhtml` | `GET /auth/register` | `AuthController.registerForm()` | `auth/register.html` |
| `showaccount.xhtml` | `GET /account/profile` | `AccountController.showProfile()` | `account/profile.html` |
| `updateaccount.xhtml` | `PUT /account` | `AccountController.updateProfile()` | redirect |
| `showproducts.xhtml` | `GET /catalog/{categoryName}` | `CatalogController.showProducts()` | `catalog/products.html` |
| `showitems.xhtml` | `GET /catalog/products/{productId}` | `CatalogController.showItems()` | `catalog/items.html` |  
| `showitem.xhtml` | `GET /catalog/items/{itemId}` | `CatalogController.showItem()` | `catalog/item-detail.html` |
| `showcart.xhtml` | `GET /cart` | `CartController.showCart()` | `cart/show.html` |
| `confirmorder.xhtml` | `GET /order/confirm` | `OrderController.confirmOrder()` | `order/confirm.html` |
| `orderconfirmed.xhtml` | `POST /orders` | `OrderController.placeOrder()` | `order/confirmed.html` |
| `searchresult.xhtml` | `GET /search?q={keyword}` | `SearchController.search()` | `search/results.html` |

## Technology Stack Comparison

### Before (Java EE 6)
```
┌─────────────────────────┐
│ Application Server      │
├─────────────────────────┤
│ • GlassFish 3.x         │
│ • JBoss AS 7.x          │  
│ • TomEE 1.x             │
└─────────────────────────┘
┌─────────────────────────┐
│ Web Framework           │
├─────────────────────────┤
│ • JSF 2.0               │
│ • Facelets              │
│ • Navigation Rules      │
└─────────────────────────┘
┌─────────────────────────┐
│ Business Logic          │
├─────────────────────────┤
│ • EJB 3.1               │
│ • CDI 1.0               │
│ • Container Managed TX  │
└─────────────────────────┘
┌─────────────────────────┐
│ Data Access             │
├─────────────────────────┤
│ • JPA 2.0               │
│ • EclipseLink/Hibernate │
│ • JNDI DataSources      │
└─────────────────────────┘
```

### After (Spring Boot 3.x)
```
┌─────────────────────────┐
│ Embedded Server         │
├─────────────────────────┤
│ • Embedded Tomcat       │
│ • Executable JAR        │
│ • Auto-configuration    │
└─────────────────────────┘
┌─────────────────────────┐
│ Web Framework           │
├─────────────────────────┤
│ • Spring MVC 6.x        │
│ • Thymeleaf 3.x         │
│ • RESTful Controllers   │
└─────────────────────────┘
┌─────────────────────────┐
│ Business Logic          │
├─────────────────────────┤
│ • Spring Services       │
│ • Spring DI             │
│ • Spring Transactions   │
└─────────────────────────┘
┌─────────────────────────┐
│ Data Access             │
├─────────────────────────┤
│ • JPA 3.1 (jakarta.*)   │
│ • Spring Data JPA       │
│ • HikariCP Pool         │
└─────────────────────────┘
```

## Key Benefits & Improvements

### Performance Improvements
- **Startup Time**: ~50% faster with embedded server vs application server
- **Memory Usage**: ~30% reduction through optimized dependency management
- **Response Time**: Improved through connection pooling and caching
- **Build Time**: Faster incremental builds with Spring Boot DevTools

### Developer Experience
- **Hot Reload**: Spring Boot DevTools for rapid development
- **Testing**: Comprehensive test slices for faster, focused testing
- **Monitoring**: Built-in Actuator endpoints for operational insights
- **Configuration**: Externalized configuration with profiles

### Operational Benefits
- **Deployment**: Single executable JAR deployment
- **Scaling**: Cloud-native deployment patterns
- **Monitoring**: Integrated metrics and health checks
- **Security**: Spring Security integration

## Risk Assessment Summary

### High Risk Items
1. **javax → jakarta namespace migration** - Automated tooling available
2. **JSF → Spring MVC conversion** - Significant UI changes required  
3. **Transaction behavior differences** - Comprehensive testing planned

### Medium Risk Items
1. **CDI → Spring DI container differences** - Well-documented migration path
2. **JPA provider behavior changes** - Hibernate standardization
3. **Loss of application server features** - Spring alternatives available

### Mitigation Strategies
- **Parallel deployment** during migration period
- **Feature flags** for gradual rollout
- **Comprehensive rollback procedures** tested and validated
- **Performance monitoring** with automatic rollback triggers

## Success Criteria

### Functional Requirements
- [ ] All existing business functionality preserved
- [ ] All user workflows operational
- [ ] Data integrity maintained throughout migration
- [ ] Authentication and authorization working

### Performance Requirements  
- [ ] Response times within 10% of legacy system
- [ ] Error rates below 0.1%
- [ ] System availability > 99.9%
- [ ] Concurrent user capacity maintained

### Quality Requirements
- [ ] Test coverage > 80%
- [ ] All characterisation tests passing
- [ ] Security vulnerabilities resolved
- [ ] Code quality metrics met

## Timeline & Resource Planning

### Overall Timeline: 10-12 weeks

| Phase | Duration | Resources Required | Dependencies |
|-------|----------|-------------------|--------------|
| Build & Packaging | 1 week | 1 Senior Dev | None |
| Bootstrapping | 1 week | 1 Senior Dev | Phase 1 complete |
| Configuration | 1 week | 1 Senior Dev | Phase 2 complete |
| EJB Migration | 2 weeks | 2 Senior Devs | Phase 3 complete |
| JSF Migration | 3 weeks | 2 Senior Devs, 1 UI Dev | Phase 4 complete |
| Observability | 1 week | 1 Senior Dev, 1 DevOps | Phase 5 complete |
| Test Migration | 2 weeks | 2 Senior Devs | All phases complete |
| Risk & Rollback | 1 week | 1 Senior Dev, 1 DevOps | Parallel to testing |

### Resource Requirements
- **3 Senior Java Developers** (Spring Boot experience required)
- **1 UI/Frontend Developer** (Thymeleaf experience preferred)
- **1 DevOps Engineer** (Docker, monitoring experience)
- **1 Technical Lead** (Migration oversight and decision making)

## Validation & Go-Live Criteria

### Pre-Go-Live Checklist
- [ ] All migration phases completed and validated
- [ ] Performance testing passed with acceptable results
- [ ] Security testing completed without critical findings
- [ ] Rollback procedures tested and verified
- [ ] Monitoring and alerting fully configured
- [ ] Documentation updated and training completed

### Go-Live Decision Points
✅ **Go**: All tests pass, performance within 10% of baseline, rollback verified  
⚠️ **Conditional Go**: Minor issues identified with mitigation plan  
❌ **No-Go**: Critical issues, performance degradation >20%, or rollback failures

## Post-Migration Activities

### Immediate (Week 1)
- [ ] Monitor system performance and error rates
- [ ] Address any immediate issues or performance concerns  
- [ ] Validate all business critical workflows
- [ ] Collect user feedback and address usability issues

### Short-term (Month 1)
- [ ] Performance optimization based on production metrics
- [ ] Technical debt reduction and code cleanup
- [ ] Documentation updates and knowledge transfer
- [ ] Training completion for support teams

### Long-term (Months 2-3)
- [ ] Leverage Spring Boot ecosystem features
- [ ] Implement advanced monitoring and observability
- [ ] Cloud deployment preparation
- [ ] Performance tuning and optimization

---

## Document Index

This migration plan consists of the following detailed implementation guides:

1. **[Build & Packaging Migration](./migration/01-build-and-packaging.md)**
   - Maven POM transformation
   - Java 17 upgrade strategy
   - Dependency management

2. **[Application Bootstrapping](./migration/02-bootstrapping.md)**
   - Spring Boot main class creation
   - Component scanning configuration
   - Web configuration setup

3. **[Configuration Migration](./migration/03-config-conversion.md)**
   - persistence.xml → application.yml
   - Profile-based configuration
   - Thymeleaf setup

4. **[EJB to Spring Services](./migration/04-ejb-to-spring.md)**
   - Service layer transformation
   - Transaction management migration
   - Dependency injection patterns

5. **[JSF to Spring MVC](./migration/05-jsf-to-springmvc.md)**
   - Controller implementation
   - Thymeleaf template creation
   - Navigation flow migration

6. **[Observability Setup](./migration/06-observability.md)**
   - Actuator configuration
   - Health checks and metrics
   - Monitoring integration

7. **[Test Migration Strategy](./migration/07-test-migration.md)**
   - Arquillian → Spring Boot tests
   - Characterisation test preservation
   - Integration test setup

8. **[Risks & Rollback Planning](./migration/08-risks-and-rollback.md)**
   - Comprehensive risk assessment
   - Rollback procedures
   - Contingency planning

Each document provides detailed technical guidance, code examples, and migration steps for successful completion of the Java EE 6 to Spring Boot 3.x transition while preserving all existing business functionality.

---
