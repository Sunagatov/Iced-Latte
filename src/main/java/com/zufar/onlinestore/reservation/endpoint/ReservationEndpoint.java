package com.zufar.onlinestore.reservation.endpoint;

import com.zufar.onlinestore.reservation.api.ReservationApi;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationDto;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import com.zufar.onlinestore.reservation.service.UserReservationHistoryService;
import com.zufar.onlinestore.reservation.validator.IncomingDtoValidator;
import com.zufar.onlinestore.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.failedReservation;

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

    @PutMapping
    @ResponseBody
    public ResponseEntity<CreatedReservationResponse> reserve(@RequestBody @Valid CreateReservationDto reservationDto,
                                                              @AuthenticationPrincipal UserDetails userDetails
    ) {
        var valid = validator.isValid(reservationDto);
        if (!valid) {
            return ResponseEntity.badRequest().body(failedReservation());
        }
        UUID userId = userRepository.findUserIdByUsername(userDetails.getUsername());
        log.info("Received the request to reserve products for userId = {}", userId);
        var reservationResponse = reservationApi.createReservation(
                new CreateReservationRequest(userId, reservationDto.reservations())
        );
        return ResponseEntity.ok(reservationResponse);
    }
}
