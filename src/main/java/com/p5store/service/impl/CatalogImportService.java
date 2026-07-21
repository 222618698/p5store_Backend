package com.p5store.service.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.dto.request.CatalogImportCategory;
import com.p5store.dto.request.CatalogImportProduct;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One-time, admin-triggered replacement of the entire product catalog with a
 * pre-extracted dataset hosted at {@code app.catalog-import.url}. Runs off the
 * request thread since a full ~30k-row import would otherwise risk exceeding
 * the HTTP proxy timeout.
 *
 * The import file is stream-parsed (rather than deserialized into one big
 * {@code List<CatalogImportProduct>}) and products are committed in small
 * independent transactions (see {@link CatalogImportWorker#importBatch}).
 * On the 512MB free-tier instance this runs on, materializing all ~30k
 * products in memory at once, or holding them all in a single hours-long
 * transaction, previously caused the process to crash partway through with
 * nothing committed. Streaming keeps peak memory bounded to one batch, and
 * per-batch commits mean a crash only loses the batch in flight.
 *
 * Always wipes and re-creates the catalog from scratch (see
 * {@link CatalogImportWorker#wipeExistingCatalog()}) rather than trying to
 * skip if "already imported" — a name-based idempotency check is unreliable
 * here since the pre-existing demo taxonomy can coincidentally share category
 * names (e.g. "Beauty", "Fashion") with the real catalog being imported.
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

        log.info("Fetching catalog import file from {}", importUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(importUrl)).GET().build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300) {
                log.error("Catalog import aborted: fetch returned HTTP {}", response.statusCode());
                return;
            }

            worker.wipeExistingCatalog();

            try (InputStream body = response.body();
                 JsonParser parser = objectMapper.getFactory().createParser(body)) {

                Map<String, Long> categoryIdByName = null;
                int imported = 0;

                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new IllegalStateException("Expected import file to start with a JSON object");
                }
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    String field = parser.currentName();
                    parser.nextToken();
                    if ("categories".equals(field)) {
                        List<CatalogImportCategory> categories = objectMapper.readValue(parser,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, CatalogImportCategory.class));
                        categoryIdByName = worker.createCategories(categories);
                    } else if ("products".equals(field)) {
                        if (categoryIdByName == null) {
                            throw new IllegalStateException("Import file must list 'categories' before 'products'");
                        }
                        if (parser.currentToken() != JsonToken.START_ARRAY) {
                            throw new IllegalStateException("Expected 'products' to be a JSON array");
                        }
                        List<CatalogImportProduct> batch = new ArrayList<>(CatalogImportWorker.BATCH_SIZE);
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            batch.add(objectMapper.readValue(parser, CatalogImportProduct.class));
                            if (batch.size() == CatalogImportWorker.BATCH_SIZE) {
                                imported += worker.importBatch(batch, categoryIdByName);
                                log.info("Imported {} products...", imported);
                                batch.clear();
                            }
                        }
                        if (!batch.isEmpty()) {
                            imported += worker.importBatch(batch, categoryIdByName);
                        }
                    } else {
                        parser.skipChildren();
                    }
                }

                log.info("Catalog import complete: {} categories, {} products",
                        categoryIdByName == null ? 0 : categoryIdByName.size(), imported);
            }
        } catch (Exception e) {
            log.error("Catalog import aborted: failed to fetch/parse import file", e);
        }
    }
}
