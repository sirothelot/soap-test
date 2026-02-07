package com.example.security;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * SERVER-SIDE Password Callback Handler for WS-Security (Spring-WS).
 *
 * WHY THIS CLASS EXISTS:
 * ======================
 * When using all three WS-Security layers (UsernameToken + Signature + Encryption),
 * WSS4J needs passwords for MULTIPLE purposes:
 *
 *   1. UsernameToken validation: compare the client's password against the expected one
 *   2. Private key access: unlock the server's private key for DECRYPTION
 *
 * Spring-WS provides SimplePasswordValidationCallbackHandler, but it only handles
 * UsernameToken validation (usage=USERNAME_TOKEN). It does NOT support the
 * DECRYPT usage type needed to unlock the server's private key.
 *
 * Spring-WS also provides KeyStoreCallbackHandler, which handles private key
 * access but doesn't do UsernameToken validation.
 *
 * SOLUTION: A single CallbackHandler that handles BOTH use cases, exactly like
 * the JAX-WS ServerPasswordCallbackHandler.
 *
 * HOW WSS4J CALLS THIS:
 * =====================
 *   Processing order (right-to-left from "UsernameToken Signature Encrypt"):
 *     1. DECRYPT:        id="server",  usage=DECRYPT  -> return "serverpass"
 *     2. VERIFY:         (uses truststore, no callback needed)
 *     3. USERNAME_TOKEN: id="alice",   usage=USERNAME_TOKEN -> return "secret123"
 *
 * COMPARISON WITH JAX-WS:
 * =======================
 * The callback handler code is essentially IDENTICAL in both projects.
 * The only difference is how the framework references it:
 *   JAX-WS (CXF):  WSS4JInInterceptor property -> PW_CALLBACK_REF
 *   Spring-WS:     interceptor.setValidationCallbackHandler(handler)
 */
public class ServerPasswordCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callback;
                String identifier = pc.getIdentifier();
                int usage = pc.getUsage();

                System.out.println("  [Server Security] Password requested for: '"
                        + identifier + "' (usage=" + usage + ")");

                switch (usage) {
                    // UsernameToken validation: set the EXPECTED password
                    // WSS4J compares this against what the client sent
                    case WSPasswordCallback.USERNAME_TOKEN:
                        if (SecurityConstants.USERNAME.equals(identifier)) {
                            pc.setPassword(SecurityConstants.PASSWORD);
                            System.out.println("  [Server Security] -> Provided user password for '" + identifier + "'");
                        } else {
                            System.out.println("  [Server Security] -> Unknown user: '" + identifier + "'");
                        }
                        break;

                    // Decryption: provide the server's private key password
                    // WSS4J needs this to unlock the private key for decrypting
                    case WSPasswordCallback.DECRYPT:
                        if (SecurityConstants.SERVER_KEY_ALIAS.equals(identifier)) {
                            pc.setPassword(SecurityConstants.SERVER_KEY_PASSWORD);
                            System.out.println("  [Server Security] -> Provided decryption key password for '" + identifier + "'");
                        } else {
                            System.out.println("  [Server Security] -> Unknown key alias: '" + identifier + "'");
                        }
                        break;

                    default:
                        System.out.println("  [Server Security] -> Unhandled usage type: " + usage
                                + " for '" + identifier + "'");
                        break;
                }
            }
        }
    }
}
