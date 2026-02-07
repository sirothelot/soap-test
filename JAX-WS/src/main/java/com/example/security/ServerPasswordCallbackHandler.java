package com.example.security;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * SERVER-SIDE Password Callback Handler for WS-Security.
 *
 * WHAT THIS CLASS DOES:
 * =====================
 * When WSS4J processes an incoming secured SOAP message on the server, it needs
 * passwords for several operations. It asks this handler for each one.
 *
 * WHEN IS THIS CALLED AND WHY:
 * ============================
 * WSS4J calls this handler with different "usage" types:
 *
 *   1. USERNAME_TOKEN (Usage = USERNAME_TOKEN)
 *      Purpose: Validate the client's username/password.
 *      What happens: WSS4J extracts the username from the <wsse:UsernameToken>
 *                    header. It calls us with that username. We return the
 *                    EXPECTED password. WSS4J compares it with the received one.
 *      Example:   identifier="alice" -> we return "secret123"
 *
 *   2. DECRYPT (Usage = DECRYPT)
 *      Purpose: Provide the password for the server's private key so WSS4J
 *               can decrypt the message body.
 *      What happens: The client encrypted the body with our PUBLIC key.
 *                    To decrypt, WSS4J needs our PRIVATE key, which is
 *                    protected by a password in the keystore.
 *      Example:   identifier="server" -> we return "serverpass"
 *
 *   3. SIGNATURE (Usage = SIGNATURE)
 *      Purpose: Provide the password for verifying signatures (rarely needed
 *               on server side since public keys aren't password-protected,
 *               but WSS4J may ask for it).
 *
 * HOW CXF USES THIS:
 * ===================
 * In CXF, we configure this class name as a property:
 *   props.put(WSHandlerConstants.PW_CALLBACK_CLASS,
 *             ServerPasswordCallbackHandler.class.getName());
 *
 * CXF creates an instance and calls handle() whenever it needs a password.
 *
 * COMPARISON WITH SPRING-WS:
 * ==========================
 * Spring-WS uses the same concept but passes it differently:
 *   interceptor.setValidationCallbackHandler(callbackHandler);
 * The CallbackHandler code is identical.
 */
public class ServerPasswordCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callback;

                // The "identifier" is the username or key alias that WSS4J
                // is asking about. We check it and return the right password.
                String identifier = pc.getIdentifier();

                System.out.println("  [Server Security] Password requested for: '"
                        + identifier + "' (usage=" + pc.getUsage() + ")");

                // CASE 1: UsernameToken validation
                // WSS4J says: "I received a UsernameToken with username='alice'.
                //              What password should 'alice' have?"
                // We set the expected password. WSS4J compares it with what was sent.
                if (SecurityConstants.USERNAME.equals(identifier)) {
                    pc.setPassword(SecurityConstants.PASSWORD);
                    System.out.println("  [Server Security] -> Provided user password for '" + identifier + "'");
                }

                // CASE 2: Server's private key password (for decryption)
                // WSS4J says: "I need to decrypt this message using the 'server'
                //              private key. What's the keystore password for it?"
                else if (SecurityConstants.SERVER_KEY_ALIAS.equals(identifier)) {
                    pc.setPassword(SecurityConstants.SERVER_KEY_PASSWORD);
                    System.out.println("  [Server Security] -> Provided keystore password for '" + identifier + "'");
                }

                else {
                    System.out.println("  [Server Security] -> Unknown identifier: '" + identifier + "'");
                }
            }
        }
    }
}
