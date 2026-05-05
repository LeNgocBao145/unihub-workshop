package org.unihubworkshop.workshopservice.mapper;

import org.springframework.stereotype.Component;
import org.unihubworkshop.workshopservice.models.StudentProfile;
import org.unihubworkshop.workshopservice.dto.StudentProfileResponse;

@Component
public class StudentProfileMapper {

    public StudentProfileResponse toResponse(StudentProfile studentProfile) {
        StudentProfileResponse dto = new StudentProfileResponse();
        dto.setStudentCode(studentProfile.getStudentCode());
        dto.setDepartment(studentProfile.getDepartment());
        dto.setMajor(studentProfile.getMajor());
        dto.setClassName(studentProfile.getClassName());
        dto.setUserId(studentProfile.getUserId());

        return dto;
    }
}
