# spring-audit-aop-interceptor

Lightweight AOP-based auditing library for Spring and Spring Boot 3+.  
Provides a simple way to intercept method or class executions annotated with `@Auditable`
and delegate audit events (`before`, `afterReturning`, `afterThrowing`) to user-defined handlers.

---

## Features

- Annotation-driven (`@Auditable`) — works on methods or entire classes
- Supports multiple audit handlers per method
- Fail-safe execution (audit errors never affect business logic)
- Structured logging via Lombok `@Slf4j`
- Seamless integration with Spring Boot

---

## Installation

The library is published to **GitHub Packages**.

### 1. Configure the repository

#### Gradle
```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/serzhe1/spring-audit-aop-interceptor")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key")  ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```
Maven
```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/serzhe1/spring-audit-aop-interceptor</url>
  </repository>
</repositories>
```

### 2. Add the dependency

Gradle
```groovy
dependencies {
    implementation "org:spring-audit-aop-interceptor:1.0.0"
}
```

Maven

```xml
<dependency>
  <groupId>org</groupId>
  <artifactId>spring-audit-aop-interceptor</artifactId>
  <version>1.0.0</version>
</dependency>
```


⸻

### Usage

#### 1. Define your audit handlers

Each handler implements BaseAuditService and is registered as a Spring bean.

```java
@Component("loggerAudit")
public class LoggerAuditService implements BaseAuditService {
    @Override public void before(JoinPoint jp) {
        log.info("Before: {}", jp.getSignature());
    }

    @Override public void afterReturning(JoinPoint jp, Object ret) {
        log.info("After success: {}", jp.getSignature());
    }

    @Override public void afterThrowing(JoinPoint jp, Throwable ex) {
        log.warn("After failure: {} -> {}", jp.getSignature(), ex.getMessage());
    }
}
```

#### 2. Annotate services or methods

You can annotate an entire class or a specific method.
If both are annotated, the method-level annotation has higher priority.
If both specify the same audit handler names, that handler will be executed only once.

```java
@Auditable(handlers = {"loggerAudit"})
@Service
public class UserService {

    // Uses handler(s) defined at class level
    public void createUser(String name) {
        log.info("Creating user: {}", name);
    }

    // Overrides the class-level annotation
    @Auditable(handlers = {"loggerAudit", "dbAudit"})
    public void deleteUser(String id) {
        throw new IllegalStateException("Cannot delete user " + id);
    }
}
```

#### 3. Execution flow

When UserService.createUser() or deleteUser() is called:

BEFORE          -> loggerAudit.before()

AFTER_RETURNING -> loggerAudit.afterReturning()

AFTER_THROWING  -> loggerAudit.afterThrowing()

Handler exceptions are caught and logged; they never interrupt business logic.

⸻

### Requirements

Java	17+ (tested on 21)

Spring Boot	3+

License	MIT
