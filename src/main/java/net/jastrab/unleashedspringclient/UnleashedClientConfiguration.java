package net.jastrab.unleashedspringclient;

import net.jastrab.unleashed.api.converters.UnleashedObjectMapper;
import net.jastrab.unleashed.api.security.ApiCredential;
import net.jastrab.unleashed.api.security.ApiCredentialImpl;
import net.jastrab.unleashedspringclient.client.UnleashedAuthInterceptor;
import net.jastrab.unleashedspringclient.client.UnleashedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Objects;

@Configuration
@EnableConfigurationProperties(UnleashedClientProperties.class)
public class UnleashedClientConfiguration {
    private final static Logger LOGGER = LoggerFactory.getLogger(UnleashedClientConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(RestTemplateBuilder.class)
    public RestTemplateBuilder unleashedRestTemplate() {
        return new RestTemplateBuilder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "unleashed.client", name = "caching.enabled", havingValue = "false")
    public CacheManager noopCacheManager() {
        LOGGER.info("Unleashed caching disabled");
        return new NoOpCacheManager();
    }

    public MappingJackson2HttpMessageConverter unleashedMessageConverter() {
        return new MappingJackson2HttpMessageConverter(new UnleashedObjectMapper());
    }

    @Bean
    @ConditionalOnMissingBean(UnleashedClient.class)
    public UnleashedClient createUnleashedClient(RestTemplateBuilder restTemplateBuilder,
                                                 UnleashedClientProperties properties) {
        Objects.requireNonNull(properties.getApiId(),
                "Unleashed API ID (unleashed.client.api-id) value must not be null");
        Objects.requireNonNull(properties.getApiKey(),
                "Unleashed API Key (unleashed.client.api-key) value must not be null");
        final ApiCredential credential = new ApiCredentialImpl(properties.getApiId(), properties.getApiKey());
        final RestTemplateBuilder builder = restTemplateBuilder
                .additionalInterceptors(new UnleashedAuthInterceptor(credential));

        return new UnleashedClient(properties.getBaseUri(), builder, unleashedMessageConverter());
    }

}
