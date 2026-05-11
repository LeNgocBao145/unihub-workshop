package org.unihubworkshop.workshopservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class ImageService {
    private final WorkshopRepository workshopRepository;
    @Autowired
    private S3Client s3Client;
    @Value("${r2.bucket-name}") private String bucketName;
    @Autowired
    private S3Presigner s3Presigner;
    public ImageService(WorkshopRepository workshopRepository) {
        this.workshopRepository = workshopRepository;
    }

    @Value("${r2.endpoint}")
    private String endpoint;
    @Transactional
    public String uploadMap(MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return "";
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận định dạng file ảnh (JPG, PNG, WEBP...).");
        }
        // 2. Tạo tên file duy nhất cho R2
        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = "room_map/" + UUID.randomUUID() + "-" + originalFilename;

        // 3. Upload lên Cloudflare R2
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return  uniqueFileName;
    }



    @Transactional
    public String uploadWorkshopPdf(MultipartFile file) throws IOException {

        if(file == null) return "";
        if (file.isEmpty()) {
            return "";
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Chỉ chấp nhận định dạng file PDF.");
        }
        // 2. Tạo tên file duy nhất cho R2
        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = "summary/" + UUID.randomUUID() + "-" + originalFilename;

        // 3. Upload lên Cloudflare R2
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return uniqueFileName;
    }
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            // Ném ra lỗi runtime để hệ thống dễ dàng xử lý (hoặc bạn có thể log lỗi tùy theo dự án)
            throw new RuntimeException("Lỗi khi xóa file trên R2: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
    public String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return "";
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imagePath)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(120))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest =
                    s3Presigner.presignGetObject(getObjectPresignRequest);

            return presignedGetObjectRequest.url().toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo URL cho ảnh: " + e.getMessage(), e);
        }
    }
}
