package com.zufar.icedlatte.user.api;

public interface UserRegistrationApi {

    boolean existsByEmail(String email);

    UserAuthenticationSnapshot registerPasswordUser(String firstName,
                                                    String lastName,
                                                    String email,
                                                    String encodedPassword);

    UserAuthenticationSnapshot registerOAuthUser(String firstName,
                                                 String lastName,
                                                 String email,
                                                 String encodedPassword);
}
