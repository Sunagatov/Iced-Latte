package com.zufar.icedlatte.security.oauth.login;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;

@Component
public class OAuthProviderClientRegistry {

    private final Map<OAuthProvider, OAuthProviderClient> clients;

    public OAuthProviderClientRegistry(List<OAuthProviderClient> providerClients) {
        EnumMap<OAuthProvider, OAuthProviderClient> registeredClients = new EnumMap<>(OAuthProvider.class);
        for (OAuthProviderClient client : providerClients) {
            OAuthProvider provider = client.provider();
            OAuthProviderClient previous = registeredClients.put(provider, client);
            if (previous != null) {
                throw new IllegalStateException("Multiple OAuth clients registered for provider: " + provider.id());
            }
        }
        this.clients = Map.copyOf(registeredClients);
    }

    public Optional<OAuthProviderClient> findClient(OAuthProvider provider) {
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(clients.get(provider));
    }
}
