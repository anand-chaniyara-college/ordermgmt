package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.exception.GlobalExceptionHandler;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.validation.ConstraintViolationException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InventoryControllerTest {

        @Mock
        private InventoryService inventoryService;

        @InjectMocks
        private InventoryController inventoryController;

        @Test
        public void testAddInventoryItems_InvalidOperationException_Returns400() throws Exception {
                MockMvc mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();

                // Simulate service throwing InvalidOperationException
                doThrow(new InvalidOperationException("Item Name is required"))
                                .when(inventoryService).addInventoryItems(any());

                String jsonPayload = "[{\"itemId\":\"ITEM005\",\"availableStock\":50}]";

                mockMvc.perform(post("/api/admin/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonPayload))
                                .andExpect(status().isBadRequest()); // Expect 400
        }

        @Test
        public void testAddInventoryItems_ConstraintViolation_Returns500_Or_400() throws Exception {
                MockMvc mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();

                // Simulate Jakarta ConstraintViolationException (usually wrapped, but
                // controller advice catches the cause if unwrapped or if we mock it directly)
                // We throw the raw exception to see if it's handled.
                doThrow(new ConstraintViolationException("Validation failed", java.util.Collections.emptySet()))
                                .when(inventoryService).addInventoryItems(any());

                String jsonPayload = "[{\"itemId\":\"ITEM005\",\"availableStock\":50}]";

                // With handler, this should be 400
                mockMvc.perform(post("/api/admin/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonPayload))
                                .andExpect(status().isBadRequest());
        }
}
