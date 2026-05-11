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
public class UpdateWorkshopRequest {

    @Size(min = 3, max = 255, message = "Workshop name must be between 3 and 255 characters")
    private String name;

    @Size(min = 1, max = 100, message = "Room must be between 1 and 100 characters")
    private String room;


    @Size(max = 100, message = "Speaker must be less than 100 characters")
    private String speaker;



    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;


    private MultipartFile summaryFile;

}

