package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.HouseholdMemberResponse;
import org.conrad.residentservice.dto.ResidentBoardUpdateRequest;
import org.conrad.residentservice.dto.ResidentCreateRequest;
import org.conrad.residentservice.dto.ResidentSummaryResponse;
import org.conrad.residentservice.dto.ResidentUpdateRequest;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class ResidentService {

    private static final Logger log = LoggerFactory.getLogger(ResidentService.class);
    private static final int MAX_PHONE_LENGTH = 20;

    private final ResidentRepository residentRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;

    public ResidentService(ResidentRepository residentRepository,
                           ApartmentRepository apartmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.residentRepository = residentRepository;
        this.apartmentRepository = apartmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void create(String actor, ResidentCreateRequest request) {
        Apartment apartment = findApartment(request.apartmentId());

        Resident resident = Resident.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .manager(request.manager())
                .apartment(apartment)
                .build();

        residentRepository.save(resident);

        log.info("Board member (residentId={}) created new resident email={}, manager={}, apartmentId={}",
                actor, resident.getEmail(), resident.isManager(), request.apartmentId());
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberResponse> household(Long residentId) {
        Resident me = findActiveResident(residentId);
        return residentRepository.findByApartmentIdAndActiveTrue(me.getApartment().getId()).stream()
                .sorted(Comparator.comparing(Resident::getLastName).thenComparing(Resident::getFirstName))
                .map(r -> new HouseholdMemberResponse(r.getId(), r.getFirstName(), r.getLastName()))
                .toList();
    }

    @Transactional
    public ResidentSummaryResponse updateOwnContact(Long residentId, ResidentUpdateRequest request) {
        Resident me = findActiveResident(residentId);
        applyEmail(me, request.email());
        applyPhone(me, request.phone());
        residentRepository.save(me);
        return toSummary(me);
    }

    @Transactional
    public ResidentSummaryResponse update(String actor, Long id, ResidentBoardUpdateRequest request) {
        Resident target = findResident(id);

        if (isActor(actor, id) && (Boolean.FALSE.equals(request.manager()) || Boolean.FALSE.equals(request.active()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A board member cannot demote or deactivate themselves");
        }

        if (request.firstName() != null) {
            target.setFirstName(requireNotBlank(request.firstName(), "firstName"));
        }
        if (request.lastName() != null) {
            target.setLastName(requireNotBlank(request.lastName(), "lastName"));
        }
        applyEmail(target, request.email());
        applyPhone(target, request.phone());
        if (request.manager() != null) {
            target.setManager(request.manager());
        }
        if (request.active() != null) {
            target.setActive(request.active());
        }
        if (request.apartmentId() != null) {
            target.setApartment(findApartment(request.apartmentId()));
        }

        residentRepository.save(target);
        log.info("Board member (residentId={}) updated residentId={}", actor, id);
        return toSummary(target);
    }

    @Transactional
    public void remove(String actor, Long id) {
        if (isActor(actor, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A board member cannot remove themselves");
        }

        Resident target = findResident(id);
        target.setActive(false);
        residentRepository.save(target);

        log.info("Board member (residentId={}) removed (deactivated) residentId={}", actor, id);
    }

    @Transactional(readOnly = true)
    public List<ResidentSummaryResponse> search(String query, boolean includeInactive) {
        return residentRepository.search(query.trim(), includeInactive).stream()
                .map(this::toSummary)
                .toList();
    }

    private Resident findResident(Long id) {
        return residentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resident not found"));
    }

    private Resident findActiveResident(Long id) {
        Resident resident = findResident(id);
        if (!resident.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        }
        return resident;
    }

    private Apartment findApartment(Long id) {
        return apartmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"));
    }

    private boolean isActor(String actor, Long id) {
        return String.valueOf(id).equals(actor);
    }

    private void applyEmail(Resident resident, String newEmail) {
        if (newEmail == null) {
            return;
        }
        String email = requireNotBlank(newEmail, "email");
        if (!email.equals(resident.getEmail())) {
            if (residentRepository.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
            }
            resident.setEmail(email);
        }
    }

    private void applyPhone(Resident resident, String newPhone) {
        if (newPhone == null) {
            return;
        }
        String phone = newPhone.trim();
        if (phone.length() > MAX_PHONE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "phone must be at most " + MAX_PHONE_LENGTH + " characters");
        }
        resident.setPhone(phone.isEmpty() ? null : phone);
    }

    private String requireNotBlank(String value, String field) {
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must not be blank");
        }
        return value.trim();
    }

    private ResidentSummaryResponse toSummary(Resident resident) {
        Apartment apartment = resident.getApartment();
        return new ResidentSummaryResponse(
                resident.getId(),
                resident.getFirstName(),
                resident.getLastName(),
                resident.getEmail(),
                resident.getPhone(),
                resident.isManager(),
                resident.isActive(),
                apartment.getId(),
                apartment.getApartmentNumber()
        );
    }
}
