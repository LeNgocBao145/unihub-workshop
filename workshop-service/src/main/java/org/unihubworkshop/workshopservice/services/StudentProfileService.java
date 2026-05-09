package org.unihubworkshop.workshopservice.services;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.unihubworkshop.workshopservice.models.StudentProfile;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.StudentProfileRepository;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.mapper.StudentProfileMapper;
import org.unihubworkshop.workshopservice.dto.StudentProfileResponse;
import org.unihubworkshop.workshopservice.dto.BookTicketRequest;
import org.unihubworkshop.workshopservice.exceptions.InvalidWorkshopException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Service
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileMapper studentProfileMapper;
    
    public StudentProfileService(StudentProfileRepository studentProfileRepository, StudentProfileMapper studentProfileMapper) {
        this.studentProfileRepository = studentProfileRepository;
        this.studentProfileMapper = studentProfileMapper;
    }

 @Transactional(readOnly = true)
    public List<StudentProfileResponse> getAllStudents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return studentProfileRepository.findAll(pageable).stream()
                .map(studentProfileMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void verifyStudentProfile(BookTicketRequest request) {
        StudentProfile studentProfile = studentProfileRepository.findByStudentCode(request.getStudentCode())
                .orElseThrow(() -> new InvalidWorkshopException("Student profile not found with code: " + request.getStudentCode()));

        validateStudentInformation(studentProfile, request);
    }

    private void validateStudentInformation(StudentProfile profile, BookTicketRequest request) {
        if (!profile.getName().equalsIgnoreCase(request.getName())) {
            throw new InvalidWorkshopException("Name information does not match");
        }

        if (!profile.getDepartment().equalsIgnoreCase(request.getDepartment())) {
            throw new InvalidWorkshopException("Department information does not match");
        }

        if (!profile.getMajor().equalsIgnoreCase(request.getMajor())) {
            throw new InvalidWorkshopException("Major information does not match");
        }

        if (!profile.getClassName().equalsIgnoreCase(request.getClassName())) {
            throw new InvalidWorkshopException("Class name information does not match");
        }
    }
}