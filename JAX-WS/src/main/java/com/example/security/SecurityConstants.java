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
 * WHY SECURE THE MESSAGE (NOT JUST THE TRANSPORT)?
 * ================================================
 * 1. HTTPS protects data in transit between two points.
 *    But SOAP messages can travel through intermediaries (proxies, ESBs).
 *    WS-Security protects the message end-to-end, even through middlemen.
 *
 * 2. With WS-Security, you can:
 *    - Authenticate: Prove WHO is calling (UsernameToken, certificates)
 *    - Sign: Prove the message HASN'T BEEN TAMPERED with
 *    - Encrypt: Hide SENSITIVE PARTS of the message
 *    - Timestamp: Prevent REPLAY attacks (reusing old messages)
 *
 * USERNAMETOKEN (what this demo uses):
 * ====================================
 * The simplest WS-Security mechanism. The client adds a username and password
 * to the SOAP header. The server validates them before processing the request.
 *
 * A secured SOAP message looks like this:
 *
 *   <soap:Envelope>
 *     <soap:Header>
 *       <wsse:Security>                              <-- WS-Security header
 *         <wsse:UsernameToken>                       <-- Authentication info
 *           <wsse:Username>alice</wsse:Username>     <-- Who is calling
 *           <wsse:Password>secret123</wsse:Password> <-- Proof of identity
 *         </wsse:UsernameToken>
 *       </wsse:Security>
 *     </soap:Header>
 *     <soap:Body>
 *       <add><a>10</a><b>5</b></add>                 <-- The actual request
 *     </soap:Body>
 *   </soap:Envelope>
 *
 * IN PRODUCTION:
 * - Always use HTTPS + UsernameToken (never send passwords in plain text)
 * - Consider password digest (hashed) instead of plain text
 * - For higher security, use X.509 certificates instead of passwords
 */
public class SecurityConstants {

    // WS-Security XML namespace URIs
    // These identify the WS-Security elements in the SOAP header
    public static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String WSSE_PREFIX = "wsse";

    // Demo credentials (in production, these come from a database, LDAP, etc.)
    public static final String USERNAME = "alice";
    public static final String PASSWORD = "secret123";
}
