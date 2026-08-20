package com.shixianwen.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;

/** 将金额作为十进制字符串返回，避免客户端用浮点数接收时产生精度误差。 */
@Configuration
public class MoneyJsonConfiguration {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer bigDecimalAsPlainString() {
        return builder -> builder.serializerByType(BigDecimal.class, new PlainBigDecimalSerializer());
    }

    private static final class PlainBigDecimalSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(
            BigDecimal value,
            JsonGenerator generator,
            SerializerProvider serializers
        ) throws IOException {
            generator.writeString(value.toPlainString());
        }
    }
}
