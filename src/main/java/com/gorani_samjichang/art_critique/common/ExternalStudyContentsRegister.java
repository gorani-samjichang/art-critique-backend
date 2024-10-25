package com.gorani_samjichang.art_critique.common;

import com.gorani_samjichang.art_critique.study.ExternalStudyContentClassificationEntity;
import com.gorani_samjichang.art_critique.study.ExternalStudyContentClassificationRepository;
import com.gorani_samjichang.art_critique.study.ExternalStudyContentEntity;
import com.gorani_samjichang.art_critique.study.ExternalStudyContentsRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalStudyContentsRegister {
    long stamp = 202401010010L;
    final S3Client s3Client;
    final ExternalStudyContentsRepository externalStudyContentsRepository;
    final ExternalStudyContentClassificationRepository externalStudyContentClassificationRepository;
    @Value("${aws.S3.bucket}")
    private String bucketName;

    private List<S3Object> readS3() {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).prefix("tsv/").build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        return listObjectsV2Response.contents();
    }

    @PostConstruct
    public void onStartup() {
        stamp = externalStudyContentsRepository.getStamp().orElse(0L);
    }

    private List<String> downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(s3Client.getObject(getObjectRequest)))) {
            ArrayList<String> fileContent = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.add(line);
            }
            return fileContent;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private ExternalStudyContentEntity insertStudyContentData(String[] l, long now) {
        ExternalStudyContentEntity entity = ExternalStudyContentEntity
                .builder()
                .title(l[0])
                .url(l[1])
                .type(l[2])
                .author(l[3])
                .stamp(now)
                .build();
        try {
            externalStudyContentsRepository.save(entity);
            return externalStudyContentsRepository.findByUrl(l[1]).get();
        } catch (ConstraintViolationException e) {
            Optional<ExternalStudyContentEntity> entityFind = externalStudyContentsRepository.findByUrl(l[1]);
            if (entityFind.isPresent()) return entityFind.get();
            throw e;
        }
    }

    private ExternalStudyContentEntity insertData(String tsv, ExternalStudyContentEntity beforeContent, long now) {
        String[] l = tsv.split("\t");
        if (!beforeContent.getUrl().equals(l[1])) {
            beforeContent = insertStudyContentData(l, now);
        }
        ExternalStudyContentClassificationEntity entity = ExternalStudyContentClassificationEntity.builder().content(beforeContent)
                .keyword(l[5])
                .category(l[4])
                .build();
        externalStudyContentClassificationRepository.save(entity);
        return beforeContent;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000 * 2)
    public void register() {
        List<S3Object> tsv_files = readS3();
        long max_stamp = stamp;
        ExternalStudyContentEntity beforeContent = ExternalStudyContentEntity.builder().url("").build();
        for (S3Object tsv_file : tsv_files) {
            try {
                String key = tsv_file.key().substring(4);
                String now = key.substring(0, 12);
                long d = Long.parseLong(now);
                if (d <= stamp) {
                    continue;
                }
                if (d > max_stamp) {
                    max_stamp = d;
                }
                List<String> contents = downloadFile(tsv_file.key());
                for (String content : contents) {
                    beforeContent = insertData(content, beforeContent, d);
                }
            } catch (Exception e) {
            }
        }
        stamp = max_stamp;
    }
}
