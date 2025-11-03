# Risks & Rollback Strategy

## Overview
Comprehensive risk assessment and mitigation strategies for the Java EE 6 to Spring Boot 3.x migration, with detailed rollback procedures and contingency planning.

## Migration Risk Assessment

### High-Risk Areas

#### 1. javax → jakarta Namespace Migration
**Risk Level: HIGH**
**Impact**: All Java EE APIs change namespace

| Component | Java EE 6 (javax) | Spring Boot 3.x (jakarta) | Risk |
|-----------|-------------------|---------------------------|------|
| Servlet API | `javax.servlet.*` | `jakarta.servlet.*` | Breaking changes |
| JPA | `javax.persistence.*` | `jakarta.persistence.*` | Entity mapping issues |
| Bean Validation | `javax.validation.*` | `jakarta.validation.*` | Annotation compatibility |
| CDI | `javax.inject.*` | `jakarta.inject.*` | Dependency injection |
| EJB | `javax.ejb.*` | N/A (Removed) | Complete replacement needed |

**Mitigation Strategies:**
```java
// Before (Java EE 6)
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.ejb.Stateless;

// After (Spring Boot 3.x)  
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service; // EJB replacement
```

**Automated Migration Tools:**
```bash
# Use OpenRewrite for namespace migration
mvn org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:LATEST \
  -Drewrite.activeRecipes=org.openrewrite.java.migrate.JavaxToJakarta
```

#### 2. JSF to Spring MVC + Thymeleaf
**Risk Level: HIGH**
**Impact**: Complete UI framework change

**JSF Feature Parity Gaps:**

| JSF Feature | Spring MVC + Thymeleaf | Risk Level | Mitigation |
|-------------|----------------------|------------|------------|
| Component Tree | No direct equivalent | High | Manual page conversion |
| Postback Model | RESTful routing | High | URL pattern redesign |
| Managed Beans | Controllers + DTOs | Medium | State management changes |
| Implicit Navigation | Explicit redirects | Medium | Navigation mapping |
| Built-in Ajax | Custom JavaScript | High | REST API development |
| Conversion/Validation | Spring Validation | Low | Similar concepts |

**Conversion Strategy:**
```java
// JSF Managed Bean (Before)
@Named
@SessionScoped
public class CatalogController {
    private List<Product> products;
    
    public String doFindProducts() {
        products = catalogService.findProducts(categoryName);
        return "showproducts.faces";
    }
}

// Spring MVC Controller (After)
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

#### 3. EJB Transaction Semantics vs Spring Transactions
**Risk Level: MEDIUM-HIGH**
**Impact**: Transaction boundary and rollback behavior differences

**Transaction Differences:**

| Aspect | EJB Container | Spring Framework | Risk |
|--------|---------------|------------------|------|
| Default Behavior | REQUIRED | REQUIRED | Low |
| Checked Exceptions | No rollback | No rollback | Low |
| Runtime Exceptions | Rollback | Rollback | Low |
| Timeout Handling | Container-specific | Configurable | Medium |
| Nested Transactions | REQUIRES_NEW | REQUIRES_NEW | Low |
| Resource Management | Container-managed | Framework-managed | Medium |

**Behavioral Validation:**
```java
// Test transaction rollback behavior
@Test
@DisplayName("Should preserve EJB rollback semantics")
void shouldPreserveTransactionRollbackBehavior() {
    // Given
    Customer customer = createTestCustomer();
    
    // When - runtime exception should rollback
    assertThatThrownBy(() -> {
        customerService.createCustomerWithRuntimeException(customer);
    }).isInstanceOf(RuntimeException.class);
    
    // Then - no customer should be persisted (same as EJB behavior)
    assertThat(customerService.findCustomer(customer.getLogin())).isEmpty();
}
```

#### 4. JNDI Resource Dependencies
**Risk Level: MEDIUM**
**Impact**: DataSource and resource configuration changes

**JNDI Migration:**
```xml
<!-- Before: Java EE JNDI -->
<persistence-unit name="applicationPetstorePU" transaction-type="JTA">
    <jta-data-source>java:global/jdbc/applicationPetstoreDS</jta-data-source>
</persistence-unit>
```

```yaml
# After: Spring Boot configuration
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:derby:petstoreDB;create=true}
    username: ${DB_USERNAME:app}
    password: ${DB_PASSWORD:app}
