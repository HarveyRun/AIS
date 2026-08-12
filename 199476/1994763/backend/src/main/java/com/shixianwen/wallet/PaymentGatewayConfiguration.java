package com.shixianwen.wallet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class PaymentGatewayConfiguration {
    @Bean
    @ConditionalOnMissingBean(PaymentGateway.class)
    @ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
    PaymentGateway mockAlipayGateway() {
        return new MockAlipayGateway();
    }
}
