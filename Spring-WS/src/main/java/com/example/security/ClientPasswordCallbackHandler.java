package com.example.security;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * CLIENT-SIDE Password Callback Handler for WS-Security (Spring-WS).
 *
 * WHY THIS CLASS EXISTS:
 * ======================
 * Spring-WS's Wss4jSecurityInterceptor has setSecurementPassword() which sets
 * ONE password for outgoing messages. But when we use BOTH UsernameToken AND
 * Signature, WSS4J needs TWO different passwords:
 *
 *   1. UsernameToken password: "secret123" (alice's credential)
 *   2. Signing key password:   "clientpass" (client's private key in keystore)
 *
 * If we only set securementPassword("secret123"), WSS4J also tries "secret123"
 * to unlock the private key, which fails with "Cannot recover key".
 *
 * SOLUTION: Provide a CallbackHandler that returns the RIGHT password for
 * each identifier, replacing the default single-password behavior.
 *
 * COMPARISON:
 *   JAX-WS (CXF):  PW_CALLBACK_CLASS = ClientPasswordCallbackHandler.class
 *   Spring-WS:     interceptor.setSecurementCallbackHandler(new ClientPasswordCallbackHandler())
 *
 *   The callback handler code is essentially identical in both projects.
 *   The only difference is how the framework references it.
 */
public class ClientPasswordCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callback;
                String identifier = pc.getIdentifier();

                System.out.println("  [Client Security] Password requested for: '"
                        + identifier + "' (usage=" + pc.getUsage() + ")");

                // UsernameToken: alice's password to send to the server
                if (SecurityConstants.USERNAME.equals(identifier)) {
                    pc.setPassword(SecurityConstants.PASSWORD);
                    System.out.println("  [Client Security] -> Provided user password for '" + identifier + "'");
                }

                // Signature: client's private key password to unlock for signing
                else if (SecurityConstants.CLIENT_KEY_ALIAS.equals(identifier)) {
                    pc.setPassword(SecurityConstants.CLIENT_KEY_PASSWORD);
                    System.out.println("  [Client Security] -> Provided keystore password for '" + identifier + "'");
                }

                else {
                    System.out.println("  [Client Security] -> Unknown identifier: '" + identifier + "'");
                }
            }
        }
    }
}
