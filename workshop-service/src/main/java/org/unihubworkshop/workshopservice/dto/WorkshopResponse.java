package org.unihubworkshop.workshopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.unihubworkshop.workshopservice.models.WorkshopType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopResponse {

    private UUID id;
    private String name;
    private UUID hostId;
    private String room;
    private String roomMap;
    private String speaker;
    private Integer totalSlots;
    private Integer availableSlots;
    private String description;
    private BigDecimal price;
    private WorkshopType type;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

