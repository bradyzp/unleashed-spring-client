package net.jastrab.unleashedspringclient;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to import and enable auto-configuration of an UnleashedClient
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import(UnleashedClientConfiguration.class)
@Configuration
@EnableCaching
public @interface EnableUnleashedClient {
}
