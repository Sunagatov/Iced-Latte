package com.zufar.onlinestore.repository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserDetailsRepository {

	private final Map<String, UserDetails> userDetails = new HashMap<>();

	public Optional<UserDetails> findByUsername(final String username) {
		return Optional.ofNullable(userDetails.get(username));
	}

	public void save(final UserDetails userDetails) {
		this.userDetails.put(userDetails.getUsername(), userDetails);
	}
}
