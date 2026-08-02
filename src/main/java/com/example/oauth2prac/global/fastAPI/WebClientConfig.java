package com.example.oauth2prac.global.fastAPI;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${fastapi.server.url}")
    private String fastapiServerUrl;

    /**
     * ML 호출용 커넥션 풀 크기.
     * <p>
     * 기본값은 Reactor Netty 의 기본 동작과 동일한 {@code max(코어수, 8) * 2} 이다.
     * 즉 이 설정을 건드리지 않으면 튜닝 전 상태 그대로이며, 부하 테스트 baseline 은
     * 이 상태에서 측정한다.
     */
    @Value("${fastapi.client.max-connections:0}")
    private int maxConnections;

    @Value("${fastapi.client.pending-acquire-max-count:-1}")
    private int pendingAcquireMaxCount;

    @Value("${fastapi.client.pending-acquire-timeout-seconds:45}")
    private long pendingAcquireTimeoutSeconds;

    @Value("${fastapi.client.response-timeout-seconds:60}")
    private long responseTimeoutSeconds;

    @Value("${fastapi.client.connect-timeout-millis:10000}")
    private int connectTimeoutMillis;

    @Bean
    public ConnectionProvider mlConnectionProvider() {
        int resolvedMax = maxConnections > 0
                ? maxConnections
                : Math.max(Runtime.getRuntime().availableProcessors(), 8) * 2;

        log.info("ML WebClient 커넥션 풀: maxConnections={}, pendingAcquireMaxCount={}, pendingAcquireTimeout={}s",
                resolvedMax, pendingAcquireMaxCount, pendingAcquireTimeoutSeconds);

        return ConnectionProvider.builder("ml")
                .maxConnections(resolvedMax)
                .pendingAcquireMaxCount(pendingAcquireMaxCount)
                .pendingAcquireTimeout(Duration.ofSeconds(pendingAcquireTimeoutSeconds))
                // 풀 포화 여부(B2)를 Grafana 에서 보려면 반드시 켜야 한다.
                .metrics(true)
                .build();
    }

    @Bean
    public WebClient webClient(ConnectionProvider mlConnectionProvider) {
        HttpClient httpClient = HttpClient.create(mlConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(fastapiServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
