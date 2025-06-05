package id.co.bankbsi.coinsight.transaction.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  // @Value("${spring.kafka.consumer.group-id}")
  // private String groupId;

  @Bean
  public NewTopic transactionTopic() {
    return TopicBuilder.name("transactions").partitions(1).replicas(1).build();
  }

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    
    // Enhanced retry and error handling settings
    config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // Retry indefinitely
    config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
    config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minutes
    config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // 1 minute
    
    // Enable idempotence to prevent duplicate messages
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    config.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
    config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    
    // Metadata refresh settings to handle leader changes
    config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 50);
    config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 1000);
    
    // Buffer settings
    config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
    config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
    
    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  // @Bean
  // public ConsumerFactory<String, Object> consumerFactory() {
  //     Map<String, Object> config = new HashMap<>();
  //     config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
  //     config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
  //     config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
  //     config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
  //     config.put(JsonDeserializer.TRUSTED_PACKAGES, "id.co.bankbsi.coinsight.*");
  //     return new DefaultKafkaConsumerFactory<>(config);
  // }

  // @Bean
  // public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>>
  // kafkaListenerContainerFactory() {
  //     ConcurrentKafkaListenerContainerFactory<String, Object> factory = new
  // ConcurrentKafkaListenerContainerFactory<>();
  //     factory.setConsumerFactory(consumerFactory());
  //     return factory;
  // }
}
