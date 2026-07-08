package com.p5store.controller;

import com.p5store.dto.request.ContactMessageRequest;
import com.p5store.dto.response.ContactMessageResponse;
import com.p5store.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/contact-messages")
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactMessageResponse submit(@Valid @RequestBody ContactMessageRequest request) {
        return contactMessageService.submit(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ContactMessageResponse> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return contactMessageService.getAll(pageable);
    }
}
