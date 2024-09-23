package com.gorani_samjichang.art_critique.common;

import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.gorani_samjichang.art_critique.common.imageservice.ImageS3Service;
import com.gorani_samjichang.art_critique.common.imageservice.S3Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class CommonUtil {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Value("${firebaseBucket}")
    String bucketName;

    final ImageS3Service imageS3Service;

    public String generateSecureRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public String uploadToProfileStorage(MultipartFile file, String fileName) throws IOException {
        return imageS3Service.upload(file, fileName, S3Paths.PROFILE);
    }
    public String uploadToFeedbackStorage(MultipartFile file, String fileName) throws IOException {
        return imageS3Service.upload(file, fileName, S3Paths.FEEDBACK_IMAGE);
    }

    public String toProfileImageURL(String serialNumber){
        if (serialNumber==null){
            return null;
        }
        return imageS3Service.generatePreSignedURL(serialNumber, S3Paths.PROFILE);
    }
    public String toFeedbackImageURL(String serialNumber){
        if (serialNumber==null){
            return null;
        }
        return imageS3Service.generatePreSignedURL(serialNumber, S3Paths.FEEDBACK_IMAGE);
    }

    public <T> void copyNonNullProperties(T source, T target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target must not be null");
        }

        Class<?> clazz = source.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                if (value != null) {
                    field.set(target, value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

}