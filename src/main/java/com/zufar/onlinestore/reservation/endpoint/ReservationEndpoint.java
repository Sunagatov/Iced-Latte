package com.zufar.onlinestore.reservation.endpoint;

import com.zufar.onlinestore.reservation.api.ReservationApi;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationDto;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import com.zufar.onlinestore.reservation.validator.IncomingDtoValidator;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PutMapping
    @ResponseBody
    public ResponseEntity<CreatedReservationResponse> reserve(@RequestBody @Valid CreateReservationDto reservationDto) {
        var valid = validator.isValid(reservationDto);
        if (!valid) {
            return ResponseEntity.badRequest().body(failedReservation());
        }
        UUID reservationId = getActiveReservationIdForLoggedInUser();
        log.info("Received the request to reserve products with reservationId = {}", reservationId);
        var reservationResponse = reservationApi.createReservation(
                new CreateReservationRequest(reservationId, reservationDto.productReservations())
        );
        return ResponseEntity.ok(reservationResponse);
    }

    private UUID getActiveReservationIdForLoggedInUser() {
        UserDetails user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        /* TODO:
         *   var reservationId = findActiveReservationId(user); // active means that it is last user reservation that is not completed (in status CREATED)
         *   if (reservationId == null){
         *     reservationId = UUID.randomUUID();
         *     bindNewReservationIdToUser(reservationId, user);
         *   }
         *   return reservationId;
         * */
        return UUID.fromString("1e5b295f-8f50-4425-90e9-8b590a27b400"); // TODO: remove stub
    }
}
