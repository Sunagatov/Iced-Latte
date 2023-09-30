package com.zufar.onlinestore.security.dto;

import com.zufar.onlinestore.common.validation.email.UniqueEmail;
import com.zufar.onlinestore.common.validation.username.UniqueUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;


public record UserRegistrationRequest(

        @NotBlank(message = "First name is the mandatory attribute")
        @Size(min = 2, max = 128, message = "First name should have a length between 2 and 128 characters")
        @Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?!.*?--)[A-Za-z\\s-]*$", message = "First name can only contain Latin letters, spaces, and hyphens")
        String firstName,

        @NotBlank(message = "Last name is the mandatory attribute")
        @Size(min = 2, max = 128, message = "Last name should have a length between 2 and 128 characters")
        @Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?!.*?--)[A-Za-z\\s-]*$", message = "Last name can only contain Latin letters, spaces, and hyphens")
        String lastName,

        @UniqueUsername(message = "Username must be unique")
        @NotBlank(message = "Username is the mandatory attribute")
        @Size(min = 2, max = 128, message = "Username should have a length between 2 and 128 characters")
        @Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?!.*?--)[A-Za-z0-9@._\\-@$!%*?&]*$", message = "Username can only contain latin letters, digits, and may include special characters @$!%*?&")
        String username,

        @UniqueEmail(message = "Email must be unique")
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email should have a length between 2 and 128 characters")
        String email,

        @NotBlank(message = "Password is the mandatory attribute")
        @Size(min = 8, max = 128, message = "Password should have a length between 8 and 128 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$", message = "Password must be at least 8 characters long and contain at least one letter, one digit, and may include special characters @$!%*?&")
        String password
) {
}
