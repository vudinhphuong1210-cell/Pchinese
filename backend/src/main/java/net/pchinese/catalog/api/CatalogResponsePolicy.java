package net.pchinese.catalog.api;

import org.springframework.http.CacheControl;

import java.util.concurrent.TimeUnit;

public final class CatalogResponsePolicy {

    private CatalogResponsePolicy() {
    }

    public static CacheControl publicCache() {
        return CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic();
    }
}
