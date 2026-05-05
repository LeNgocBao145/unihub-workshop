package org.unihubworkshop.authservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.unihubworkshop.authservice.entities.StudentProfile;
import org.unihubworkshop.authservice.repositories.StudentProfileRepository;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    public Page<StudentProfile> findAll(Specification<StudentProfile> spec, Pageable pageable) {
        return studentProfileRepository.findAll(spec, pageable);
    }
}