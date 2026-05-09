package org.unihubworkshop.dataimportservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "data_imports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DataImportRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "filename", length = 512)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DataImportStatus status;

    @Column(name = "processed_rows")
    private Integer processedRows;

    @Column(name = "error_log", columnDefinition = "text")
    private String errorLog;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;
}
