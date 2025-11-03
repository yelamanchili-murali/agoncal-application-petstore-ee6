# Observability Migration

## Overview
Migration from Java EE application server monitoring to Spring Boot Actuator-based observability with modern monitoring, metrics, and health checking capabilities.

## Current State (Java EE)

### Limited Observability
- **Health Checking**: Application server health endpoints
- **Metrics**: JMX MBeans via application server
- **Logging**: Java Util Logging or Log4j
- **Monitoring**: Application server management consoles
- **Tracing**: Manual logging, no distributed tracing

### Java EE Monitoring Points
```java
// Current logging approach
@Loggable
public class CatalogService {
    // Method execution logging via interceptor
}

// JMX exposure (if any)
@ManagedBean
public class PetstoreStatistics {
    // Manual JMX bean registration
}
```

## Target State (Spring Boot Actuator)

### Comprehensive Observability Stack
- **Health Checks**: Actuator health indicators
- **Metrics**: Micrometer metrics with multiple backends
- **Application Info**: Build info, Git info, environment details
- **Logging**: Logback with structured logging
- **HTTP Endpoints**: RESTful monitoring endpoints
- **Future**: Distributed tracing with OpenTelemetry

## Spring Boot Actuator Configuration

### 1. Dependencies
```xml
<dependencies>
    <!-- Core Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Metrics Registry (choose one) -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- Optional: Detailed metrics -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
    </dependency>
</dependencies>
```

### 2. Basic Actuator Configuration

#### application.yml
```yaml
# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,loggers
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
    info:
      enabled: true
    metrics:
      enabled: true
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
    ping:
      enabled: true
  info:
    build:
      enabled: true
    git:
      enabled: true
    env:
      enabled: true
  metrics:
    tags:
      application: petstore-springboot
      environment: ${spring.profiles.active}
    export:
      prometheus:
        enabled: true
      simple:
        enabled: false

# Security for actuator endpoints
spring:
  security:
    user:
      name: actuator
      password: ${ACTUATOR_PASSWORD:monitor123}
      roles: ACTUATOR

# Info endpoint content
info:
  app:
    name: Petstore Spring Boot
    description: Migrated Java EE Petstore application
    version: '@project.version@'
    encoding: '@project.build.sourceEncoding@'
    java:
      version: '@java.version@'
  build:
    artifact: '@project.artifactId@'
    name: '@project.name@'
    time: '@maven.build.timestamp@'
    version: '@project.version@'
```

### 3. Profile-Specific Actuator Configuration

#### application-dev.yml
```yaml
# Development - All endpoints exposed
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  security:
    enabled: false  # No security in development
    
logging:
  level:
    org.springframework.boot.actuate: DEBUG
```

#### application-prod.yml
```yaml
# Production - Limited endpoint exposure
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: never
  security:
    enabled: true
  server:
    port: 9090  # Separate port for management
  
# Secure actuator endpoints
spring:
  security:
    user:
      name: ${ACTUATOR_USER:admin}
      password: ${ACTUATOR_PASSWORD}
```

## Health Indicators

### 1. Built-in Health Indicators
```yaml
management:
  health:
    # Database health check
    db:
      enabled: true
    # Disk space health check  
    diskspace:
      enabled: true
      threshold: 100MB
    # Custom health indicators
    custom:
      enabled: true
```

### 2. Custom Health Indicators
```java
package org.agoncal.application.petstore.health;

import org.agoncal.application.petstore.service.CatalogService;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CatalogHealthIndicator implements HealthIndicator {

    private final CatalogService catalogService;

    public CatalogHealthIndicator(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public Health health() {
        try {
            long categoryCount = catalogService.countCategories();
            long productCount = catalogService.countProducts();
            long itemCount = catalogService.countItems();
            
            if (categoryCount == 0) {
                return Health.down()
                    .withDetail("reason", "No categories found in catalog")
                    .withDetail("categoryCount", categoryCount)
                    .build();
            }
            
            return Health.up()
                .withDetail("categoryCount", categoryCount)
                .withDetail("productCount", productCount) 
                .withDetail("itemCount", itemCount)
                .withDetail("catalogStatus", "operational")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}

@Component
public class OrderProcessingHealthIndicator implements HealthIndicator {

    private final OrderService orderService;

    @Override
    public Health health() {
        try {
            // Check recent order processing
            long recentOrders = orderService.countRecentOrders(Duration.ofMinutes(5));
            double avgProcessingTime = orderService.getAverageProcessingTime();
            
            Health.Builder status = avgProcessingTime < 2.0 ? 
                Health.up() : Health.outOfService();
                
            return status
                .withDetail("recentOrders", recentOrders)
                .withDetail("avgProcessingTimeSeconds", avgProcessingTime)
                .withDetail("threshold", "2.0 seconds")
                .build();
                
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
```

