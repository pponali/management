package com.scaler.price.validation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.Operator;
import com.scaler.price.rule.domain.RuleCondition;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.service.CompetitorService;
import org.apache.poi.ss.usermodel.ConditionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionValidatorTest {

    @Mock
    private TimeValidator mockTimeValidator;
    @Mock
    private CompetitorService mockCompetitorService;
    @Mock
    private ConfigurationService mockConfigService;
    @Mock
    private ObjectMapper mockObjectMapper;
    @Mock
    private PriceServiceMetrics mockMetricsService;

    private ConditionValidator conditionValidatorUnderTest;

    @BeforeEach
    void setUp() {
        conditionValidatorUnderTest = new ConditionValidator(mockTimeValidator, mockCompetitorService,
                mockConfigService, mockObjectMapper, mockMetricsService);
    }

    @Test
    void testValidateConditions() throws Exception {
        // Setup
        final Set<RuleCondition> conditions = Set.of(RuleCondition.builder()
                .type(ConditionType.forId((byte) 0b0))
                .attribute("attribute")
                .operator(Operator.EQUALS)
                .value("value")
                .build());
        when(mockConfigService.getMaxConditionsPerRule()).thenReturn(0);
        when(mockObjectMapper.readValue("value", String.class)).thenReturn("result");

        // Run the test
        conditionValidatorUnderTest.validateConditions(conditions);

        // Verify the results
        verify(mockTimeValidator).validateTimeCondition(RuleCondition.builder()
                .type(ConditionType.forId((byte) 0b0))
                .attribute("attribute")
                .operator(Operator.EQUALS)
                .value("value")
                .build());
    }

    @Test
    void testValidateConditions_ObjectMapperThrowsJsonProcessingException() throws Exception {
        // Setup
        final Set<RuleCondition> conditions = Set.of(RuleCondition.builder()
                .type(ConditionType.forId((byte) 0b0))
                .attribute("attribute")
                .operator(Operator.EQUALS)
                .value("value")
                .build());
        when(mockConfigService.getMaxConditionsPerRule()).thenReturn(0);
        when(mockObjectMapper.readValue("value", String.class)).thenThrow(JsonProcessingException.class);

        // Run the test
        assertThatThrownBy(() -> conditionValidatorUnderTest.validateConditions(conditions))
                .isInstanceOf(RuleValidationException.class);
    }

    @Test
    void testValidateConditions_ObjectMapperThrowsJsonMappingException() throws Exception {
        // Setup
        final Set<RuleCondition> conditions = Set.of(RuleCondition.builder()
                .type(ConditionType.forId((byte) 0b0))
                .attribute("attribute")
                .operator(Operator.EQUALS)
                .value("value")
                .build());
        when(mockConfigService.getMaxConditionsPerRule()).thenReturn(0);
        when(mockObjectMapper.readValue("value", String.class)).thenThrow(JsonMappingException.class);

        // Run the test
        assertThatThrownBy(() -> conditionValidatorUnderTest.validateConditions(conditions))
                .isInstanceOf(RuleValidationException.class);
    }
}
