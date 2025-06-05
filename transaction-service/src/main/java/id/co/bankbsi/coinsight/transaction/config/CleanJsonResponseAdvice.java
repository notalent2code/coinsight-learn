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
    // Avoid advice on actuator/health-related classes
    String className = returnType.getParameterType().getName();
    return !className.startsWith("org.springframework.boot.actuate")
        && !className.startsWith("org.springframework.boot.autoconfigure")
        && !className.contains("Health");
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {

    if (body == null) return null;

    // 💡 Skip actuator endpoints entirely
    String path = request.getURI().getPath();
    if (path.startsWith("/actuator")) {
      return body;
    }

    try {
      // Clean serialization artifacts
      String json = objectMapper.writeValueAsString(body);
      return objectMapper.readValue(json, body.getClass());
    } catch (Exception e) {
      log.warn("❗ Failed to clean JSON response for {}: {}", body.getClass().getSimpleName(), e.getMessage());
      return body;
    }
  }
}
