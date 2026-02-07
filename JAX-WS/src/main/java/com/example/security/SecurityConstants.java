package com.example.security;

/**
 * WS-Security Constants - shared between server and client.
 *
 * THREE LAYERS OF WS-SECURITY IN THIS DEMO:
 * ==========================================
 *
 * LAYER 1: AUTHENTICATION (UsernameToken)
 *   "Who are you?" - proves identity with username/password.
 *   The username and password are sent in the SOAP header.
 *
 *   <wsse:UsernameToken>
 *     <wsse:Username>alice</wsse:Username>
 *     <wsse:Password>secret123</wsse:Password>
 *   </wsse:UsernameToken>
 *
 * LAYER 2: DIGITAL SIGNATURE
 *   "Was this message tampered with?" - proves message integrity.
 *   The sender signs the message body with their PRIVATE KEY.
 *   The receiver verifies using the sender's PUBLIC KEY (from truststore).
 *
 *   If an intermediary changes even one character of the SOAP body,
 *   the signature verification will FAIL and the message is rejected.
 *
 *   <ds:Signature>
 *     <ds:SignedInfo>...</ds:SignedInfo>            (what was signed)
 *     <ds:SignatureValue>ABC123...</ds:SignatureValue>  (the signature)
 *     <ds:KeyInfo>...</ds:KeyInfo>                 (which key signed it)
 *   </ds:Signature>
 *
 * LAYER 3: ENCRYPTION
 *   "Can intermediaries read this?" - protects confidentiality.
 *   The sender encrypts the SOAP body with the RECEIVER's PUBLIC KEY.
 *   Only the receiver (who has the matching PRIVATE KEY) can decrypt it.
 *
 *   <xenc:EncryptedData>
 *     <xenc:CipherData>
 *       <xenc:CipherValue>...gibberish...</xenc:CipherValue>
 *     </xenc:CipherData>
 *   </xenc:EncryptedData>
 *
 * ORDER OF OPERATIONS:
 * ====================
 *   Client sending:    1. Add UsernameToken  2. Sign  3. Encrypt
 *   Server receiving:  1. Decrypt  2. Verify signature  3. Validate credentials
 *
 *   (Encryption must be last so the signature is also encrypted,
 *    preventing attackers from even seeing WHO signed the message.)
 *
 * KEYSTORES:
 * ==========
 *   client-keystore.jks   = client's private key (for signing)
 *   client-truststore.jks = server's public cert  (for encrypting TO server)
 *   server-keystore.jks   = server's private key  (for decrypting)
 *   server-truststore.jks = client's public cert  (for verifying client signatures)
 */
public class SecurityConstants {

    // --- Authentication credentials ---
    public static final String USERNAME = "alice";
    public static final String PASSWORD = "secret123";

    // --- Keystore aliases (names of keys inside the keystores) ---
    public static final String CLIENT_KEY_ALIAS = "client";
    public static final String SERVER_KEY_ALIAS = "server";

    // --- Keystore passwords ---
    // In production: load from environment variables, HashiCorp Vault, etc.
    public static final String CLIENT_KEY_PASSWORD = "clientpass";
    public static final String SERVER_KEY_PASSWORD = "serverpass";

    // --- Crypto properties files (tell WSS4J where the keystores are) ---
    public static final String CLIENT_CRYPTO_PROPERTIES = "client-crypto.properties";
    public static final String SERVER_CRYPTO_PROPERTIES = "server-crypto.properties";

    // --- WS-Security XML namespace (for hand-written UsernameToken code) ---
    public static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String WSSE_PREFIX = "wsse";
}
