package id.co.bankbsi.coinsight.ocr.config;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AzureClientConfig {

    @Value("${azure.form-recognizer.endpoint}")
    private String formRecognizerEndpoint;

    @Value("${azure.form-recognizer.api-key}")
    private String formRecognizerApiKey;

    @Value("${azure.openai.endpoint}")
    private String openAiEndpoint;

    @Value("${azure.openai.api-key}")
    private String openAiApiKey;

    @Value("${azure.openai.deployment-id}")
    private String deploymentId;

    @Value("${azure.form-recognizer.timeout.connection:10000}")
    private int connectionTimeout;

    @Value("${azure.form-recognizer.timeout.read:30000}")
    private int readTimeout;

    @Value("${azure.form-recognizer.timeout.response:30000}")
    private int responseTimeout;

    @Bean
    public HttpClient httpClient() {
        return new NettyAsyncHttpClientBuilder()
                .connectTimeout(Duration.ofMillis(connectionTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .build();
    }

    @Bean
    public DocumentAnalysisClient documentAnalysisClient() {
        log.info("Creating DocumentAnalysisClient with endpoint: {}", formRecognizerEndpoint);
        log.info("Timeout settings - Connection: {}ms, Read: {}ms, Response: {}ms", 
                connectionTimeout, readTimeout, responseTimeout);
        
        return new DocumentAnalysisClientBuilder()
                .endpoint(formRecognizerEndpoint)
                .credential(new AzureKeyCredential(formRecognizerApiKey))
                .httpClient(httpClient())
                .buildClient();
    }

    @Bean
    public OpenAIClient openAIClient() {
        log.info("Creating OpenAIClient with endpoint: {}", openAiEndpoint);
        
        return new OpenAIClientBuilder()
                .endpoint(openAiEndpoint)
                .credential(new AzureKeyCredential(openAiApiKey))
                .httpClient(httpClient())
                .buildClient();
    }

    @Bean
    public String openAIDeploymentId() {
        return deploymentId;
    }
}
