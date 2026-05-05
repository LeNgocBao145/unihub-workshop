package org.unihubworkshop.workshopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopPaymentResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
}
