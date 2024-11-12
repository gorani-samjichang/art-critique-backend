package com.gorani_samjichang.art_critique.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.study.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalStudyContentsRegister {
    long stamp = 202401010010L;
    final S3Client s3Client;
    final ExternalStudyContentsRepository externalStudyContentsRepository;
    final ExternalStudyContentClassificationRepository externalStudyContentClassificationRepository;
    @Value("${aws.S3.bucket}")
    private String bucketName;
    @PersistenceContext
    private EntityManager entityManager;
    final InnerContentsRepository innerContentsRepository;
    final InnerContentsDetailsRepository innerContentsDetailsRepository;
    long jsonStamp = 0L;

    @Getter
    @Setter
    static class DataJSON {
        @JsonProperty("BigCategory")
        private String bigCategory;
        @JsonProperty("SmallCategory")
        private String smallCategory;
        @JsonProperty("ImageFileList")
        private List<String> imageFileList;
        @JsonProperty("Content")
        private ContentRequestDTO content;
    }

    private List<S3Object> readS3(String prefix) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        return listObjectsV2Response.contents();
    }

    @PostConstruct
    public void onStartup() {
        stamp = externalStudyContentsRepository.getStamp().orElse(0L);
        jsonStamp = innerContentsRepository.getAdminArticleCount();
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

    @Scheduled(fixedDelay = 60 * 60 * 1000 * 2)
    public void register() {
        List<S3Object> tsv_files = readS3("tsv/");
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

    @Scheduled(fixedDelay = 1000 * 2 * 60 * 60)
    public void innerContentsFilesToDB() {
        while (jsonStamp >= 0) {
            try {
                innerContentsSave();
                jsonStamp++;
            } catch (Exception e) {
                log.info("{}번째 파일이 없거나 양식이 맞지 않습니다", jsonStamp);
                break;
            }
        }
    }

    public void innerContentsSave() throws Exception {
        DataJSON data = new ObjectMapper().readValue(innerContentsJSONFileDownload(), DataJSON.class);
        saveContent(data);
    }

    private String innerContentsJSONFileDownload() throws IOException {
        String key = String.format("jsons/%06d.json", jsonStamp + 1);
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(s3Client.getObject(request)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            log.info("{} file 읽기 완료", key);
            return sb.toString();
        } catch (Exception e) {
            throw e;
        }
    }

    public void saveContent(DataJSON dataJson) {
        MemberEntity admin = entityManager.getReference(MemberEntity.class, 1);
        InnerStudyCategory category = entityManager.getReference(InnerStudyCategory.class, dataJson.getSmallCategory());
        ContentRequestDTO requestDTO = dataJson.getContent();
        List<String> fileNames = dataJson.getImageFileList();
        int index = 0;
        for (ArticleContent content : dataJson.getContent().getArticleContent()) {
            if ("img".equals(content.getType())) {
                if (content.isThumnail()) break;
                index++;
            }
        }
        String thumbnailURL = fileNames.get(index);
        InnerContentsEntity innerContentsEntity = InnerContentsEntity.builder()
                .serialNumber(UUID.randomUUID().toString())
                .thumbnailUrl(thumbnailURL)
                .level(requestDTO.getLevel())
                .author(admin)
                .createdAt(LocalDateTime.now())
                .view(0L)
                .likes(0L)
                .title(requestDTO.getArticleTitle())
                .tags(requestDTO.getTagList())
                .subCategory(category)
                .build();
        innerContentsRepository.save(innerContentsEntity);
        InnerContentsDetailsEntity detail;
        for (ArticleContent article : requestDTO.getArticleContent()) {
            if ("img".equals(article.getType())) {
                article.setContent(fileNames.get(index));
                index++;
            }
            detail = InnerContentsDetailsEntity.builder()
                    .type(article.getType())
                    .content(article.getContent())
                    .innerContents(innerContentsEntity)
                    .build();
            innerContentsDetailsRepository.save(detail);
        }

    }
}
