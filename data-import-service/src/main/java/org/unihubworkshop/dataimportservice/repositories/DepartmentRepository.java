package org.unihubworkshop.dataimportservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.dataimportservice.models.Department;

import java.util.Set;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    @Query("SELECT d.departmentName FROM Department d WHERE d.departmentName IN :names")
    Set<String> findExistingNames(@Param("names") Set<String> names);

}
