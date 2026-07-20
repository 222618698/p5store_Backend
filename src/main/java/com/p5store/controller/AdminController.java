package com.p5store.controller;

import com.p5store.service.impl.CatalogImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CatalogImportService catalogImportService;

    // Fire-and-forget: kicks off an async import (see CatalogImportService)
    // and returns immediately rather than waiting for ~30k rows to insert.
    // Progress/completion is only visible in the server logs; poll
    // GET /v1/products?size=1 (totalElements) to check when it's done.
    @PostMapping("/catalog-import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void triggerCatalogImport() {
        catalogImportService.importCatalog();
    }
}
