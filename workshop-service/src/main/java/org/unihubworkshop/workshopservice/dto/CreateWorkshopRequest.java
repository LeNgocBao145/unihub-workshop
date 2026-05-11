package org.unihubworkshop.workshopservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.unihubworkshop.workshopservice.models.WorkshopType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkshopRequest {

    @NotBlank(message = "Workshop name is required")
    @Size(min = 3, max = 255, message = "Workshop name must be between 3 and 255 characters")
    private String name;

    @NotBlank(message = "Room is required")
    @Size(min = 1, max = 100, message = "Room must be between 1 and 100 characters")
    private String room;

    @Size(max = 100, message = "Speaker must be less than 100 characters")
    private String speaker;

    @NotNull(message = "Total slots is required")
    @Positive(message = "Total slots must be positive")
    private Integer totalSlots;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be zero or positive")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price = BigDecimal.ZERO;


    private MultipartFile summaryFile;

    private MultipartFile roomMap;

    private WorkshopType type = WorkshopType.FREE;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}

