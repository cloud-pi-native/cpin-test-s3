package com.example.s3cli.runner;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.s3cli.service.S3Service;

@Component
public class UploadRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UploadRunner.class);

    private final S3Service s3Service;

    @Value("${app.s3.file_to_upload:}")
    private String fileToUpload;

    public UploadRunner(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @Override
    public void run(String... args) throws Exception {
        String path = fileToUpload;
        String key = new File(path).getName();

        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            log.error("File not found or not a file: {}", path);
            return;
        }

        log.info("Uploading {} to bucket as key '{}'...", path, key);
        s3Service.uploadFile(f, key);
        log.info("Upload complete.");
    }
}
