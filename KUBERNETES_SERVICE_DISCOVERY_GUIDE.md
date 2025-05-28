# Kubernetes Service Discovery Migration Guide

## Why Remove Eureka for Kubernetes?

### Kubernetes Native Advantages:
- **Built-in Service Discovery**: DNS-based service resolution
- **Health Checks**: Automatic pod health monitoring
- **Load Balancing**: Built-in load balancing across replicas
- **Security**: Network policies and RBAC
- **Observability**: Built-in metrics and logging
- **Zero Infrastructure**: No additional service discovery servers needed

### Eureka Disadvantages in Kubernetes:
- **Redundant**: Duplicates Kubernetes functionality
- **Additional Complexity**: More moving parts to maintain
- **Resource Overhead**: Extra pods and memory usage
- **Network Issues**: Can conflict with Kubernetes networking
- **Split Brain**: Two sources of truth for service locations

## Migration Steps

### 1. Remove Eureka Dependencies

```xml
<!-- REMOVE from all service pom.xml files -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 2. Update Service Configuration

```yaml
# NEW: Use Kubernetes service names in application.yml
spring:
  application:
    name: transaction-service
  # Remove eureka configuration
  datasource:
    url: jdbc:postgresql://transaction-db:5432/transaction_service

# Service-to-service communication
services:
  auth-service: http://auth-service:8081
  budget-service: http://budget-service:8083
  notification-service: http://notification-service:8085
```

### 3. Kubernetes Service Definitions

```yaml
# transaction-service-k8s.yaml
apiVersion: v1
kind: Service
metadata:
  name: transaction-service
  labels:
    app: transaction-service
spec:
  selector:
    app: transaction-service
  ports:
    - name: http
      port: 8082
      targetPort: 8082
  type: ClusterIP

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transaction-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: transaction-service
  template:
    metadata:
      labels:
        app: transaction-service
    spec:
      containers:
      - name: transaction-service
        image: coinsight/transaction-service:latest
        ports:
        - containerPort: 8082
        env:
        - name: DB_HOST
          value: "transaction-db"
        - name: KAFKA_SERVERS
          value: "kafka:9092"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 5
          periodSeconds: 5
```

### 4. Update Service Communication Code

```java
// OLD: Using Eureka with @LoadBalanced
@LoadBalanced
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

// NEW: Use Kubernetes service names directly
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("http://auth-service:8081") // Kubernetes service name
            .build();
    }
}
```

### 5. Gateway Service Updates

```yaml
# gateway-service configuration for Kubernetes
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:8081  # Kubernetes service name
          predicates:
            - Path=/api/auth/**
        - id: transaction-service
          uri: http://transaction-service:8082
          predicates:
            - Path=/api/transactions/**
        - id: budget-service
          uri: http://budget-service:8083
          predicates:
            - Path=/api/budgets/**
```

## Alternative Service Discovery Solutions

### If You Need Advanced Service Discovery Features:

#### 1. **Consul** (Better than Eureka)
```yaml
# Consul advantages over Eureka:
- Better health checking
- Service mesh capabilities (Consul Connect)
- Key-value store
- Multi-datacenter support
- Better security (ACLs, encryption)
```

#### 2. **Istio Service Mesh**
```yaml
# For advanced microservices patterns:
- Automatic service discovery
- Traffic management
- Security policies
- Observability
- Circuit breakers
- Retries and timeouts
```

#### 3. **Linkerd** (Lightweight Service Mesh)
```yaml
# Simpler than Istio:
- Automatic service discovery
- Load balancing
- Retries and timeouts
- mTLS encryption
- Observability
```

## Performance Comparison

### Kubernetes Native:
- **Latency**: ~1ms (DNS lookup)
- **Memory**: 0 additional overhead
- **CPU**: 0 additional overhead
- **Reliability**: 99.9%+ (built into platform)

### Eureka:
- **Latency**: ~10-50ms (registry lookup + network)
- **Memory**: ~512MB per Eureka server
- **CPU**: ~200m per server
- **Reliability**: Depends on Eureka cluster health

### Consul:
- **Latency**: ~5-20ms (agent lookup)
- **Memory**: ~256MB per agent
- **CPU**: ~100m per agent
- **Reliability**: 99.9%+ with proper clustering

## Recommended Architecture for Kubernetes

```yaml
# Recommended stack:
✅ Kubernetes Native Service Discovery
✅ Ingress Controller (NGINX/Traefik)
✅ Config Server (Spring Cloud Config)
✅ Circuit Breakers (Resilience4j)
✅ Distributed Tracing (Jaeger/Zipkin)
✅ Monitoring (Prometheus + Grafana)

❌ Eureka Server (redundant in K8s)
❌ Client-side load balancing (K8s handles this)
❌ Service registry maintenance
```

## Migration Checklist

- [ ] Remove Eureka server from docker-compose.yml
- [ ] Remove Eureka dependencies from all services
- [ ] Update service URLs to use Kubernetes service names
- [ ] Create Kubernetes Service definitions for all services
- [ ] Update Gateway routing to use service names
- [ ] Remove @LoadBalanced annotations
- [ ] Test service-to-service communication
- [ ] Update monitoring and health checks
- [ ] Remove Eureka configuration from all services

## Conclusion

For Kubernetes deployments:
- **Use Kubernetes native service discovery**
- **Remove Eureka completely**
- **Optionally add Istio/Linkerd for advanced features**
- **Keep it simple and leverage platform capabilities**

This approach reduces complexity, improves reliability, and leverages Kubernetes' built-in capabilities.
