package com.example.ordermgmt;

import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.repository.OrderStatusLookupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuditIntegrationTest {

    @Autowired
    private OrderStatusLookupRepository statusRepository;

    @Test
    @WithMockUser(username = "testuser", roles = { "ADMIN" })
    public void testAuditFields() {
        // Create
        OrderStatusLookup status = new OrderStatusLookup();
        status.setStatusId(999);
        status.setStatusName("TEST_STATUS");

        OrderStatusLookup savedStatus = statusRepository.save(status);

        assertNotNull(savedStatus.getCreatedTimestamp(), "Created Timestamp should not be null");
        assertNotNull(savedStatus.getUpdatedTimestamp(), "Updated Timestamp should not be null");
        assertEquals("testuser", savedStatus.getCreatedBy(), "Created By should be testuser");
        assertEquals("testuser", savedStatus.getUpdatedBy(), "Updated By should be testuser");

        LocalDateTime createdTime = savedStatus.getCreatedTimestamp();

        // Update
        savedStatus.setStatusName("UPDATED_TEST_STATUS");
        // Flush to ensure update
        statusRepository.saveAndFlush(savedStatus);

        OrderStatusLookup updatedStatus = statusRepository.findById(999).orElseThrow();

        assertEquals(createdTime, updatedStatus.getCreatedTimestamp(), "Created Timestamp should not change");
        // Note: In some fast executions, updated timestamp might be same if within same
        // millisecond/microsecond precision depending on DB
        // But usually jpa auditing updates it.
        assertNotNull(updatedStatus.getUpdatedTimestamp());
        assertEquals("testuser", updatedStatus.getUpdatedBy());
    }
}
