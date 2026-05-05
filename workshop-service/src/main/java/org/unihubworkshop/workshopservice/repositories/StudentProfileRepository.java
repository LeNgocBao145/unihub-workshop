package org.unihubworkshop.workshopservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.workshopservice.models.StudentProfile;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends 
        JpaRepository<StudentProfile, UUID>, 
        JpaSpecificationExecutor<StudentProfile> {
}