### 3. External Service Health Checks
```java
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    
    private final RestTemplate restTemplate;
    
    @Override
    public Health health() {
        try {
            // Check external payment service
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://payment-service/health", String.class);
                
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("paymentService", "available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("paymentService", "unavailable")
                    .withDetail("status", response.getStatusCode())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("paymentService", "error")
                .withException(e)
                .build();
        }
    }
}
```

## Metrics Implementation

### 1. Custom Application Metrics
```java
package org.agoncal.application.petstore.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PetstoreMetrics {

    private final Counter orderCreatedCounter;
    private final Counter loginAttemptCounter; 
    private final Counter loginSuccessCounter;
    private final Timer orderProcessingTimer;
    private final Gauge activeSessionsGauge;

    public PetstoreMetrics(MeterRegistry meterRegistry) {
        // Order metrics
        this.orderCreatedCounter = Counter.builder("petstore.orders.created")
            .description("Total number of orders created")
            .tag("application", "petstore")
            .register(meterRegistry);
            
        this.orderProcessingTimer = Timer.builder("petstore.orders.processing.time")
            .description("Time taken to process orders")
            .register(meterRegistry);

        // Authentication metrics
        this.loginAttemptCounter = Counter.builder("petstore.auth.attempts")
            .description("Total login attempts")
            .register(meterRegistry);
            
        this.loginSuccessCounter = Counter.builder("petstore.auth.success")
            .description("Successful login attempts")
            .register(meterRegistry);

        // Session metrics
        this.activeSessionsGauge = Gauge.builder("petstore.sessions.active")
            .description("Number of active user sessions")
            .register(meterRegistry, this, PetstoreMetrics::getActiveSessions);
    }

    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }

    public void recordOrderProcessingTime(Duration processingTime) {
        orderProcessingTimer.record(processingTime);
    }

    public void recordLoginAttempt(boolean successful) {
        loginAttemptCounter.increment();
        if (successful) {
            loginSuccessCounter.increment();
        }
    }

    private double getActiveSessions() {
        // Implementation to count active sessions
        return SessionRegistry.getActiveSessionCount();
    }
}
```

### 2. Service-Level Metrics
```java
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final PetstoreMetrics metrics;
    private final MeterRegistry meterRegistry;

    public CatalogService(EntityManager entityManager, 
                         PetstoreMetrics metrics,
                         MeterRegistry meterRegistry) {
        this.entityManager = entityManager;
        this.metrics = metrics;
        this.meterRegistry = meterRegistry;
    }

    @Timed(value = "petstore.catalog.search", description = "Time taken to search catalog")
    public List<Item> searchItems(String keyword) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Item> results = performSearch(keyword);
            
            // Record search metrics
            meterRegistry.counter("petstore.catalog.searches", 
                "keyword", keyword.length() > 10 ? "long" : "short",
                "resultCount", String.valueOf(results.size()))
                .increment();
                
            return results;
        } finally {
            sample.stop(Timer.builder("petstore.catalog.search.time").register(meterRegistry));
        }
    }

    @EventListener
    public void handleProductViewed(ProductViewedEvent event) {
        meterRegistry.counter("petstore.catalog.product.views",
            "category", event.getProduct().getCategory().getName(),
            "productId", event.getProduct().getId().toString())
            .increment();
    }
}
```

### 3. HTTP Request Metrics (Automatic)
Spring Boot Actuator automatically provides HTTP request metrics:
- Request count and timing
- Status code distributions  
- URI patterns
- Exception counts

## Application Information

### 1. Build Information
```xml
<!-- Add to pom.xml -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>build-info</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 2. Git Information
```xml
<!-- Add to pom.xml -->
<plugin>
    <groupId>pl.project13.maven</groupId>
    <artifactId>git-commit-id-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>revision</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 3. Custom Info Contributor
```java
@Component
public class PetstoreInfoContributor implements InfoContributor {

    private final CatalogService catalogService;
    private final CustomerService customerService;

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> details = new HashMap<>();
        
        // Business metrics for info endpoint
        details.put("totalCategories", catalogService.countCategories());
        details.put("totalProducts", catalogService.countProducts());
        details.put("totalCustomers", customerService.countCustomers());
        details.put("lastUpdated", Instant.now());
        
        builder.withDetail("business", details);
        
        // System information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> system = new HashMap<>();
        system.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        system.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        system.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        system.put("availableProcessors", runtime.availableProcessors());
        
        builder.withDetail("system", system);
    }
}
```

## Logging Configuration

