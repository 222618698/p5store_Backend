package com.p5store.dto.request;

import java.util.List;

public record CatalogImportPayload(
    List<CatalogImportCategory> categories,
    List<CatalogImportProduct> products
) {}
