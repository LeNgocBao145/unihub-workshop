package org.unihubworkshop.workshopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {
    private UUID userId;
    private String studentCode;
    private String department;
    private String major;
    private String className;
}