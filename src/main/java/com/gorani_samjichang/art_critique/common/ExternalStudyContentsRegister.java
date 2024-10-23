package com.gorani_samjichang.art_critique.common;

import com.gorani_samjichang.art_critique.study.ExternalStudyContentEntity;
import com.gorani_samjichang.art_critique.study.ExternalStudyContentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
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

@Service
@RequiredArgsConstructor
public class ExternalStudyContentsRegister {
    long stamp = 202401010010L;
    final S3Client s3Client;
    final ExternalStudyContentsRepository externalStudyContentsRepository;
    @Value("${aws.S3.bucket}")
    private String bucketName;

    private List<S3Object> readS3() {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).prefix("tsv/").build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        return listObjectsV2Response.contents();
    }

    @PostConstruct
    public void onStartup(){
        stamp=externalStudyContentsRepository.getStamp().orElse(0L);
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

    private void insertData(String tsv, long now) {
        String[] l = tsv.split("\t");
        ExternalStudyContentEntity entity = ExternalStudyContentEntity
                .builder()
                .title(l[0])
                .url(l[1])
                .author(l[2])
                .about(l[3])
                .keyword((l[4]))
                .stamp(now)
                .build();
        externalStudyContentsRepository.save(entity);
    }

    @Scheduled(fixedRate = 60*60*1000)
    public void register() {
        List<S3Object> tsv_files = readS3();
        long max_stamp = stamp;
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
                    insertData(content, d);
                }
            } catch (Exception e) {
            }
        }
        stamp = max_stamp;
    }
}
