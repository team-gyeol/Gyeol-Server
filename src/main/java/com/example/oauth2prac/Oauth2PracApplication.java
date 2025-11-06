package com.example.oauth2prac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Oauth2PracApplication {

    public static void main(String[] args) {
        SpringApplication.run(Oauth2PracApplication.class, args);
    }

}
