package mdl.order_system_test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class ConductorConfig {

    @Bean
    public RestTemplate conductorRestTemplate() {
        return new RestTemplate();
    }
}
