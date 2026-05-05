package org.unihubworkshop.workshopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {
    private long totalWorkshops;
    private long totalRegistrations;
    private List<WorkshopSimpleResponse> workshops;
}
