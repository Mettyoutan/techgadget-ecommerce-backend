package com.techgadget.ecommerce.service;

import com.techgadget.ecommerce.dto.response.image.StoredImageDto;
import com.techgadget.ecommerce.exception.ContentTooLargeException;
import com.techgadget.ecommerce.exception.InternalServerException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.kv;

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
            log.error("ensureBucketExists.failed: {}.", e.getMessage(), e);
            throw new InternalServerException();
        }

        log.info("Bucket exists!", kv("bucket", bucket));
    }

    /**
     * Store object & create thumbnail (Optional)
     */
    public StoredImageDto store(MultipartFile file, String originalKey) {

        log.debug("store.started.",
            kv("originalKey", originalKey),
            kv("fileSize", file.getSize())
        );

        try (InputStream is = file.getInputStream()) {
            // Upload original image object
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(originalKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Upload thumbnail object
            String thumbKey = uploadThumbnail(file, originalKey);

            return new StoredImageDto(originalKey, thumbKey);

        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            log.warn("store.failed: MinIO failure.",
                    kv("bucket", bucket),
                    kv("originalKey", originalKey),
                    kv("errorCode", e.errorResponse().code()),
                    e
            );
            if ("EntityTooLarge".equals(code)) {
                throw new ContentTooLargeException("Uploaded image is too large");
            }
            throw new InternalServerException();
        } catch (Exception e) {
            log.error("store.failed: unexpected error while storing image.",
                    kv("originalKey", originalKey),
                    e
            );
            throw new InternalServerException();
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
            throw new InternalServerException();
        }
    }

    /**
     * Get presigned url (GET method)
     */
    public @Nullable String generateViewUrl(String objectKey) {

        log.debug("generateViewUrl.started.",
                kv("objectKey", objectKey)
        );

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET) // only GET method
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.warn("generateViewUrl.failed: MinIO failed to get presigned url.",
                    kv("objectKey", objectKey)
            );
            return null; // Image is skipped
        }
    }

    /**
     * Upload thumbnail from nonThumbnailKet (OPTIONAL)
     * -
     * Return thumbnailKey / null
     */
    private @Nullable String uploadThumbnail(MultipartFile file, String originalKey) {

        // giraffe.jpg -> giraffe-thumb.jpg
        String thumbKey = originalKey.replace(".", "-thumb.");

        try (
                // baos for thumbnails output
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = file.getInputStream()
        ) {

            Thumbnails.of(is)
                    .size(400, 400)
                    .outputFormat("jpg")
                    .toOutputStream(baos);

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
            log.warn("uploadThumbnail.failed: {}.",
                    e.getMessage(),
                    kv("bucket", bucket),
                    kv("thumbKey", thumbKey)
            );
            return null; // Thumbnail OPTIONAL
        }

    }
}
