package org.unihubworkshop.workshopservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookTicketRequest {
    @NotBlank(message = "Student code is required")
    private String studentCode;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Major is required")
    private String major;

    @NotBlank(message = "Class name is required")
    private String className;
}

