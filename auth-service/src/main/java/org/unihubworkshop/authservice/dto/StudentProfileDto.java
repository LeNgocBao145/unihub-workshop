package org.unihubworkshop.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.unihubworkshop.authservice.entities.StudentProfile;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileDto {
    private UUID userId;
    private String studentCode;
    private String department;
    private String major;
    private String className;

    public static StudentProfileDto fromEntity(StudentProfile entity) {
        if (entity == null) {
            return null;
        }
        return StudentProfileDto.builder()
                .userId(entity.getUserId())
                .studentCode(entity.getStudentCode())
                .department(entity.getDepartment())
                .major(entity.getMajor())
                .className(entity.getClassName())
                .build();
    }
}