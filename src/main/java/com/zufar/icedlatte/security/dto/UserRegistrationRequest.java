package com.zufar.icedlatte.security.dto;

import com.zufar.icedlatte.common.validation.email.UniqueEmail;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserRegistrationRequest(

        @NotBlank(message = "First name is the mandatory attribute")
        @Size(min = 2, max = 128, message = "First name should have a length between 2 and 128 characters")
        @Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?!.*?--)[A-Za-z\\s-]*$", message = "First name can only contain Latin letters, spaces, and hyphens")
        String firstName,

        @NotBlank(message = "Last name is the mandatory attribute")
        @Size(min = 2, max = 128, message = "Last name should have a length between 2 and 128 characters")
        @Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?!.*?--)[A-Za-z\\s-]*$", message = "Last name can only contain Latin letters, spaces, and hyphens")
        String lastName,

        LocalDate birthDate,

        String phoneNumber,

        @UniqueEmail
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email should have a length between 2 and 128 characters")
        String email,

        @NotBlank(message = "Password is the mandatory attribute")
        @Size(min = 8, max = 128, message = "Password should have a length between 8 and 128 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$", message = "Password must be at least 8 characters long and contain at least one letter, one digit, and may include special characters @$!%*?&")
        String password,

        @Valid
        AddressDto addressDto
) {

    public UserRegistrationRequest(String firstName,
                                   String lastName,
                                   String email,
                                   String password) {
        this(firstName, lastName, null, null, email, password, null);
    }
}
