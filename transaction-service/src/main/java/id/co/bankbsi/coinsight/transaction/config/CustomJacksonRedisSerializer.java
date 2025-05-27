// package id.co.bankbsi.coinsight.transaction.config;

// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.springframework.data.redis.serializer.RedisSerializer;
// import org.springframework.data.redis.serializer.SerializationException;

// public class CustomJacksonRedisSerializer<T> implements RedisSerializer<T> {

//     private final ObjectMapper objectMapper;
//     private final Class<T> type;

//     public CustomJacksonRedisSerializer(ObjectMapper objectMapper, Class<T> type) {
//         this.objectMapper = objectMapper;
//         this.type = type;
//     }

//     @Override
//     public byte[] serialize(T t) throws SerializationException {
//         if (t == null) {
//             return new byte[0];
//         }
//         try {
//             return objectMapper.writeValueAsBytes(t);
//         } catch (JsonProcessingException e) {
//             throw new SerializationException("Could not write JSON: " + e.getMessage(), e);
//         }
//     }

//     @Override
//     public T deserialize(byte[] bytes) throws SerializationException {
//         if (bytes == null || bytes.length == 0) {
//             return null;
//         }
//         try {
//             return objectMapper.readValue(bytes, type);
//         } catch (Exception e) {
//             throw new SerializationException("Could not read JSON: " + e.getMessage(), e);
//         }
//     }
// }
