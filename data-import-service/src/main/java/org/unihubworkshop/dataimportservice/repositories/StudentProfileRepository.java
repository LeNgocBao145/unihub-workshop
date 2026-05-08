package org.unihubworkshop.dataimportservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.dataimportservice.models.StudentProfile;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByStudentCode(String studentCode);

    @Query("SELECT sp.studentCode FROM StudentProfile sp WHERE sp.studentCode IN (:studentCodes)")
    Set<String> findExistingStudentCodes(@Param("studentCodes") Set<String> studentCodes);
}
