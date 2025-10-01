package com.example.s3cli.service;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    @Value("${app.s3.bucket:}")
    private String bucket;

    @Value("${app.s3.region:}")
    private String region;

    @Value("${app.s3.endpoint:}")
    private String endpoint;

    @Value("${app.s3.forcePathStyle:false}")
    private boolean forcePathStyle;

    @Value("${app.s3.requestChecksumCalculation:#{null}}")
    private String requestChecksumCalculation;

    @Value("${app.s3.responseChecksumValidation:#{null}}")
    private String responseChecksumValidation;

    @Value("${app.s3.checksumAlgorithm:CRC32}")
    private String checksumAlgorithm;

    @Value("${app.s3.chunkedEncodingEnabled:false}")
    private boolean chunkedEncodingEnabled;

    @Value("${app.s3.serverSideEncryption:NONE}")
    private String serverSideEncryption;

    @Value("${app.s3.withSHAHeader:false}")
    private boolean withSHAHeader;

    @Value("${app.s3.dummychecksum:false}")
    private boolean dummychecksum;

    private S3Client s3;

    @PostConstruct
    public void init() {
        Region r = Region.of(region);
        final AttributeMap attributeMap = AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true).build();
        final SdkHttpClient sdkHttpClient = new DefaultSdkHttpClientBuilder().buildWithDefaults(attributeMap);
        software.amazon.awssdk.services.s3.S3ClientBuilder b = S3Client.builder().httpClient(sdkHttpClient).region(r)
                .forcePathStyle(forcePathStyle)
                .serviceConfiguration(S3Configuration.builder()
                        .chunkedEncodingEnabled(chunkedEncodingEnabled)
                        .build())
                .requestChecksumCalculation(RequestChecksumCalculation.fromValue(requestChecksumCalculation))
                .responseChecksumValidation(ResponseChecksumValidation.fromValue(responseChecksumValidation));

        if (endpoint != null && !endpoint.isBlank()) {
            b.endpointOverride(URI.create(endpoint));
        }

        // Use default provider chain (env vars, profile, etc.)
        s3 = b.build();
    }

    public void uploadFile(File file, String key) {

        log.info("requestChecksumCalculation: {} - {}", requestChecksumCalculation,
                RequestChecksumCalculation.fromValue(requestChecksumCalculation));
        log.info("responseChecksumValidation: {} - {}", responseChecksumValidation,
                ResponseChecksumValidation.fromValue(responseChecksumValidation));
        log.info("checksumAlgorithm: {}", checksumAlgorithm);
        log.info("chunkedEncodingEnabled: {}", chunkedEncodingEnabled);
        log.info("withSHAHeader: {}", withSHAHeader);

        try {
            log.info("--------------------------------");
            log.info("\n\n\nCreating bucket: {}\n\n\n", bucket);
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("\n\n\nBucket created: {}\n\n\n", bucket);
            log.info("--------------------------------");
        } catch (Exception e) {
            log.error("Error creating bucket: {}", e.getMessage(), e);
        }

        log.info("--------------------------------");
        log.info("\n\n\nUploading file: {}\n\n\n", file.getName());

        String checksumSHA256 = null;

        try (FileInputStream is = new FileInputStream(file)) {
            checksumSHA256 = DigestUtils.sha256Hex(is);
            log.info("\n\n\nChecksum SHA256: {}\n\n\n", checksumSHA256);
        } catch (Exception e) {
            log.error("Error calculating checksum SHA256: {}", e.getMessage(), e);
        }

        try (FileInputStream is = new FileInputStream(file)) {
            log.info("\n\n\nChecksum md5: {}\n\n\n", DigestUtils.md5Hex(is));
        } catch (Exception e) {
            log.error("Error calculating checksum md5: {}", e.getMessage(), e);
        }

        if (dummychecksum) {
            checksumSHA256 = "dummy";
        }

        try {
            if (withSHAHeader) {

                PutObjectRequest por = PutObjectRequest.builder().bucket(bucket)
                        .checksumAlgorithm(ChecksumAlgorithm.fromValue(checksumAlgorithm))
                        .checksumSHA256(checksumSHA256)
                        .key(key).build();
                s3.putObject(por, RequestBody.fromFile(file));
            } else {
                PutObjectRequest por = PutObjectRequest.builder().bucket(bucket)
                        .checksumAlgorithm(ChecksumAlgorithm.fromValue(checksumAlgorithm))
                        .key(key).build();
                s3.putObject(por, RequestBody.fromFile(file));
            }
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
        }
        log.info("--------------------------------");
        log.info("\n\n\nFile uploaded: {}\n\n\n", file.getName());

        try {
            log.info("\n\n\nListing objects: {}\n\n\n", bucket);
            ListObjectsV2Response list = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build());

            for (S3Object s3Object : list.contents()) {
                log.info("--------------------------------");
                log.info("\n\n\nObject: {}\n\n\n", s3Object.toString());
                log.info("\n\n\nKey: {}, Size: {}, Has Checksum Algorithm: {}, Checksum Algorithm: {}, ETag: {}\n\n\n",
                        s3Object.key(),
                        s3Object.size(), s3Object.hasChecksumAlgorithm(), s3Object.checksumAlgorithm(),
                        s3Object.eTag());

                try (ResponseInputStream<GetObjectResponse> obj = s3
                        .getObject(GetObjectRequest.builder().bucket(bucket).key(s3Object.key()).build());) {
                    log.info("\n\n\nChecksum md5: {}\n\n\n", DigestUtils.md5Hex(obj));
                    log.info("\n\n\nChecksum SHA256: {}\n\n\n", DigestUtils.sha256Hex(obj));
                } catch (Exception e) {
                    log.error("Error getting object: {}", e.getMessage(), e);
                }
                log.info("--------------------------------");
            }
            log.info("\n\n\nObjects listed: {}\n\n\n", bucket);
        } catch (Exception e) {
            log.error("Error listing objects: {}", e.getMessage(), e);
        }

    }

}
