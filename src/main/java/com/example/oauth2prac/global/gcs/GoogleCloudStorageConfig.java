package com.example.oauth2prac.global.gcs;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class GoogleCloudStorageConfig {

    @Value("${gcp.storage.project-id}")
    private String projectId;

    /**
     * 로컬 부하 테스트에서는 GCS 에뮬레이터(fake-gcs-server)를 가리킨다.
     * 값이 비어 있으면 평소대로 실제 GCS + Application Default Credentials 를 쓴다.
     */
    @Value("${gcp.storage.emulator-host:}")
    private String emulatorHost;

    @Bean
    public Storage storage() {
        if (StringUtils.hasText(emulatorHost)) {
            log.warn("GCS 에뮬레이터를 사용합니다: {} (실제 GCS 아님)", emulatorHost);
            return StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setHost(emulatorHost)
                    .setCredentials(NoCredentials.getInstance())
                    .build()
                    .getService();
        }

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
