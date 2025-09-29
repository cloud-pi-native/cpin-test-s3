package com.example.s3cli.service;

import java.io.File;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.AttributeMap;

@Service
public class S3Service {

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.region:us-east-1}")
    private String region;

    @Value("${app.s3.endpoint:}")
    private String endpoint;

    private S3Client s3;

    @PostConstruct
    public void init() {
        Region r = Region.of(region);
        final AttributeMap attributeMap = AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true).build();
        final SdkHttpClient sdkHttpClient = new DefaultSdkHttpClientBuilder().buildWithDefaults(attributeMap);
        software.amazon.awssdk.services.s3.S3ClientBuilder b = S3Client.builder().httpClient(sdkHttpClient).region(r)
                .forcePathStyle(true);

        if (endpoint != null && !endpoint.isBlank()) {
            b.endpointOverride(URI.create(endpoint));
        }

        // Use default provider chain (env vars, profile, etc.)
        s3 = b.build();
    }

    public void uploadFile(File file, String key) {
        PutObjectRequest por = PutObjectRequest.builder().bucket(bucket).key(key).build();

        s3.putObject(por, RequestBody.fromFile(file));
    }
}
