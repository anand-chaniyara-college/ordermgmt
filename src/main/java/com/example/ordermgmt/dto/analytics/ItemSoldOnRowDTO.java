package com.example.ordermgmt.dto.analytics;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemSoldOnRowDTO {
    private UUID itemId;
    private LocalDateTime soldOn;
}
