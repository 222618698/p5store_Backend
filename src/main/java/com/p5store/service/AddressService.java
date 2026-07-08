package com.p5store.service;

import com.p5store.dto.request.AddressRequest;
import com.p5store.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {
    AddressResponse create(Long userId, AddressRequest request);
    List<AddressResponse> list(Long userId);
    void delete(Long userId, Long addressId);
}
