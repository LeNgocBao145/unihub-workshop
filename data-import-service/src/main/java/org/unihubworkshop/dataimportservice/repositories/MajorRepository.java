package org.unihubworkshop.dataimportservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.dataimportservice.models.Major;

import java.util.Set;
import java.util.UUID;

@Repository
public interface MajorRepository extends JpaRepository<Major, UUID> {
    @Query("SELECT d.majorName FROM Major d WHERE d.majorName IN :names")
    Set<String> findExistingNames(@Param("names") Set<String> names);
}
