package com.scaler.price.core.management.controller;

import com.scaler.price.core.management.domain.UploadStatus;
import com.scaler.price.core.management.dto.BulkUploadResultDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BulkPriceUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testBulkUpload() throws Exception {
        // Load the template file
        ClassPathResource resource = new ClassPathResource("uploads/prices/price_upload_template.xlsx");
        
        // Create a mock multipart file
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "price_upload_template.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            resource.getInputStream()
        );

        // Perform the upload request
        MvcResult result = mockMvc.perform(multipart("/api/v1/prices/bulk/upload")
                .file(file)
                .param("sellerId", "1672")
                .param("siteId", "17")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response
        String content = result.getResponse().getContentAsString();
        BulkUploadResultDTO response = objectMapper.readValue(content, BulkUploadResultDTO.class);

        // Verify the response
        assertNotNull(response);
        assertNotNull(response.getUploadId());
        assertTrue(response.getTotalRecords() > 0);
        assertEquals(UploadStatus.IN_PROGRESS, response.getStatus());

        // Wait for processing to complete (optional)
        Thread.sleep(5000);  // Adjust time based on your processing needs

        // Verify final status (optional)
        MvcResult statusResult = mockMvc.perform(multipart("/api/v1/prices/bulk/status/" + response.getUploadId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        BulkUploadResultDTO finalStatus = objectMapper.readValue(
                statusResult.getResponse().getContentAsString(),
                BulkUploadResultDTO.class
        );

        assertNotNull(finalStatus);
        assertTrue(finalStatus.getSuccessCount() > 0);
        assertEquals(UploadStatus.COMPLETED, finalStatus.getStatus());
    }

    @Test
    void testBulkUploadWithInvalidFile() throws Exception {
        // Create an invalid file
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "invalid.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "Invalid content".getBytes()
        );

        // Perform the upload request
        MvcResult result = mockMvc.perform(multipart("/api/v1/prices/bulk/upload")
                .file(file)
                .param("sellerId", "1672")
                .param("siteId", "17")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Parse the response
        String content = result.getResponse().getContentAsString();
        BulkUploadResultDTO response = objectMapper.readValue(content, BulkUploadResultDTO.class);

        // Verify the response
        assertNotNull(response);
        assertEquals(UploadStatus.FAILED, response.getStatus());
        assertNotNull(response.getMessage());
    }
}
