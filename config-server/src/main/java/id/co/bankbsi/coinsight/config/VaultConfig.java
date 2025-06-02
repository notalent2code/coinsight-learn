package id.co.bankbsi.coinsight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

@Configuration
public class VaultConfig {
    
    @Value("${VAULT_HOST:vault}")
    private String vaultHost;
    
    @Value("${VAULT_PORT:8200}")
    private int vaultPort;
    
    @Value("${VAULT_TOKEN:myroot}")
    private String vaultToken;
    
    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint vaultEndpoint = VaultEndpoint.create(vaultHost, vaultPort);
        vaultEndpoint.setScheme("http");
        
        return new VaultTemplate(vaultEndpoint, new TokenAuthentication(vaultToken));
    }
}