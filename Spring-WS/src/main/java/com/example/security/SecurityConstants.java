package com.example.security;

/**
 * WS-Security Constants - shared between server and client.
 *
 * WHAT IS WS-SECURITY?
 * ====================
 * WS-Security (WSS) is a standard for adding security to SOAP messages.
 * Unlike HTTPS (which secures the transport), WS-Security secures the
 * MESSAGE ITSELF by adding security information to the SOAP header.
 *
 * This class holds the demo credentials used by both the server
 * (for validation) and the client (for sending).
 *
 * COMPARISON WITH JAX-WS:
 * =======================
 * Both projects use the same credentials and the same WS-Security standard.
 * The difference is HOW the credentials are injected and validated:
 *
 * JAX-WS:    Hand-written SOAPHandler classes that build/parse XML manually.
 * Spring-WS: Wss4jSecurityInterceptor configured via properties (no XML code needed).
 *
 * IN PRODUCTION:
 * - Credentials come from a database, LDAP, or identity provider
 * - Passwords should be hashed (password digest) not sent in plain text
 * - Always use HTTPS in addition to WS-Security
 */
public class SecurityConstants {

    // Demo credentials (same as the JAX-WS project for comparison)
    public static final String USERNAME = "alice";
    public static final String PASSWORD = "secret123";
}
