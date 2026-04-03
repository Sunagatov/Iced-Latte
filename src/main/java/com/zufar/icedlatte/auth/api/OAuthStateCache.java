package com.zufar.icedlatte.auth.api;

public interface OAuthStateCache {
    void store(String nonce, String callbackBase);
    /** Returns the callbackBase for the nonce, or null if absent/expired. Consumes the entry. */
    String consume(String nonce);
}
