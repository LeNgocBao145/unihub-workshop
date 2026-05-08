package org.unihubworkshop.workshopservice.services;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.HttpMethod;

@Service
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${spaces.bucket}")
    private String bucket;

    public S3Service(@Value("${spaces.endpoint}") String endpoint,
                     @Value("${spaces.region:ap-southeast-1}") String region,
                     @Value("${spaces.access-key}") String accessKey,
                     @Value("${spaces.secret-key}") String secretKey) {
        BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .build();
    }

    public String uploadFile(MultipartFile file, String keyPrefix) throws IOException {
        File convFile = convertMultipartFileToFile(file);
        String key = keyPrefix + "/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
        PutObjectRequest putReq = new PutObjectRequest(bucket, key, convFile)
                .withCannedAcl(CannedAccessControlList.Private);
        s3Client.putObject(putReq);
        
        // Generate pre-signed URL valid for 7 days
        Date expiration = new Date(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
        GeneratePresignedUrlRequest presignedRequest = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);
        
        URL presignedUrl = s3Client.generatePresignedUrl(presignedRequest);
        
        // delete temp file
        convFile.delete();
        return presignedUrl.toString();
    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("upload-", "-tmp");
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            IOUtils.copy(file.getInputStream(), fos);
        }
        return convFile;
    }
}
