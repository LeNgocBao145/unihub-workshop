package org.unihubworkshop.dataimportservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.dataimportservice.models.StudentClass;

import java.util.Set;
import java.util.UUID;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, UUID> {
    @Query("SELECT d.className FROM StudentClass d WHERE d.className IN :names")
    Set<String> findExistingNames(@Param("names") Set<String> names);
}
