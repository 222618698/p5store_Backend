package com.p5store.service.impl;

import com.p5store.domain.Address;
import com.p5store.domain.User;
import com.p5store.dto.request.AddressRequest;
import com.p5store.dto.response.AddressResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.AddressRepository;
import com.p5store.repository.UserRepository;
import com.p5store.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public AddressResponse create(Long userId, AddressRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<Address> existing = addressRepository.findByUserId(userId);

        Address address = new Address();
        address.setUser(user);
        address.setStreet(req.street());
        address.setCity(req.city());
        address.setProvince(req.province());
        address.setPostalCode(req.postalCode());
        address.setCountry(req.country());
        try {
            address.setType(req.type() != null
                    ? Address.AddressType.valueOf(req.type().toUpperCase())
                    : Address.AddressType.SHIPPING);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid address type: " + req.type());
        }
        // First address for a user automatically becomes their default.
        address.setDefault(existing.isEmpty() || Boolean.TRUE.equals(req.isDefault()));

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public void delete(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));
        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessException("Address does not belong to this user");
        }
        addressRepository.delete(address);
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getStreet(), a.getCity(), a.getProvince(),
                a.getPostalCode(), a.getCountry(), a.isDefault(), a.getType().name());
    }
}
