package id.co.bankbsi.coinsight.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class CleanJsonResponseAdvice implements ResponseBodyAdvice<Object> {

  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    // Skip Spring Boot internal classes that don't support deserialization
    String className = returnType.getParameterType().getName();
    if (className.startsWith("org.springframework.boot.actuator") ||
        className.startsWith("org.springframework.boot.autoconfigure") ||
        className.contains("Health")) {
      return false;
    }

    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body == null) {
      return null;
    }

    // Skip processing for Spring Boot internal classes
    String className = body.getClass().getName();
    if (className.startsWith("org.springframework.boot.actuator") ||
        className.startsWith("org.springframework.boot.autoconfigure") ||
        className.contains("Health") ||
        className.contains("CompositeHealth")) {
      return body;
    }

    // Skip processing for actuator endpoints
    String requestPath = request.getURI().getPath();
    if (requestPath.startsWith("/actuator")) {
      return body;
    }

    try {
      // Convert the object to clean JSON and back to remove any Redis serialization
      // artifacts
      // This is a bit of a hack but works reliably
      String json = objectMapper.writeValueAsString(body);
      return objectMapper.readValue(json, body.getClass());
    } catch (Exception e) {
      log.warn("Failed to clean JSON response for {}: {}", body.getClass().getSimpleName(), e.getMessage());
      return body;
    }
  }
}
