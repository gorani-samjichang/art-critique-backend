package com.gorani_samjichang.art_critique.common.imageservice;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@DependsOn("stsClient")
public class ImageS3Service {
    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.sts.role-arn}")
    private String roleArn;

    @Value("${aws.sts.session-name}")
    private String sessionName;

    final StsClient stsClient;

    private S3Client s3Client;

    private S3Presigner s3Presigner;


    Credentials getAssumeRoleCredentials() {
        AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .roleSessionName(sessionName)
                .build();
        AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);

        Credentials s3TempCredential = roleResponse.credentials();
        return s3TempCredential;
    }

    @Scheduled(initialDelay = 0, fixedRate = 3550000)
    void prepareS3Client() {
        System.out.println("prepareS3Client" + " at " + LocalDateTime.now());
        Credentials credentials = getAssumeRoleCredentials();
        AwsSessionCredentials awsCredentials = AwsSessionCredentials.create(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken());
        s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

    }

    public String upload(MultipartFile file, String filename, S3Paths s3Paths) throws IOException, S3Exception {
        Path tempFile = Files.createTempFile("upload-", filename);
        file.transferTo(tempFile.toFile());
        String pathname = s3Paths.getPath();
        // S3에 파일 업로드
        String keyName = pathname + filename + ".jpg";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        s3Client.putObject(putObjectRequest, tempFile);

        // 임시 파일 삭제
        Files.delete(tempFile);

        // 업로드된 파일의 S3 경로 반환
//        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + keyName;
        return keyName;
    }

    @Scheduled(initialDelay = 1, fixedRate = 3550000)
    private void prepareS3Presigner() {
        System.out.println("prepareS3Presigner" + " at " + LocalDateTime.now());

        Credentials credentials = getAssumeRoleCredentials();
        AwsSessionCredentials awsCredentials = AwsSessionCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey(),
                credentials.sessionToken()
        );
        s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    public String generatePreSignedURL(String serialNumber, S3Paths s3Paths) {
        String key = s3Paths.getPath() + serialNumber + ".jpg";
        System.out.println("key:" + key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))  // URL 유효 시간 설정
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
        String url = presignedGetObjectRequest.url().toString();
        System.out.println("generated: " + url + " at " + LocalDateTime.now());
        return url;
    }
}
