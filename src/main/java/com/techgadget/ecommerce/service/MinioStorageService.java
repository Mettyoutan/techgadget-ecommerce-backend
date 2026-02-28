package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.response.image.StoredImageDto;
import com.techgadget.ecommerce.exception.ContentTooLargeException;
import com.techgadget.ecommerce.exception.InternalServerException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageService(
            MinioClient minioClient,
            @Value("${minio.bucket}") String bucket
    ) {
        this.minioClient = minioClient;
        this.bucket = bucket;

        // EnsureBucketExists
        this.ensureBucketExists();
    }

    /**
     * Check if bucket exists
     * -
     * If not, create the bucket
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            log.warn("Failed to check if bucket {} exists: {}", bucket, e.getMessage());
            throw new InternalServerException("Failed to ensure bucket.");
        }
    }

    /**
     * Store object & create thumbnail (Optional)
     */
    public StoredImageDto store(MultipartFile file, String objectKey) {

        try (InputStream is = file.getInputStream()) {
            // Upload original image object
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Upload thumbnail object
            String thumbKey = uploadThumbnail(file, objectKey);

            return new StoredImageDto(objectKey, thumbKey);

        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            log.warn("MinIO failed to store image - Bucket={}, ObjectKey={}, Code={}",
                    bucket, objectKey, e.errorResponse().code(), e
            );
            if ("EntityTooLarge".equals(code)) {
                throw new ContentTooLargeException("Uploaded image is too large");
            }
            throw new InternalServerException("Failed to store image.");
        } catch (Exception e) {
            log.error("Unexpected error while storing image - ObjectKey={}", objectKey, e);
            throw new InternalServerException("Failed to store image.");
        }
    }

    /**
     * Delete object
     */
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to remove image - ObjectKey={}", objectKey, e);
            throw new InternalServerException("Failed to remove image.");
        }
    }

    /**
     * Get presigned url (GET method)
     */
    public String generateViewUrl(String objectOrThumbnailKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET) // only GET method
                            .bucket(bucket)
                            .object(objectOrThumbnailKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get presigned url - ObjectKey/ThumbnailKey={}", objectOrThumbnailKey, e);
            throw new InternalServerException("Failed to get presigned url.");
        }
    }

    /**
     * Upload thumbnail from nonThumbnailKet (OPTIONAL)
     * -
     * Return thumbnailKey / null
     */
    private String uploadThumbnail(MultipartFile file, String nonThumbnailKey) {

        try (
                // baos for thumbnails output
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = file.getInputStream()
        ) {

            Thumbnails.of(is)
                    .size(400, 400)
                    .outputFormat("jpg")
                    .toOutputStream(baos);

            // giraffe.jpg -> giraffe-thumb.jpg
            String thumbKey = nonThumbnailKey.replace(".", "-thumb.");

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(thumbKey)
                            .stream(new ByteArrayInputStream(baos.toByteArray()),
                                    baos.size(), -1)
                            .contentType("image/jpeg")
                            .build()
            );
            return thumbKey;

        } catch (Exception e) {
            log.warn("Failed to upload thumbnail - Bucket={}, ObjectKey={}",
                    bucket, nonThumbnailKey);
            return null; // Thumbnail OPTIONAL
        }

    }
}
