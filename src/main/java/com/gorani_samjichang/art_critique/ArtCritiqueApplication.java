package com.gorani_samjichang.art_critique;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArtCritiqueApplication {

    public static void main(String[] args) {

        SpringApplication.run(ArtCritiqueApplication.class, args);
    }

}