### 1. Logback Configuration (logback-spring.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- Console appender for development -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <!-- File appender for production -->
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/petstore/application.log</file>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/petstore/application.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
                <totalSizeCap>1GB</totalSizeCap>
            </rollingPolicy>
        </appender>
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

    <!-- Application-specific loggers -->
    <logger name="org.agoncal.application.petstore" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    
</configuration>
```

### 2. Structured Logging
```java
@Service
@Slf4j
public class OrderService {

    public Order createOrder(Order order, Customer customer) {
        // Structured logging with MDC
        MDC.put("customerId", customer.getId().toString());
        MDC.put("orderValue", order.getTotal().toString());
        
        try {
            log.info("Processing order creation for customer: {}", customer.getLogin());
            
            Order savedOrder = processOrder(order, customer);
            
            log.info("Order created successfully - orderId: {}, total: {}", 
                savedOrder.getId(), savedOrder.getTotal());
                
            return savedOrder;
        } catch (Exception e) {
            log.error("Failed to create order for customer: {}", customer.getLogin(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

## Actuator Endpoints Overview

### Core Endpoints (Always Available)
| Endpoint | Purpose | Example URL |
|----------|---------|-------------|
| `/actuator/health` | Application health status | `GET /actuator/health` |
| `/actuator/info` | Application information | `GET /actuator/info` |

### Optional Endpoints (Configure as needed)
| Endpoint | Purpose | Production Use |
|----------|---------|----------------|
| `/actuator/metrics` | Micrometer metrics | ✅ Yes |
| `/actuator/prometheus` | Prometheus format metrics | ✅ Yes |
| `/actuator/env` | Environment properties | ⚠️ Secure only |
| `/actuator/loggers` | Runtime logger configuration | ⚠️ Secure only |
| `/actuator/httptrace` | HTTP request traces | ❌ Dev only |
| `/actuator/heapdump` | JVM heap dump | ❌ Dev only |
| `/actuator/threaddump` | JVM thread dump | ❌ Dev only |

## Security Configuration

### Actuator Security
```java
@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(requests ->
                requests
                    .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                    .requestMatchers(EndpointRequest.to("metrics", "prometheus")).hasRole("MONITOR")
                    .anyRequest().hasRole("ADMIN")
            )
            .httpBasic(withDefaults())
            .build();
    }
}
```

## Monitoring Integration

### 1. Prometheus Configuration
```yaml
# prometheus.yml (external Prometheus configuration)
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'petstore'
    static_configs:
      - targets: ['petstore-app:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
```

### 2. Grafana Dashboard Queries
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Response time 95th percentile
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Order creation rate
rate(petstore_orders_created_total[5m])

# Database connection pool usage
hikaricp_connections_active / hikaricp_connections_max
```

## Future Enhancements (Phase 2)

### 1. Distributed Tracing with OpenTelemetry
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
```

### 2. APM Integration
```yaml
# Application Performance Monitoring
spring:
  application:
    name: petstore-springboot
  sleuth:
    zipkin:
      base-url: http://zipkin:9411
    sampler:
      probability: 0.1  # Sample 10% of traces
```

### 3. Custom Dashboards
- Business KPI dashboard
- Technical metrics dashboard
- Error tracking dashboard
- Performance monitoring dashboard

## Alerting Strategy

### Health-Based Alerts
```yaml
# Alert rules (for Prometheus AlertManager)
groups:
  - name: petstore
    rules:
      - alert: PetstoreDown
        expr: up{job="petstore"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Petstore application is down"
          
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
```

## Testing Observability

### Health Endpoint Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorHealthTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointShouldReturnUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void customHealthIndicatorShouldBeIncluded() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);
        Map<String, Object> health = response.getBody();
        
        assertThat(health).containsKey("components");
        Map<String, Object> components = (Map<String, Object>) health.get("components");
        assertThat(components).containsKey("catalog");
    }
}
```

## Migration Checklist

- [ ] Add Spring Boot Actuator dependency
- [ ] Configure actuator endpoints in application.yml
- [ ] Create custom health indicators for business logic
- [ ] Implement custom metrics with Micrometer
- [ ] Configure structured logging with Logback
- [ ] Set up build and Git information
- [ ] Create custom info contributors
- [ ] Secure actuator endpoints appropriately
- [ ] Test health and metrics endpoints
- [ ] Document monitoring runbook

## Deployment Considerations

### Docker Health Checks
```dockerfile
FROM openjdk:17-jre-slim
COPY target/petstore-springboot.jar app.jar

# Health check using actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes Probes
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: petstore
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

## Next Steps

1. Implement basic health and info endpoints
2. Add custom business metrics
3. Configure structured logging
4. Set up Prometheus metrics export
5. Create monitoring dashboards
6. Proceed to test migration strategy