package com.gorani_samjichang.art_critique.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@DependsOn("s3Client")
public class ImageUploader {
    private final S3Client s3Client;
    @Value("${aws.S3.bucket}")
    private String bucketName;
    @Value("${aws.region}")
    private String region;
    @Value("${aws.S3.path}")
    private String path;

    public String uploadToS3(MultipartFile file, String filename) throws IOException {
        Path tempFile = Files.createTempFile("upload-", filename);
        file.transferTo(tempFile.toFile());

        // S3에 파일 업로드
        String keyName =  path + filename + ".jpg";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        s3Client.putObject(putObjectRequest, tempFile);

        // 임시 파일 삭제
        Files.delete(tempFile);

        // 업로드된 파일의 S3 경로 반환
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + keyName;
    }

}
