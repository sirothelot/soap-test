package com.example.security;

/**
 * WS-Security Constants - shared between server and client.
 *
 * THREE LAYERS OF WS-SECURITY IN THIS DEMO:
 * ==========================================
 *
 * LAYER 1: AUTHENTICATION (UsernameToken)
 *   "Who are you?" - proves identity with username/password.
 *
 * LAYER 2: DIGITAL SIGNATURE
 *   "Was this message tampered with?" - proves message integrity.
 *   Uses the sender's PRIVATE KEY to sign, receiver's TRUSTSTORE to verify.
 *
 * LAYER 3: ENCRYPTION
 *   "Can intermediaries read this?" - protects confidentiality.
 *   Uses the receiver's PUBLIC KEY to encrypt, receiver's PRIVATE KEY to decrypt.
 *
 * COMPARISON WITH JAX-WS:
 * =======================
 * Both projects use the SAME constants, keystores, and WSS4J library.
 * The difference is HOW the security is wired:
 *   JAX-WS:    SOAPHandler classes call WSS4J API directly (~150 lines)
 *   Spring-WS: Wss4jSecurityInterceptor configured via properties (~15 lines)
 */
public class SecurityConstants {

    // --- Authentication credentials ---
    public static final String USERNAME = "alice";
    public static final String PASSWORD = "secret123";

    // --- Keystore aliases (names of keys inside the keystores) ---
    public static final String CLIENT_KEY_ALIAS = "client";
    public static final String SERVER_KEY_ALIAS = "server";

    // --- Keystore passwords ---
    public static final String CLIENT_KEY_PASSWORD = "clientpass";
    public static final String SERVER_KEY_PASSWORD = "serverpass";

    // --- Crypto properties files ---
    public static final String CLIENT_CRYPTO_PROPERTIES = "client-crypto.properties";
    public static final String SERVER_CRYPTO_PROPERTIES = "server-crypto.properties";
}
