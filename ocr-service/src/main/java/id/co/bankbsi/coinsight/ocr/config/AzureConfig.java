// package id.co.bankbsi.coinsight.ocr.config;

// import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
// import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
// import com.azure.ai.openai.OpenAIClient;
// import com.azure.ai.openai.OpenAIClientBuilder;
// import com.azure.core.credential.AzureKeyCredential;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class AzureConfig {

//   @Value("${azure.form-recognizer.endpoint}")
//   private String formRecognizerEndpoint;

//   @Value("${azure.form-recognizer.api-key}")
//   private String formRecognizerApiKey;

//   @Value("${azure.openai.endpoint}")
//   private String openAIEndpoint;

//   @Value("${azure.openai.api-key}")
//   private String openAIApiKey;

//   @Value("${azure.openai.deployment-id}")
//   private String deploymentId;

//   @Bean
//   public DocumentAnalysisClient documentAnalysisClient() {
//     return new DocumentAnalysisClientBuilder()
//         .credential(new AzureKeyCredential(formRecognizerApiKey))
//         .endpoint(formRecognizerEndpoint)
//         .buildClient();
//   }

//   @Bean
//   public OpenAIClient openAIClient() {
//     return new OpenAIClientBuilder()
//         .credential(new AzureKeyCredential(openAIApiKey))
//         .endpoint(openAIEndpoint)
//         .buildClient();
//   }

//   @Bean
//   public String openAIDeploymentId() {
//     return deploymentId;
//   }
// }
