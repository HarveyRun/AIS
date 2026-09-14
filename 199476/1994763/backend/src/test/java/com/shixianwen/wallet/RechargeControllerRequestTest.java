package com.shixianwen.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RechargeControllerRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ordinaryJsonIntegerRemainsSupported() throws Exception {
        RechargeController.Request request = objectMapper.readValue(
            "{\"amount\":1000,\"requestId\":\"request_123456\"}",
            RechargeController.Request.class
        );

        assertEquals("1000", request.amount());
        assertTrue(Validation.buildDefaultValidatorFactory().getValidator().validate(request).isEmpty());
    }

    @Test
    void scientificNotationStringIsRejected() throws Exception {
        RechargeController.Request request = objectMapper.readValue(
            "{\"amount\":\"1e3\",\"requestId\":\"request_123456\"}",
            RechargeController.Request.class
        );

        assertFalse(Validation.buildDefaultValidatorFactory().getValidator().validate(request).isEmpty());
    }
}
