package com.p5store.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.dto.request.CatalogImportPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * One-time, admin-triggered replacement of the entire product catalog with a
 * pre-extracted dataset hosted at {@code app.catalog-import.url}. Runs off the
 * request thread since a full ~30k-row import would otherwise risk exceeding
 * the HTTP proxy timeout.
 *
 * Always wipes and re-creates the catalog from scratch (see
 * {@link CatalogImportWorker#wipeExistingCatalog()}) rather than trying to
 * skip if "already imported" — a name-based idempotency check is unreliable
 * here since the pre-existing demo taxonomy can coincidentally share category
 * names (e.g. "Beauty", "Fashion") with the real catalog being imported.
 * Re-running this is safe, just redundant work, and it's admin-triggered
 * only, so accidental repeated runs aren't a real concern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogImportService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final CatalogImportWorker worker;
    private final ObjectMapper objectMapper;

    @Value("${app.catalog-import.url:}")
    private String importUrl;

    @Async
    public void importCatalog() {
        if (importUrl.isBlank()) {
            log.error("Catalog import aborted: app.catalog-import.url / APP_CATALOG_IMPORT_URL is not set");
            return;
        }

        CatalogImportPayload payload;
        try {
            log.info("Fetching catalog import file from {}", importUrl);
            HttpRequest request = HttpRequest.newBuilder(URI.create(importUrl)).GET().build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300) {
                log.error("Catalog import aborted: fetch returned HTTP {}", response.statusCode());
                return;
            }
            payload = objectMapper.readValue(response.body(), CatalogImportPayload.class);
        } catch (Exception e) {
            log.error("Catalog import aborted: failed to fetch/parse import file", e);
            return;
        }

        worker.wipeExistingCatalog();
        Map<String, Long> categoryIdByName = worker.createCategories(payload.categories());
        int imported = worker.importProducts(payload.products(), categoryIdByName);

        log.info("Catalog import complete: {} categories, {} products", categoryIdByName.size(), imported);
    }
}