```

### Medium-Risk Areas

#### 1. CDI to Spring DI Container Differences
**Risk Level: MEDIUM**

| Feature | CDI | Spring | Compatibility |
|---------|-----|--------|---------------|
| `@Inject` | Standard | `@Autowired` | High |
| `@Named` | Component naming | `@Component` | High |  
| `@RequestScoped` | Web scope | `@RequestScope` | High |
| `@SessionScoped` | Web scope | `@SessionScope` | High |
| `@Producer` | Factory methods | `@Bean` | Medium |
| Interceptors | `@AroundInvoke` | Spring AOP | Medium |

#### 2. JPA Provider Migration 
**Risk Level: MEDIUM**
**Impact**: Hibernate-specific behavior vs multi-provider support

**Provider Differences:**
```java
// Potential Hibernate-specific behavior that might differ
@Entity
public class Category {
    // EclipseLink vs Hibernate sequence generation
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // May behave differently
    private Long id;
    
    // Lazy loading behavior differences
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "category")
    private List<Product> products; // Proxy behavior may differ
}
```

#### 3. Application Server Features Loss
**Risk Level: MEDIUM**
**Impact**: Loss of enterprise features

| Java EE Feature | Spring Boot Equivalent | Gap |
|-----------------|------------------------|-----|
| JMS | Spring JMS | Configuration complexity |
| JCA | Custom integration | Manual implementation |
| JMX | Actuator + Micrometer | Reduced functionality |
| JAAS | Spring Security | Different programming model |
| Work Manager | TaskExecutor | Different API |

### Low-Risk Areas

#### 1. Bean Validation (JSR-303)
**Risk Level: LOW**
**Impact**: Validation annotations mostly compatible

```java
// Same validation annotations work
@Entity
public class Customer {
    @NotNull
    @Size(max = 10)
    private String login;
    
    @Email  // Custom validator - may need adjustment
    private String email;
}
```

#### 2. JPA Entity Mapping
**Risk Level: LOW**
**Impact**: Entity classes largely unchanged

## Rollback Strategies

### 1. Branch-Based Rollback Strategy

#### Git Branching Strategy
```bash
# Main development branches
git checkout -b migration/spring-boot-main    # Main migration branch
git checkout -b migration/build-packaging     # POM migration
git checkout -b migration/ejb-to-spring       # Service layer
git checkout -b migration/jsf-to-mvc          # Web layer

# Keep original working on separate branch
git checkout -b legacy/java-ee-backup         # Original working code
```

#### Rollback Procedure
```bash
# Emergency rollback to Java EE version
git checkout legacy/java-ee-backup
mvn clean package -P glassfish-embedded
# Deploy to GlassFish server
```

### 2. Parallel Deployment Strategy

#### Infrastructure Setup
```yaml
# docker-compose.yml - Run both versions side by side
version: '3.8'
services:
  petstore-legacy:
    image: petstore-javaee:latest
    ports:
      - "8080:8080"
    environment:
      - ENV=production
      
  petstore-springboot:
    image: petstore-springboot:latest  
    ports:
      - "8081:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      
  nginx-loadbalancer:
    image: nginx
    ports:
      - "80:80"
    depends_on:
      - petstore-legacy
      - petstore-springboot
```

#### Traffic Routing Configuration
```nginx
# nginx.conf - Gradual traffic migration
upstream legacy_backend {
    server petstore-legacy:8080;
}

upstream springboot_backend {  
    server petstore-springboot:8080;
}

server {
    listen 80;
    
    location / {
        # Route 90% to legacy, 10% to Spring Boot initially
        if ($arg_version = "new") {
            proxy_pass http://springboot_backend;
        }
        
        # Gradual migration based on random selection
        set $backend "legacy";
        if ($request_id ~ "[0-9a-f]$") {  # 10% of requests
            set $backend "springboot";
        }
        
        proxy_pass http://${backend}_backend;
    }
}
```

### 3. Strangler Fig Pattern for UI Migration

#### Incremental Page Migration
```java
// Route specific pages to Spring Boot while keeping others in JSF
@Controller
public class MigrationRoutingController {
    
    // New Spring Boot pages
    @GetMapping("/auth/login")
    public String newLogin() {
        return "auth/login"; // Thymeleaf template
    }
    
