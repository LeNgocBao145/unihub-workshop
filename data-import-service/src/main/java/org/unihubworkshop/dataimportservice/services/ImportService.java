package org.unihubworkshop.dataimportservice.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.unihubworkshop.dataimportservice.models.*;
import org.unihubworkshop.dataimportservice.repositories.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final StudentProfileRepository repository;
    private final DataImportRepository dataImportRepository;
    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;
    private final StudentClassRepository studentClassRepository;
    public ImportService(StudentProfileRepository repository,
                         DataImportRepository dataImportRepository,
                         DepartmentRepository departmentRepository,
                         MajorRepository majorRepository,
                         StudentClassRepository studentClassRepository) {
        this.repository = repository;
        this.dataImportRepository = dataImportRepository;
        this.departmentRepository = departmentRepository;
        this.majorRepository = majorRepository;
        this.studentClassRepository = studentClassRepository;
    }

    public String processUrl(String fileUrl) {
        log.info("Starting import process for file: {}", fileUrl);
        String filename = extractFileName(fileUrl);
        DataImportRecord importRecord = new DataImportRecord();
        importRecord.setId(UUID.randomUUID());
        importRecord.setFilename(filename);
        importRecord.setImportedAt(LocalDateTime.now());

        try {
            // Step 1: Download file
            log.debug("Downloading file from URL: {}", fileUrl);
            byte[] fileContent = downloadFile(fileUrl);
            log.debug("File downloaded successfully, size: {} bytes", fileContent.length);

            // Step 2: Pass 1 - Collect all student codes from CSV
            log.debug("Extracting student codes from CSV");
            CsvPreprocessData csvData = preprocessCsvData(fileContent);
            log.info("Found {} student codes in CSV", csvData.getStudentCodes().size());
            if (csvData.getStudentCodes().isEmpty()) {
                log.warn("No valid records found in file: {}", filename);
                importRecord.setProcessedRows(0);
                importRecord.setStatus(DataImportStatus.SUCCESS);
                importRecord.setErrorLog("No valid records found in file");
                dataImportRepository.save(importRecord);
                return "Imported, processed_rows=0, errors=0, duplicates=0";
            }

            // Step 3: Single DB query to find existing student codes
            log.debug("Querying database for existing student codes");
            Set<String> existingCodes = repository.findExistingStudentCodes(csvData.getStudentCodes());
            log.info("Found {} existing student codes in database", existingCodes.size());

            // [THÊM MỚI] Step 3.5: Kiểm tra và lưu mới các Department, Major, Class chưa tồn tại
            log.debug("Checking and saving missing reference entities (Departments, Majors, Classes)");
            saveMissingReferenceData(csvData);

            // Step 4: Pass 2 - Parse CSV and build list of profiles
            log.debug("Parsing CSV and building student profiles");
            CsvParseResult parseResult = parseCsvAndBuildProfiles(fileContent, existingCodes);

            // Step 5: Batch save all valid profiles
            if (!parseResult.getProfilesToSave().isEmpty()) {
                log.info("Saving {} student profiles to database", parseResult.getProfilesToSave().size());
                repository.saveAll(parseResult.getProfilesToSave());
            }

            // Step 6: Update import record
            importRecord.setProcessedRows(parseResult.getImportedRows());
            importRecord.setStatus(parseResult.getErrors().isEmpty() ? DataImportStatus.SUCCESS : DataImportStatus.FAILED);
            importRecord.setErrorLog(buildErrorLog(parseResult.getErrors(), parseResult.getDuplicates()));
            dataImportRepository.save(importRecord);

            String report = buildReport(parseResult.getImportedRows(), parseResult.getErrors(), parseResult.getDuplicates());
            log.info("Import completed for file {}: {}", filename, report);
            return report;
        } catch (Exception ex) {
            log.error("Failed to process import file: {}", fileUrl, ex);
            importRecord.setProcessedRows(0);
            importRecord.setStatus(DataImportStatus.FAILED);
            importRecord.setErrorLog(ex.getMessage());
            dataImportRepository.save(importRecord);
            throw new RuntimeException("Failed to process import file", ex);
        }
    }

    private byte[] downloadFile(String fileUrl) throws Exception {
        log.debug("Creating HTTP client for file download");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        try {
            String cleanUrl = cleanUrlString(fileUrl);
            URL url = new URL(cleanUrl);
            URI uri = url.toURI();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            log.debug("Sending HTTP GET request");
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() != 200) {
                log.error("Failed to download file. HTTP Status: {}", response.statusCode());
                throw new RuntimeException("Failed to download file, status=" + response.statusCode());
            }

            log.debug("File downloaded successfully. Response size: {} bytes", response.body().length);
            return response.body();
        } catch (Exception ex) {
            log.error("Error creating HTTP request for URL", ex);
            throw ex;
        }
    }

    private CsvPreprocessData preprocessCsvData(byte[] fileContent) throws Exception {
        Set<String> studentCodes = new HashSet<>();
        Set<String> departments = new HashSet<>();
        Set<String> majors = new HashSet<>();
        Set<String> classes = new HashSet<>();

        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {
            for (CSVRecord record : parser) {
                String studentCode = getField(record, 1, "student_code", "MSSV", "mssv");
                if (studentCode != null && !studentCode.isBlank()) {
                    studentCodes.add(studentCode);
                }

                // [THÊM MỚI] Thu thập Department, Major, Class
                String department = getField(record, 2, "department", "Khoa", "khoa");
                if (department != null && !department.isBlank()) departments.add(department);

                String major = getField(record, 3, "major", "Chuyên ngành", "chuyên ngành");
                if (major != null && !major.isBlank()) majors.add(major);

                String className = getField(record, 4, "class_name", "Lớp", "lop");
                if (className != null && !className.isBlank()) classes.add(className);
            }
        }

        return new CsvPreprocessData(studentCodes, departments, majors, classes);
    }
    private void saveMissingReferenceData(CsvPreprocessData csvData) {
        // 1. Xử lý Department
        if (!csvData.getDepartments().isEmpty()) {
            Set<String> existingDepts = departmentRepository.findExistingNames(csvData.getDepartments());
            List<Department> newDepts = csvData.getDepartments().stream()
                    .filter(name -> !existingDepts.contains(name))
                    .map(name -> {
                        Department d = new Department();
                        d.setId(UUID.randomUUID());
                        d.setDepartmentName(name); // Thay bằng tên property của bạn (VD: setName)
                        return d;
                    }).collect(Collectors.toList());
            if (!newDepts.isEmpty()) {
                departmentRepository.saveAll(newDepts);
                log.info("Saved {} new departments to database", newDepts.size());
            }
        }

        // 2. Xử lý Major
        if (!csvData.getMajors().isEmpty()) {
            Set<String> existingMajors = majorRepository.findExistingNames(csvData.getMajors());
            List<Major> newMajors = csvData.getMajors().stream()
                    .filter(name -> !existingMajors.contains(name))
                    .map(name -> {
                        Major m = new Major();
                        m.setId(UUID.randomUUID());
                        m.setMajorName(name); // Thay bằng tên property của bạn
                        return m;
                    }).collect(Collectors.toList());
            if (!newMajors.isEmpty()) {
                majorRepository.saveAll(newMajors);
                log.info("Saved {} new majors to database", newMajors.size());
            }
        }

        // 3. Xử lý Class
        if (!csvData.getClasses().isEmpty()) {
            Set<String> existingClasses = studentClassRepository.findExistingNames(csvData.getClasses());
            List<StudentClass> newClasses = csvData.getClasses().stream()
                    .filter(name -> !existingClasses.contains(name))
                    .map(name -> {
                        StudentClass c = new StudentClass();
                        c.setId(UUID.randomUUID());
                        c.setClassName(name); // Thay bằng tên property của bạn
                        return c;
                    }).collect(Collectors.toList());
            if (!newClasses.isEmpty()) {
                studentClassRepository.saveAll(newClasses);
                log.info("Saved {} new classes to database", newClasses.size());
            }
        }
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
            String cleanUrl = cleanUrlString(fileUrl);
            URL url = new URL(cleanUrl);
            String path = url.getPath();
            if (path == null || path.isBlank()) {
                return "import-file";
            }
            int slash = path.lastIndexOf('/');
            String filename = slash >= 0 ? path.substring(slash + 1) : path;
            return filename.isBlank() ? "import-file" : filename;
        } catch (Exception ex) {
            log.warn("Failed to extract filename from URL, using default", ex);
            return "import-file";
        }
    }

    private String cleanUrlString(String url) {
        if (url == null) {
            return "";
        }
        return url.trim().replaceAll("^\"|\"$", "");
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
    private static class CsvPreprocessData {
        private final Set<String> studentCodes;
        private final Set<String> departments;
        private final Set<String> majors;
        private final Set<String> classes;

        public CsvPreprocessData(Set<String> studentCodes, Set<String> departments, Set<String> majors, Set<String> classes) {
            this.studentCodes = studentCodes;
            this.departments = departments;
            this.majors = majors;
            this.classes = classes;
        }

        public Set<String> getStudentCodes() { return studentCodes; }
        public Set<String> getDepartments() { return departments; }
        public Set<String> getMajors() { return majors; }
        public Set<String> getClasses() { return classes; }
    }

}

