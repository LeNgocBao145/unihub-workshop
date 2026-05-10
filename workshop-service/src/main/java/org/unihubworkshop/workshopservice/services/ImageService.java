//package org.unihubworkshop.workshopservice.services;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//import org.unihubworkshop.workshopservice.models.Workshop;
//import org.unihubworkshop.workshopservice.repositories.WorkshopRepository;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//
//import java.io.IOException;
//import java.util.UUID;
//
//@Service
//public class ImageService {
//    private final WorkshopRepository workshopRepository;
//    @Autowired
//    private S3Client s3Client;
//    @Value("${r2.bucket-name}") private String bucketName;
//
//    public ImageService(WorkshopRepository workshopRepository) {
//        this.workshopRepository = workshopRepository;
//    }
//
//    @Value("${r2.endpoint}")
//    private String endpoint;
//    @Transactional
//    public String uploadMap(MultipartFile file) throws IOException {
//
//        if (file.isEmpty()) {
//            return "";
//        }
//        String contentType = file.getContentType();
//        if (contentType == null || !contentType.startsWith("image/")) {
//            throw new IllegalArgumentException("Chỉ chấp nhận định dạng file ảnh (JPG, PNG, WEBP...).");
//        }
//        // 2. Tạo tên file duy nhất cho R2
//        String originalFilename = file.getOriginalFilename();
//        String uniqueFileName = "room_map/" + UUID.randomUUID() + "-" + originalFilename;
//
//        // 3. Upload lên Cloudflare R2
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                .bucket(bucketName)
//                .key(uniqueFileName)
//                .contentType(file.getContentType())
//                .build();
//
//        s3Client.putObject(putObjectRequest,
//                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
//
//        return endpoint + "/" + uniqueFileName;
//    }
//
//
//
//    @Transactional
//    public String uploadWorkshopPdf(MultipartFile file) throws IOException {
//
//        if (file.isEmpty()) {
//            return "";
//        }
//        String contentType = file.getContentType();
//        if (contentType == null || !contentType.equals("application/pdf")) {
//            throw new IllegalArgumentException("Chỉ chấp nhận định dạng file PDF.");
//        }
//        // 2. Tạo tên file duy nhất cho R2
//        String originalFilename = file.getOriginalFilename();
//        String uniqueFileName = "summary/" + UUID.randomUUID() + "-" + originalFilename;
//
//        // 3. Upload lên Cloudflare R2
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                .bucket(bucketName)
//                .key(uniqueFileName)
//                .contentType(file.getContentType())
//                .build();
//
//        s3Client.putObject(putObjectRequest,
//                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
//
//        return endpoint + "/" + uniqueFileName;
//    }
//
//}
