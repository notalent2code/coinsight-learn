package id.co.bankbsi.coinsight.notification.config;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class EmailConfig {

  @Value("${spring.mail.host:localhost}")
  private String host;

  @Value("${spring.mail.port:1025}")
  private int port;

  @Value("${spring.mail.username:}")
  private String username;

  @Value("${spring.mail.password:}")
  private String password;

  @Bean
  public JavaMailSender javaMailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(host);
    mailSender.setPort(port);

    if (!username.isEmpty()) {
      mailSender.setUsername(username);
      mailSender.setPassword(password);
    }

    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", !username.isEmpty());
    props.put("mail.smtp.starttls.enable", "false");
    props.put("mail.debug", "false");

    return mailSender;
  }
}
