package com.scaler.price.core.management.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceNotFoundException;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.service.PriceService;
import com.scaler.price.core.management.service.PriceValidationService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PriceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceService priceService;

    @MockBean
    private PriceValidationService validationService;

    @Autowired
    private ObjectMapper objectMapper;

    private PriceDTO testPriceDTO;

    @BeforeEach
    void setUp() {
        testPriceDTO = new PriceDTO();
        testPriceDTO.setProductId(1L);
        testPriceDTO.setSellerId(1L);
        testPriceDTO.setSiteId(1L);
        testPriceDTO.setBasePrice(new BigDecimal("100.00"));
        testPriceDTO.setSellingPrice(new BigDecimal("120.00"));
        testPriceDTO.setEffectiveFrom(LocalDateTime.now());
        testPriceDTO.setEffectiveTo(LocalDateTime.now().plusDays(30));
    }

    @Test
    void createPrice_ValidPrice_ReturnsCreatedPrice() throws Exception, PriceValidationException {
        when(priceService.createPrice(any(PriceDTO.class))).thenReturn(testPriceDTO);

        mockMvc.perform(post("/api/v1/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPriceDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(testPriceDTO.getProductId()))
                .andExpect(jsonPath("$.sellerId").value(testPriceDTO.getSellerId()))
                .andExpect(jsonPath("$.basePrice").value(testPriceDTO.getBasePrice().doubleValue()));
    }

    /* @Test
    void createPrice_InvalidPrice_ReturnsBadRequest() throws Exception, PriceValidationException {
        doThrow(new PriceValidationException("Invalid price"))
            .when(validationService).validatePrice(any(PriceDTO.class));

        mockMvc.perform(post("/api/v1/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPriceDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid price"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists());
    } */

    @Test
    void getPrice_ExistingPrice_ReturnsPrice() throws Exception {
        when(priceService.getPrice(1L)).thenReturn(testPriceDTO);

        mockMvc.perform(get("/api/v1/prices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(testPriceDTO.getProductId()))
                .andExpect(jsonPath("$.sellerId").value(testPriceDTO.getSellerId()));
    }

    @Test
    void getPrice_NonExistingPrice_ReturnsNotFound() throws Exception {
        when(priceService.getPrice(999L))
                .thenThrow(new PriceNotFoundException("Price not found with id: 999"));

        mockMvc.perform(get("/api/v1/prices/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Price not found with id: 999"));
    }

    @Test
    void updatePrice_ValidPrice_ReturnsUpdatedPrice() throws Exception, PriceValidationException {
        when(priceService.updatePrice(eq(1L), any(PriceDTO.class))).thenReturn(testPriceDTO);

        mockMvc.perform(put("/api/v1/prices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPriceDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(testPriceDTO.getProductId()))
                .andExpect(jsonPath("$.sellerId").value(testPriceDTO.getSellerId()));
    }

    @Test
    void deletePrice_ExistingPrice_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/prices/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPriceBySiteAndSeller_ExistingPrice_ReturnsPrice() throws Exception {
        when(priceService.getActivePrice(1L, 1L, 1L)).thenReturn(testPriceDTO);

        mockMvc.perform(get("/api/v1/prices/site/1/seller/1/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(testPriceDTO.getProductId()))
                .andExpect(jsonPath("$.sellerId").value(testPriceDTO.getSellerId()));
    }

    @Test
    void validatePrice_ValidPrice_ReturnsSuccessMessage() throws Exception {
        mockMvc.perform(get("/api/v1/prices/validate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Price validation successful"));
    }

    @Test
    void validateNewPrice_ValidPrice_ReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/v1/prices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPriceDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Price validation successful"));
    }

    @Test
    void getPricesByProduct_ExistingProduct_ReturnsPriceList() throws Exception {
        List<PriceDTO> prices = Arrays.asList(testPriceDTO);
        when(priceService.getPricesByProduct(1L)).thenReturn(prices);

        mockMvc.perform(get("/api/v1/prices/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(testPriceDTO.getProductId()))
                .andExpect(jsonPath("$[0].sellerId").value(testPriceDTO.getSellerId()));
    }

    @Test
    void getPricesByProduct_NonExistingProduct_ReturnsEmptyList() throws Exception {
        when(priceService.getPricesByProduct(999L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/prices/product/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPriceBySite_ExistingPrice_ReturnsPrice() throws Exception {
        when(priceService.getWinningSellerPrice(1L, 1L)).thenReturn(testPriceDTO);

        mockMvc.perform(get("/api/v1/prices/site/1/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(testPriceDTO.getProductId()))
                .andExpect(jsonPath("$.sellerId").value(testPriceDTO.getSellerId()));
    }

    @Test
    void getPriceBySite_NonExistingPrice_ReturnsNotFound() throws Exception {
        when(priceService.getWinningSellerPrice(999L, 999L))
                .thenThrow(new PriceNotFoundException("No winning seller price found"));

        mockMvc.perform(get("/api/v1/prices/site/999/product/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No winning seller price found"));
    }

    @Test
    void updatePrice_NonExistingPrice_ReturnsNotFound() throws Exception, PriceValidationException {
        when(priceService.updatePrice(eq(999L), any(PriceDTO.class)))
                .thenThrow(new PriceNotFoundException("Price not found with id: 999"));

        mockMvc.perform(put("/api/v1/prices/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPriceDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Price not found with id: 999"));
    }

    @Test
    void createPrice_InvalidPriceData_ReturnsBadRequest() throws Exception {
        testPriceDTO.setBasePrice(new BigDecimal("-100.00")); // Invalid negative price

        mockMvc.perform(post("/api/v1/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPriceDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateNewPrice_InvalidPrice_ReturnsValidationError() throws Exception, PriceValidationException {
        doThrow(new PriceValidationException("Invalid price data"))
                .when(validationService).validatePrice(any(PriceDTO.class));

        mockMvc.perform(post("/api/v1/prices/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPriceDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Invalid price data"));
    }
}