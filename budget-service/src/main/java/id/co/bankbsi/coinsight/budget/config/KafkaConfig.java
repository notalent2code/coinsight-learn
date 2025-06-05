package id.co.bankbsi.coinsight.budget.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public ConsumerFactory<String, Object> transactionCreatedConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "id.co.bankbsi.coinsight.*");
    props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "id.co.bankbsi.coinsight.budget.event.TransactionCreatedEvent");
    
    // Enhanced timeout and session management
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for better control
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
    
    // Timeout configurations to prevent TimeoutException
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
    
    // Connection and request timeouts
    props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 40000); // 40 seconds
    props.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000); // 1 minute
    props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000); // 9 minutes
    
    // Retry and backoff settings
    props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
    props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);
    
    // Metadata settings to handle broker issues
    props.put("metadata.max.age.ms", 300000); // 5 minutes
    props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);
    
    // Fetch settings
    props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
    
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConsumerFactory<String, Object> transactionDeletedConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "id.co.bankbsi.coinsight.*");
    props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "id.co.bankbsi.coinsight.budget.event.TransactionDeletedEvent");
    
    // Fix offset management
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
    
    // Same timeout configurations as transactionCreatedConsumerFactory
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
    
    props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 40000);
    props.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000);
    props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);
    
    props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
    props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);
    
    props.put("metadata.max.age.ms", 300000);
    props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);
    
    props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
    
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> transactionCreatedListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(transactionCreatedConsumerFactory());
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    
    // Enhanced error handling for TimeoutException and other issues
    org.springframework.kafka.listener.DefaultErrorHandler errorHandler = 
        new org.springframework.kafka.listener.DefaultErrorHandler();
    errorHandler.addNotRetryableExceptions(
        org.apache.kafka.common.errors.TimeoutException.class,
        org.apache.kafka.common.errors.NotLeaderOrFollowerException.class
    );
    factory.setCommonErrorHandler(errorHandler);
    
    // Container properties for better reliability
    factory.getContainerProperties().setPollTimeout(3000);
    factory.getContainerProperties().setMissingTopicsFatal(false);
    factory.setConcurrency(1); // Start with single thread per partition
    
    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> transactionDeletedListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(transactionDeletedConsumerFactory());
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    
    // Enhanced error handling for TimeoutException and other issues
    org.springframework.kafka.listener.DefaultErrorHandler errorHandler = 
        new org.springframework.kafka.listener.DefaultErrorHandler();
    errorHandler.addNotRetryableExceptions(
        org.apache.kafka.common.errors.TimeoutException.class,
        org.apache.kafka.common.errors.NotLeaderOrFollowerException.class
    );
    factory.setCommonErrorHandler(errorHandler);
    
    // Container properties for better reliability
    factory.getContainerProperties().setPollTimeout(3000);
    factory.getContainerProperties().setMissingTopicsFatal(false);
    factory.setConcurrency(1); // Start with single thread per partition
    
    return factory;
  }
}