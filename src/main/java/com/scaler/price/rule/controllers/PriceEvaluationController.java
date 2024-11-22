package com.scaler.price.rule.controllers;

import com.scaler.price.rule.dto.RuleDTO;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.RuleEvaluationException;
import com.scaler.price.rule.service.RuleEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/price-evaluation")
public class PriceEvaluationController {

    private final RuleEvaluationService evaluationService;

    @Autowired
    public PriceEvaluationController(RuleEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<RuleEvaluationResult> evaluatePrice(@RequestBody RuleEvaluationRequest request) {
        RuleEvaluationResult result = evaluationService.evaluatePrice(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch-evaluate")
    public ResponseEntity<List<RuleEvaluationResult>> evaluateBatchPrices(@RequestBody List<RuleEvaluationRequest> requests) {
        List<RuleEvaluationResult> results = requests.stream()
                .map(evaluationService::evaluatePrice)
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/preview")
    public ResponseEntity<RuleEvaluationResult> previewRuleApplication(
            @RequestBody RuleDTO ruleDTO,
            @RequestBody RuleEvaluationRequest ruleEvaluationRequest) {
        RuleEvaluationResult result = null;
        try {
            result = evaluationService.previewRuleApplication(ruleDTO, ruleEvaluationRequest);
        } catch (ActionRegistrationException e) {
            throw new RuntimeException(e);
        } catch (ActionExecutionException e) {
            throw new RuntimeException(e);
        } catch (RuleEvaluationException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(result);
    }
}