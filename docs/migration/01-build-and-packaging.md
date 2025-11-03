# Build & Packaging Migration

## Overview
This document outlines the migration from Java EE 6 WAR packaging with application server dependencies to Spring Boot 3.x JAR packaging with embedded server.

## Current State Analysis

### Existing POM Structure
```xml
<groupId>org.agoncal.application</groupId>
<artifactId>petstoreee6</artifactId>
<packaging>war</packaging>
<version>1.0</version>

<properties>
    <version.java>1.6</version.java>
    <version.javaee>6.0</version.javaee>
</properties>
```

**Dependencies:** GlassFish/JBoss embedded containers, JavaEE API, JSF/MyFaces, Derby, Arquillian

## Target State

### New Parent Configuration
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
</parent>

<groupId>org.agoncal.application</groupId>
<artifactId>petstore-springboot</artifactId>
<packaging>jar</packaging>
<version>2.0.0</version>
<name>Petstore Application - Spring Boot</name>
<description>Migrated petstore application using Spring Boot 3.x</description>
```

### Java Version Upgrade
```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### Spring Boot Starters Dependencies
```xml
<dependencies>
    <!-- Core Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Data & Persistence -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Validation (JSR-303) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Thymeleaf Template Engine (JSF replacement) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    
    <!-- Production Monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Security (future enhancement) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.apache.derby</groupId>
        <artifactId>derby</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.derby</groupId>
        <artifactId>derbytools</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Build Plugin Configuration
```xml
<build>
    <finalName>petstore-springboot</finalName>
    <plugins>
        <!-- Spring Boot Maven Plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Maven Compiler Plugin -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <parameters>true</parameters>
            </configuration>
        </plugin>
        
        <!-- Surefire for Unit Tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.1.2</version>
        </plugin>
        
        <!-- Failsafe for Integration Tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.1.2</version>
        </plugin>
    </plugins>
</build>
```

## Dependencies Removed
- `glassfish-embedded-all` (provided)
- `javaee-api` (provided)  
- `arquillian-*` containers
- `myfaces-*` JSF implementations
- `jrebel-maven-plugin`
- `maven-embedded-glassfish-plugin`
- `tomee-maven-plugin`

## WAR vs JAR Decision

### JAR Packaging (Recommended)
- **Pros:** 
  - Embedded server (Tomcat)
  - Simpler deployment (`java -jar`)
  - Cloud-native friendly
  - Faster startup
- **Cons:** 
  - Traditional app server features lost

### WAR Option (If Required)
```xml
<packaging>war</packaging>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

## Build Commands

### Development
```bash
mvn spring-boot:run
```

### Production Build
```bash
mvn clean package
java -jar target/petstore-springboot.jar
```

### Docker Build
```bash
mvn spring-boot:build-image
```

## Profile Configuration
```xml
<profiles>
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>dev</spring.profiles.active>
        </properties>
    </profile>
    
    <profile>
        <id>test</id>
        <properties>
            <spring.profiles.active>test</spring.profiles.active>
        </properties>
    </profile>
    
    <profile>
        <id>prod</id>
        <properties>
            <spring.profiles.active>prod</spring.profiles.active>
        </properties>
    </profile>
</profiles>
```

## Migration Rationale

1. **Simplified Dependency Management**: Spring Boot BOM eliminates version conflicts
2. **Embedded Server**: No external application server required
3. **Auto-Configuration**: Reduces boilerplate configuration
4. **Modern Java**: Java 17 enables records, sealed classes, pattern matching
5. **Production Ready**: Built-in metrics, health checks, and monitoring via Actuator

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Java 6→17 compatibility | High | Incremental testing, use compatibility tools |
| WAR→JAR deployment change | Medium | Document new deployment procedures |
| Dependency conflicts | Medium | Use Spring Boot's dependency management |
| Build time increase | Low | Use Maven daemon, incremental compilation |

## Next Steps

1. Create new `pom.xml` with Spring Boot parent
2. Update Maven wrapper to latest version
3. Test build with `mvn clean compile`
4. Verify Java 17 compatibility
5. Proceed to application bootstrapping migration