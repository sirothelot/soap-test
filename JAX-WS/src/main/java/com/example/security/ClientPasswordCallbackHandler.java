package com.example.security;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * CLIENT-SIDE Password Callback Handler for WS-Security.
 *
 * WHAT THIS CLASS DOES:
 * =====================
 * When WSS4J builds an outgoing secured SOAP message on the client, it needs
 * passwords for several operations. It asks this handler for each one.
 *
 * WHEN IS THIS CALLED AND WHY:
 * ============================
 * WSS4J calls this handler with different identifiers:
 *
 *   1. USERNAME = "alice" (for UsernameToken)
 *      Purpose: Get the password to include in the <wsse:UsernameToken> header.
 *      What happens: WSS4J says "I need alice's password to send to the server."
 *                    We return "secret123" which gets included in the SOAP header.
 *
 *   2. KEY_ALIAS = "client" (for Signature)
 *      Purpose: Get the password to unlock the client's PRIVATE key in the keystore.
 *      What happens: WSS4J says "I need to sign this message with the 'client'
 *                    private key. What's the keystore password for that key?"
 *                    We return "clientpass" so WSS4J can read the private key
 *                    from client-keystore.jks and create the digital signature.
 *
 * NOTE: Encryption does NOT need a password callback because:
 *   - Encryption uses the SERVER's PUBLIC key (from client-truststore.jks)
 *   - Public keys are not password-protected in truststores
 *   - WSS4J reads the server's public cert directly, no password needed
 *
 * HOW CXF USES THIS:
 * ===================
 * In CXF, we configure this class name as a property:
 *   props.put(WSHandlerConstants.PW_CALLBACK_CLASS,
 *             ClientPasswordCallbackHandler.class.getName());
 *
 * CXF creates an instance and calls handle() whenever it needs a password.
 *
 * COMPARISON WITH SPRING-WS:
 * ==========================
 * Spring-WS uses the same concept:
 *   interceptor.setSecurementCallbackHandler(callbackHandler);
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

                // CASE 1: UsernameToken password
                // WSS4J says: "I'm building a UsernameToken for 'alice'.
                //              What password should I include?"
                if (SecurityConstants.USERNAME.equals(identifier)) {
                    pc.setPassword(SecurityConstants.PASSWORD);
                    System.out.println("  [Client Security] -> Provided user password for '" + identifier + "'");
                }

                // CASE 2: Client signing key password
                // WSS4J says: "I need to sign this message with the 'client' key.
                //              What's the keystore password for that private key?"
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
