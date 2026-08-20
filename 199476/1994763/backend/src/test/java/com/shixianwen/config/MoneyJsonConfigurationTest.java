package com.shixianwen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyJsonConfigurationTest {
    @Test
    void serializesBigDecimalAsPlainJsonString() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new MoneyJsonConfiguration().bigDecimalAsPlainString().customize(builder);
        ObjectMapper mapper = builder.build();

        assertEquals("\"10.20\"", mapper.writeValueAsString(new BigDecimal("10.20")));
    }
}
