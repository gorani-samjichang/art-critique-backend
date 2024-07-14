package com.gorani_samjichang.art_critique.common;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileNotFoundException;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    @Value("${firebaseSdkPath}")
    private String firebaseSdkPath;

    @Value("${firebaseBucket}")
    private String firebaseBucket;

    private FirebaseApp firebaseApp;

    @PostConstruct
    public void initializeFCM() throws IOException {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ClassPathResource(firebaseSdkPath).getInputStream()))
                    .setStorageBucket(firebaseBucket)
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
