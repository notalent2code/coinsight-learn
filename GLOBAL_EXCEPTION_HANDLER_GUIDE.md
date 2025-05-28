# Global Exception Handler Architecture in Coinsight Microservices

## Overview

The Coinsight microservices application implements a sophisticated multi-layered exception handling architecture that provides consistent error responses across all services while allowing for service-specific customizations. This document explains how the Global Exception Handler works and the patterns used throughout the application.

## Architecture Components

### 1. Common Global Exception Handler
**Location**: `/common/src/main/java/id/co/bankbsi/coinsight/common/exception/GlobalExceptionHandler.java`

This serves as the base exception handler that provides standardized error handling for common scenarios:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        // Handles input validation errors
        // Returns detailed field-level validation errors
    }
    
    @ExceptionHandler(ResourceNotFoundException.class) 
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        // Handles 404 scenarios when resources are not found
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        // Handles business logic violations
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        // Catch-all handler for unexpected exceptions
    }
}
```

### 2. Service-Specific Exception Handlers

Each microservice has its own GlobalExceptionHandler that extends or complements the common handler:

#### Transaction Service Handler
**Location**: `/transaction-service/src/main/java/id/co/bankbsi/coinsight/transaction/exception/GlobalExceptionHandler.java`

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Object> handleCategoryNotFoundException(CategoryNotFoundException ex) {
        // Handles transaction category not found scenarios
    }
    
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Object> handleTransactionNotFoundException(TransactionNotFoundException ex) {
        // Handles transaction not found scenarios
    }
}
```

#### Budget Service Handler
**Location**: `/budget-service/src/main/java/id/co/bankbsi/coinsight/budget/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BudgetNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBudgetNotFound(BudgetNotFoundException ex) {
        // Handles budget not found scenarios
    }
    
    @ExceptionHandler(DuplicateBudgetException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateBudget(DuplicateBudgetException ex) {
        // Handles duplicate budget creation attempts
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        // Handles invalid argument scenarios
    }
}
```

#### Auth Service Handler
**Location**: `/auth-service/src/main/java/id/co/bankbsi/coinsight/auth/exception/GlobalExceptionHandler.java`

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Object> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        // Handles duplicate email registration attempts
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFoundException(UserNotFoundException ex) {
        // Handles user not found scenarios
    }
}
```

#### Gateway Service Handler
**Location**: `/gateway-service/handler/RateLimitExceptionHandler.java`

```java
@Component
@Order(-1)
@Slf4j
public class RateLimitExceptionHandler implements ErrorWebExceptionHandler {
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof RedisRateLimiter.RedisRateLimiterException) {
            return handleRateLimitException(exchange);
        }
        return Mono.error(ex);
    }
}
```

### 3. Standardized Error Response

All exception handlers use a common `ErrorResponse` structure for consistency:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}
```

## Exception Handling Flow

### 1. Request Processing Flow

```
Client Request
     ↓
Gateway Service (Rate Limiting Check)
     ↓
Service-Specific Controller
     ↓
Business Logic Layer
     ↓
Exception Occurs
     ↓
Service-Specific Exception Handler (if exists)
     ↓
Common Exception Handler (if not handled)
     ↓
Standardized Error Response
     ↓
Client
```

### 2. Exception Hierarchy

The application uses a hierarchical approach to exception handling:

1. **Gateway Level**: Rate limiting and routing errors
2. **Service Level**: Business-specific exceptions (e.g., `BudgetNotFoundException`)
3. **Common Level**: Standard exceptions (validation, generic errors)
4. **Framework Level**: Spring framework exceptions

### 3. Exception Handler Priority

Exception handlers are processed in the following order:

1. **Most Specific**: Exact exception type matches in service-specific handlers
2. **Service Generic**: Catch-all handlers in service-specific handlers
3. **Common Specific**: Specific exception types in common handler
4. **Common Generic**: Catch-all handler in common module

## Key Features

### 1. Consistent Error Structure

All services return errors in the same format:
```json
{
    "timestamp": "2025-05-27T10:30:00",
    "status": 404,
    "error": "Resource Not Found",
    "message": "Budget with ID 123 not found",
    "path": "/api/budgets/123",
    "validationErrors": null
}
```

### 2. Validation Error Handling

Input validation errors include field-specific details:
```json
{
    "timestamp": "2025-05-27T10:30:00",
    "status": 400,
    "error": "Validation Failed",
    "message": "Input validation failed",
    "path": "/api/transactions",
    "validationErrors": {
        "amount": "Amount must be positive",
        "categoryId": "Category ID is required"
    }
}
```

### 3. Logging Integration

All exception handlers include comprehensive logging:
- Error level for unexpected exceptions
- Warn level for business rule violations
- Info level for validation errors

### 4. Security Considerations

Exception handlers avoid exposing sensitive information:
- No stack traces in production responses
- Generic error messages for security-sensitive operations
- Detailed logging for debugging while keeping responses clean

## Service-Specific Customizations

### Transaction Service
- Handles OCR processing errors
- Redis caching exception handling
- Transaction category validation

### Budget Service
- Budget lifecycle management errors
- Kafka event processing failures
- Budget threshold violations

### Auth Service
- Keycloak integration errors
- JWT token validation failures
- User management exceptions

### Gateway Service
- Rate limiting violations
- Circuit breaker failures
- Routing and load balancing errors

### Notification Service
- Email sending failures
- Message processing errors
- Template rendering issues

## Best Practices Implemented

### 1. Separation of Concerns
- Common exceptions in shared module
- Service-specific exceptions in respective services
- Gateway-specific reactive error handling

### 2. Error Response Consistency
- Standardized `ErrorResponse` class
- Consistent HTTP status codes
- Uniform timestamp and path inclusion

### 3. Comprehensive Logging
- Structured logging with correlation IDs
- Different log levels for different error types
- Security-conscious error message handling

### 4. Graceful Degradation
- Fallback error responses
- Circuit breaker integration
- Rate limiting with informative messages

## Monitoring and Observability

The exception handling architecture integrates with:

1. **Prometheus Metrics**: Exception counters and rates
2. **Distributed Tracing**: Error correlation across services
3. **Centralized Logging**: Aggregated error analysis
4. **Health Checks**: Service availability monitoring

## Future Enhancements

### Potential Improvements

1. **Exception Aggregation**: Collect multiple validation errors
2. **Internationalization**: Multi-language error messages
3. **Error Codes**: Standardized error code system
4. **Circuit Breaking**: Enhanced failure detection
5. **Retry Logic**: Automatic retry for transient failures

## Conclusion

The Coinsight microservices application implements a robust, multi-layered exception handling architecture that provides:

- **Consistency**: Uniform error responses across all services
- **Flexibility**: Service-specific customizations when needed
- **Observability**: Comprehensive logging and monitoring
- **Security**: Safe error message handling
- **Maintainability**: Clear separation of concerns

This architecture ensures that clients receive predictable, informative error responses while maintaining system security and providing developers with the information needed for debugging and monitoring.