    // Proxy to legacy JSF for unmigrated pages
    @GetMapping("/legacy/**")
    public void proxyToLegacy(HttpServletRequest request, HttpServletResponse response) {
        // Proxy to Java EE application
        proxyService.forwardToLegacy(request, response);
    }
}
```

#### URL Rewriting Strategy
```java
// Gradually redirect legacy URLs to new Spring Boot endpoints
@Component
public class LegacyUrlRedirectFilter implements Filter {
    
    private final Map<String, String> urlMappings = Map.of(
        "/signon.faces", "/auth/login",
        "/showproducts.faces", "/catalog/{categoryName}",
        "/showcart.faces", "/cart"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        
        if (urlMappings.containsKey(requestURI)) {
            String newUrl = urlMappings.get(requestURI);
            ((HttpServletResponse) response).sendRedirect(newUrl);
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

### 4. Database Migration Rollback

#### Schema Versioning Strategy
```sql
-- Create schema version tracking table
CREATE TABLE schema_migration (
    version VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Track current schema state
INSERT INTO schema_migration (version, description) 
VALUES ('javaee-6.0', 'Original Java EE 6 schema');
```

#### Database Rollback Scripts
```sql
-- rollback-springboot-changes.sql
-- Remove Spring Boot specific changes if needed

-- Drop Spring Security tables if added
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS authorities;

-- Restore original sequence naming if changed
-- ALTER SEQUENCE hibernate_sequence RENAME TO original_sequence;

-- Update schema version
INSERT INTO schema_migration (version, description)
VALUES ('rollback-1.0', 'Rollback from Spring Boot migration');
```

## Contingency Planning

### 1. Performance Degradation Response

#### Performance Monitoring
```java
// Add performance monitoring during migration
@Component
public class MigrationPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleRequestCompleted(RequestCompletedEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        meterRegistry.timer("migration.request.duration",
            "endpoint", event.getEndpoint(),
            "version", "springboot")
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
            
        // Alert if performance degrades > 50%
        if (event.getDuration() > LEGACY_BASELINE * 1.5) {
            alertService.sendPerformanceAlert(event);
        }
    }
}
```

#### Automatic Rollback Trigger
```bash
#!/bin/bash
# performance-monitor.sh - Monitor and auto-rollback if needed

RESPONSE_TIME_THRESHOLD=2000  # 2 seconds
ERROR_RATE_THRESHOLD=5        # 5% error rate

while true; do
    # Check response time
    AVG_RESPONSE_TIME=$(curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | jq '.measurements[0].value')
    
    # Check error rate  
    ERROR_RATE=$(curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | jq '.measurements[1].value')
    
    if (( $(echo "$AVG_RESPONSE_TIME > $RESPONSE_TIME_THRESHOLD" | bc -l) )); then
        echo "High response time detected: ${AVG_RESPONSE_TIME}ms"
        # Trigger rollback
        ./rollback-to-legacy.sh
        break
    fi
    
    sleep 60
done
```

### 2. Data Integrity Issues

#### Data Validation Scripts
```sql
-- data-integrity-check.sql
-- Validate data consistency between versions

-- Check customer data consistency
SELECT 'Customer count mismatch' as issue, 
       COUNT(*) as legacy_count 
FROM customer 
WHERE created_date < '2024-01-01'  -- Pre-migration data
HAVING COUNT(*) != (
    SELECT COUNT(*) FROM customer_audit 
    WHERE audit_type = 'LEGACY_IMPORT'
);

-- Verify order totals calculation
SELECT o.id, o.total, 
       SUM(ol.quantity * ol.unit_price) as calculated_total
FROM orders o 
JOIN order_line ol ON o.id = ol.order_id
HAVING o.total != calculated_total;
```

#### Data Recovery Procedures
```bash
#!/bin/bash
# data-recovery.sh

# Backup current data
pg_dump -h localhost -U petstore petstore_db > backup_pre_recovery.sql

# Restore from pre-migration backup
pg_dump -h localhost -U petstore petstore_db < backup_pre_migration.sql

# Replay transactions from audit log
psql -h localhost -U petstore -d petstore_db -f replay_transactions.sql
```

### 3. Integration Failure Response

#### Circuit Breaker for Legacy Integration
```java
@Component
public class LegacyIntegrationService {
    
    @CircuitBreaker(name = "legacy-integration", fallbackMethod = "fallbackToLegacy")
    @TimeLimiter(name = "legacy-integration")
    public CompletableFuture<String> callLegacyService(String request) {
        return CompletableFuture.supplyAsync(() -> {
            // Call to legacy Java EE service
            return legacyServiceClient.processRequest(request);
        });
    }
    
    public CompletableFuture<String> fallbackToLegacy(String request, Exception ex) {
        // Fallback to direct database access or cached data
        return CompletableFuture.completedFuture(getCachedResult(request));
    }
}
```

## Risk Mitigation Checklist

### Pre-Migration Validation
- [ ] **Complete backup** of production database and application
- [ ] **Performance baseline** established for all critical operations
- [ ] **Feature inventory** documented with acceptance criteria
- [ ] **Integration test suite** covering all business workflows
- [ ] **Rollback procedures** tested in staging environment
- [ ] **Monitoring and alerting** configured for migration metrics

### During Migration
- [ ] **Feature flags** implemented for gradual rollout
- [ ] **A/B testing** setup for comparing legacy vs new implementation
- [ ] **Real-time monitoring** of performance and error rates
- [ ] **Automated rollback triggers** based on performance thresholds
- [ ] **Manual rollback procedures** documented and accessible
- [ ] **Communication plan** for stakeholders during issues

### Post-Migration Validation
- [ ] **Smoke tests** for all critical user journeys
- [ ] **Performance comparison** with pre-migration baseline
- [ ] **Data integrity validation** across all entities
- [ ] **Integration testing** with external systems
- [ ] **Security testing** for authentication and authorization
- [ ] **Load testing** to validate scalability

## Communication Plan

### Stakeholder Matrix
| Stakeholder | Information Needed | Communication Method | Frequency |
|-------------|-------------------|---------------------|-----------|
| Business Users | Feature availability, downtime | Email, Dashboard | Weekly |
| DevOps Team | Deployment status, rollback triggers | Slack, Alerts | Real-time |
| Support Team | Known issues, troubleshooting | Wiki, Training | Before go-live |
| Management | Progress, risks, timeline | Reports, Meetings | Bi-weekly |

### Escalation Procedures
```
Level 1: Development Team (0-2 hours)
├── Performance degradation > 50%
├── Error rate > 2%
└── Failed integration tests

Level 2: Technical Lead + DevOps (2-4 hours)  
├── Performance degradation > 100%
├── Error rate > 5%
└── Data integrity issues

Level 3: Management + Rollback Decision (4+ hours)
├── System unavailable
├── Data corruption
└── Security breaches
```

## Success Criteria & Go/No-Go Decision Points

### Go-Live Criteria
- [ ] All integration tests pass with 100% success rate
- [ ] Performance within 10% of legacy system baseline
- [ ] Error rate < 0.1% in staging environment
- [ ] All critical business workflows validated
- [ ] Rollback procedures tested and verified
- [ ] Monitoring and alerting fully functional
- [ ] Support team trained and prepared

### No-Go Criteria (Mandatory Rollback)
- ❌ Performance degradation > 50% of baseline
- ❌ Error rate > 1% for critical operations
- ❌ Data integrity issues detected
- ❌ Security vulnerabilities identified
- ❌ Rollback procedures not working
- ❌ Critical business functionality missing

## Lessons Learned Documentation

### Post-Migration Review Template
```markdown
# Migration Review: Java EE 6 to Spring Boot 3.x

## What Went Well
- [ ] Successful areas of migration
- [ ] Effective risk mitigation strategies
- [ ] Performance improvements achieved

## What Could Be Improved  
- [ ] Unexpected challenges encountered
- [ ] Risk mitigation gaps identified
- [ ] Process improvements for future migrations

## Action Items
- [ ] Technical debt to address
- [ ] Documentation updates needed
- [ ] Process improvements to implement
```

## Next Steps

1. **Finalize rollback procedures** and test in staging
2. **Implement monitoring and alerting** for migration metrics
3. **Prepare communication templates** for all stakeholders
4. **Conduct final risk assessment** with all teams
5. **Schedule migration windows** with appropriate rollback buffers
6. **Create final migration plan** linking all documentation