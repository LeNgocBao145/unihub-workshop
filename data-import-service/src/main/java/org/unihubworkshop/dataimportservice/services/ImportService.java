package org.unihubworkshop.dataimportservice.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.unihubworkshop.dataimportservice.models.DataImportRecord;
import org.unihubworkshop.dataimportservice.models.DataImportStatus;
import org.unihubworkshop.dataimportservice.models.StudentProfile;
import org.unihubworkshop.dataimportservice.repositories.DataImportRepository;
import org.unihubworkshop.dataimportservice.repositories.StudentProfileRepository;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

@Service
public class ImportService {

    private final StudentProfileRepository repository;
    private final DataImportRepository dataImportRepository;

    public ImportService(StudentProfileRepository repository, DataImportRepository dataImportRepository) {
        this.repository = repository;
        this.dataImportRepository = dataImportRepository;
    }

    public String processUrl(String fileUrl) {
        String filename = extractFileName(fileUrl);
        DataImportRecord importRecord = new DataImportRecord();
        importRecord.setId(UUID.randomUUID());
        importRecord.setFilename(filename);
        importRecord.setImportedAt(LocalDateTime.now());

        try {
            // Step 1: Download file
            byte[] fileContent = downloadFile(fileUrl);

            // Step 2: Pass 1 - Collect all student codes from CSV
            Set<String> csvStudentCodes = extractStudentCodesFromCsv(fileContent);
            if (csvStudentCodes.isEmpty()) {
                importRecord.setProcessedRows(0);
                importRecord.setStatus(DataImportStatus.SUCCESS);
                importRecord.setErrorLog("No valid records found in file");
                dataImportRepository.save(importRecord);
                return "Imported, processed_rows=0, errors=0, duplicates=0";
            }

            // Step 3: Single DB query to find existing student codes
            Set<String> existingCodes = repository.findExistingStudentCodes(csvStudentCodes);

            // Step 4: Pass 2 - Parse CSV and build list of profiles
            CsvParseResult parseResult = parseCsvAndBuildProfiles(fileContent, existingCodes);

            // Step 5: Batch save all valid profiles
            if (!parseResult.getProfilesToSave().isEmpty()) {
                repository.saveAll(parseResult.getProfilesToSave());
            }

            // Step 6: Update import record
            importRecord.setProcessedRows(parseResult.getImportedRows());
            importRecord.setStatus(parseResult.getErrors().isEmpty() ? DataImportStatus.SUCCESS : DataImportStatus.FAILED);
            importRecord.setErrorLog(buildErrorLog(parseResult.getErrors(), parseResult.getDuplicates()));
            dataImportRepository.save(importRecord);

            return buildReport(parseResult.getImportedRows(), parseResult.getErrors(), parseResult.getDuplicates());
        } catch (Exception ex) {
            importRecord.setProcessedRows(0);
            importRecord.setStatus(DataImportStatus.FAILED);
            importRecord.setErrorLog(ex.getMessage());
            dataImportRepository.save(importRecord);
            throw new RuntimeException("Failed to process import file", ex);
        }
    }

    private byte[] downloadFile(String fileUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to download file, status=" + response.statusCode());
        }

        return response.body();
    }

    // Pass 1: Extract all student codes from CSV for batch lookup (O(1) complexity)
    private Set<String> extractStudentCodesFromCsv(byte[] fileContent) throws Exception {
        Set<String> studentCodes = new HashSet<>();

        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {
            for (CSVRecord record : parser) {
                String studentCode = getField(record, 1, "student_code", "MSSV", "mssv");
                if (studentCode != null && !studentCode.isBlank()) {
                    studentCodes.add(studentCode);
                }
            }
        }

        return studentCodes;
    }

    // Pass 2: Parse CSV and build list of StudentProfile objects for batch save
    private CsvParseResult parseCsvAndBuildProfiles(byte[] fileContent, Set<String> existingCodes) throws Exception {
        List<StudentProfile> profilesToSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        int importedRows = 0;

        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {
            for (CSVRecord record : parser) {
                String fullName = getField(record, 0, "Họ và tên", "full_name", "name");
                String studentCode = getField(record, 1, "student_code", "MSSV", "mssv");
                String department = getField(record, 2, "department", "Khoa", "khoa");
                String major = getField(record, 3, "major", "Chuyên ngành", "chuyên ngành");
                String className = getField(record, 4, "class_name", "Lớp", "lop");

                if (studentCode == null || studentCode.isBlank()) {
                    errors.add("Missing student_code at record: " + record.getRecordNumber());
                    continue;
                }

                if (department == null || department.isBlank()) {
                    errors.add("Missing department for student " + studentCode + " at record: " + record.getRecordNumber());
                    continue;
                }

                // O(1) lookup using Set instead of DB query per row
                if (existingCodes.contains(studentCode)) {
                    duplicates.add(studentCode);
                    continue;
                }

                // Build profile object for batch save
                StudentProfile profile = new StudentProfile();
                profile.setId(UUID.randomUUID());
                profile.setName(fullName);
                profile.setStudentCode(studentCode);
                profile.setDepartment(department);
                profile.setMajor(major);
                profile.setClassName(className);
                profilesToSave.add(profile);
                importedRows++;
            }
        }

        return new CsvParseResult(profilesToSave, importedRows, errors, duplicates);
    }

    private String buildReport(int importedRows, List<String> errors, List<String> duplicates) {
        StringBuilder report = new StringBuilder();
        report.append("Imported, processed_rows=").append(importedRows)
                .append(", errors=").append(errors.size())
                .append(", duplicates=").append(duplicates.size());
        if (!errors.isEmpty()) {
            report.append("; errors: ").append(String.join(";", errors));
        }
        if (!duplicates.isEmpty()) {
            report.append("; duplicates: ").append(String.join(",", duplicates));
        }
        return report.toString();
    }

    private String getField(CSVRecord record, int fallbackIndex, String... headerCandidates) {
        for (String header : headerCandidates) {
            if (record.isMapped(header)) {
                String value = record.get(header);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }

        if (record.size() > fallbackIndex) {
            String value = record.get(fallbackIndex);
            return value == null || value.isBlank() ? null : value;
        }

        return null;
    }

    private String buildErrorLog(List<String> errors, List<String> duplicates) {
        if (errors.isEmpty() && duplicates.isEmpty()) {
            return null;
        }

        List<String> blocks = new ArrayList<>();
        if (!errors.isEmpty()) {
            blocks.add("errors=" + String.join(";", errors));
        }
        if (!duplicates.isEmpty()) {
            blocks.add("duplicates=" + String.join(",", duplicates));
        }
        return String.join(" | ", blocks);
    }

    private String extractFileName(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return fileUrl;
            }
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        } catch (Exception ex) {
            return fileUrl;
        }
    }

    private static class CsvParseResult {
        private final List<StudentProfile> profilesToSave;
        private final int importedRows;
        private final List<String> errors;
        private final List<String> duplicates;

        public CsvParseResult(List<StudentProfile> profilesToSave, int importedRows, List<String> errors, List<String> duplicates) {
            this.profilesToSave = profilesToSave;
            this.importedRows = importedRows;
            this.errors = errors;
            this.duplicates = duplicates;
        }

        public List<StudentProfile> getProfilesToSave() {
            return profilesToSave;
        }

        public int getImportedRows() {
            return importedRows;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getDuplicates() {
            return duplicates;
        }
    }
}

