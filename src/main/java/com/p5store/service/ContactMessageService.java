package com.p5store.service;

import com.p5store.dto.request.ContactMessageRequest;
import com.p5store.dto.response.ContactMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactMessageService {
    ContactMessageResponse submit(ContactMessageRequest request);
    Page<ContactMessageResponse> getAll(Pageable pageable);
}
