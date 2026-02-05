package com.github.dimitryivaniuta.booking.service;

import com.github.dimitryivaniuta.booking.api.dto.ApartmentCreateRequest;
import com.github.dimitryivaniuta.booking.domain.Apartment;
import com.github.dimitryivaniuta.booking.repo.ApartmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for apartments.
 */
@Service
@RequiredArgsConstructor
public class ApartmentService {

  private final ApartmentRepository apartmentRepository;

  /**
   * Creates a new apartment.
   *
   * @param req request
   * @return created apartment
   */
  @Transactional
  public Apartment create(ApartmentCreateRequest req) {
    Apartment a = new Apartment(req.name(), req.city(), req.capacity());
    return apartmentRepository.save(a);
  }

  /**
   * Loads an apartment by id.
   *
   * @param id apartment id
   * @return apartment
   */
  @Transactional(readOnly = true)
  public Apartment get(UUID id) {
    return apartmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Apartment not found: " + id));
  }
}
