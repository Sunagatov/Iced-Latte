package com.zufar.onlinestore.security.authentication;

import com.zufar.onlinestore.repository.UserDetailsRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserDetailsRepository userDetailsRepository;

	@Override
	public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
		return userDetailsRepository
				.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User doesn't exists"));
	}
}