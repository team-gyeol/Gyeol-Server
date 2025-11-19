package com.example.oauth2prac.global.gcs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCloudStorageConfig {

    @Value("${gcp.storage.project-id}")
    private String projectId;

    @Bean
    public Storage storage() {
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}