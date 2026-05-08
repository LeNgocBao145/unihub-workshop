package org.unihubworkshop.dataimportservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.dataimportservice.models.DataImportRecord;

import java.util.UUID;

@Repository
public interface DataImportRepository extends JpaRepository<DataImportRecord, UUID> {
}
