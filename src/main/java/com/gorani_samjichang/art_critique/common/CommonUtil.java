package com.gorani_samjichang.art_critique.common;

import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;

@Component
public class CommonUtil {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Value("${firebaseBucket}")
    String bucketName;
    public String generateSecureRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public String uploadToStorage(MultipartFile file, String fileName) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket();
        InputStream content = new ByteArrayInputStream(file.getBytes());
        bucket.create(fileName, content, file.getContentType());
        return "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/" + fileName + "?alt=media&token=";
    }
}
