package io.praharsh.payments;

import io.praharsh.payments.config.PaymentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(PaymentProperties.class)
@SpringBootApplication
public class PaymentFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentFlowApplication.class, args);
    }
}
