package com.p5store.service.impl;

import com.p5store.domain.ContactMessage;
import com.p5store.dto.request.ContactMessageRequest;
import com.p5store.dto.response.ContactMessageResponse;
import com.p5store.repository.ContactMessageRepository;
import com.p5store.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;

    @Override
    @Transactional
    public ContactMessageResponse submit(ContactMessageRequest req) {
        ContactMessage message = new ContactMessage();
        message.setFullName(req.fullName());
        message.setEmail(req.email());
        message.setPhone(req.phone());
        message.setCompany(req.company());
        message.setMessage(req.message());
        message = contactMessageRepository.save(message);

        return new ContactMessageResponse(
                message.getId(), message.getFullName(), message.getEmail(),
                message.getPhone(), message.getCompany(), message.getMessage(),
                message.getCreatedAt());
    }

    @Override
    public Page<ContactMessageResponse> getAll(Pageable pageable) {
        return contactMessageRepository.findAll(pageable).map(message -> new ContactMessageResponse(
                message.getId(), message.getFullName(), message.getEmail(),
                message.getPhone(), message.getCompany(), message.getMessage(),
                message.getCreatedAt()));
    }
}
