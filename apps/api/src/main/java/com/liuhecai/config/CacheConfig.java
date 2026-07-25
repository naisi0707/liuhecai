package com.liuhecai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DOMAIN_BY_HOST = "domain-by-host";
    public static final String TENANT_CURRENT = "tenant-current";
    public static final String DRAWS_LATEST_ALL = "draws-latest-all";
    public static final String DRAW_HISTORY = "draw-history";
    public static final String CMS_MENUS = "cms-menus";
    public static final String CMS_PAGES = "cms-pages";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(DOMAIN_BY_HOST, Caffeine.newBuilder()
                .maximumSize(2000).expireAfterWrite(60, TimeUnit.SECONDS).build());
        manager.registerCustomCache(TENANT_CURRENT, Caffeine.newBuilder()
                .maximumSize(1000).expireAfterWrite(60, TimeUnit.SECONDS).build());
        manager.registerCustomCache(DRAWS_LATEST_ALL, Caffeine.newBuilder()
                .maximumSize(500).expireAfterWrite(15, TimeUnit.SECONDS).build());
        manager.registerCustomCache(DRAW_HISTORY, Caffeine.newBuilder()
                .maximumSize(200).expireAfterWrite(120, TimeUnit.SECONDS).build());
        manager.registerCustomCache(CMS_MENUS, Caffeine.newBuilder()
                .maximumSize(500).expireAfterWrite(60, TimeUnit.SECONDS).build());
        manager.registerCustomCache(CMS_PAGES, Caffeine.newBuilder()
                .maximumSize(1000).expireAfterWrite(60, TimeUnit.SECONDS).build());
        return manager;
    }
}
