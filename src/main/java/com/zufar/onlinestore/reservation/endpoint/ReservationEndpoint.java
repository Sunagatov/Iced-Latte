package com.zufar.onlinestore.reservation.endpoint;

import com.zufar.onlinestore.reservation.api.ReservationApi;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationDto;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.ProductReservation;
import com.zufar.onlinestore.reservation.config.ReservationTimeoutConfiguration;
import com.zufar.onlinestore.reservation.repository.ReservationRepository;
import com.zufar.onlinestore.reservation.service.UserReservationService;
import com.zufar.onlinestore.reservation.validator.IncomingDtoValidator;
import com.zufar.onlinestore.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.nothingReserved;
import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.reserved;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = ReservationEndpoint.RESERVATION_URL)
public class ReservationEndpoint {

    public static final String RESERVATION_URL = "/api/v1/reservation";

    private final IncomingDtoValidator<CreateReservationDto> validator;
    private final ReservationApi reservationApi;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final UserReservationService userReservationService;
    private final ReservationTimeoutConfiguration timeoutConfiguration;

    @GetMapping
    @ResponseBody
    public ResponseEntity<CreatedReservationResponse> findAllReservedProducts(@AuthenticationPrincipal UserDetails userDetails) {
        var userId = userRepository.findUserIdByUsername(userDetails.getUsername());
        log.info("Received the request to get all reserved products for userId = {}", userId);
        var reservationInfo = userReservationService.getReservationInfoForUpdate(userId);
        var reservationExpiredAt = reservationInfo.createdAt().plus(timeoutConfiguration.defaultTimeout());
        var reservations = reservationRepository.findAllByReservationId(reservationInfo.reservationId());

        if (reservations.isEmpty()) {
            return ResponseEntity.ok(nothingReserved());
        }
        var productReservations = reservations.stream()
                .map(reservation ->
                        new ProductReservation(
                                reservation.getWarehouseItemId(),
                                reservation.getReservedQuantity()
                        )
                ).toList();
        return ResponseEntity.ok(reserved(productReservations, reservationExpiredAt));
    }

    @PutMapping
    @ResponseBody
    public ResponseEntity<CreatedReservationResponse> createReservation(
            @RequestBody @Valid CreateReservationDto reservationDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var valid = validator.isValid(reservationDto);
        if (!valid) {
            return ResponseEntity.badRequest().body(nothingReserved());
        }
        var userId = userRepository.findUserIdByUsername(userDetails.getUsername());
        log.info("Received the request to reserve products for userId = {}", userId);
        var createdReservationResponse = reservationApi.createReservation(
                new CreateReservationRequest(userId, reservationDto.reservations())
        );
        return ResponseEntity.ok(createdReservationResponse);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ConfirmedReservationResponse> confirmReservation(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var userId = userRepository.findUserIdByUsername(userDetails.getUsername());
        log.info("Received the request to confirm reservation of products for userId = {}", userId);
        var confirmedReservationResponse = reservationApi.confirmReservation(new ConfirmReservationRequest(userId));
        return ResponseEntity.ok(confirmedReservationResponse);
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<CancelledReservationResponse> cancelReservation(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var userId = userRepository.findUserIdByUsername(userDetails.getUsername());
        log.info("Received the request to cancel reservation of products for userId = {}", userId);
        var cancelledReservationResponse = reservationApi.cancelReservation(new CancelReservationRequest(userId));
        return ResponseEntity.ok(cancelledReservationResponse);
    }
}
