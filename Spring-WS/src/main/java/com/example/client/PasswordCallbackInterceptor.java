package com.example.client;

import com.example.security.ClientPasswordCallbackHandler;
import org.apache.wss4j.common.ConfigurationConstants;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

/**
 * Client interceptor that injects a CallbackHandler into the MessageContext.
 *
 * WHY THIS CLASS EXISTS:
 * ======================
 * Spring-WS's Wss4jSecurityInterceptor only has setSecurementPassword(),
 * which sets ONE password for ALL outgoing security actions (UsernameToken,
 * Signature, Encryption).
 *
 * When using BOTH UsernameToken AND Signature, WSS4J needs TWO different
 * passwords:
 *   - UsernameToken: alice's credential ("secret123")
 *   - Signature:     client private key password ("clientpass")
 *
 * Spring-WS (pre-4.1.0) has NO setSecurementCallbackHandler() or
 * setWsHandlerOption() methods to configure a custom password callback.
 *
 * THE WORKAROUND:
 * ===============
 * WSS4J's WSHandler.getPasswordCallbackHandler() searches for the callback
 * handler in this order:
 *   1. handler options (PW_CALLBACK_REF)  -> not accessible from Spring-WS
 *   2. message context (PW_CALLBACK_REF)  -> WE CAN SET THIS!
 *   3. handler options (PW_CALLBACK_CLASS) -> not accessible from Spring-WS
 *   4. message context (PW_CALLBACK_CLASS) -> we could set this too
 *
 * By registering this interceptor BEFORE the Wss4jSecurityInterceptor in
 * the interceptor chain, we inject a PW_CALLBACK_REF property into the
 * MessageContext. When WSS4J processes the security actions, it finds our
 * CallbackHandler and uses it to get the correct password for each action.
 *
 * HOW WSS4J USES THE CALLBACK:
 * =============================
 *   1. For UsernameToken: calls handler with id="alice", usage=USERNAME_TOKEN
 *      -> our handler returns "secret123"
 *   2. For Signature:     calls handler with id="client", usage=SIGNATURE
 *      -> our handler returns "clientpass"
 *
 * COMPARISON WITH JAX-WS (CXF):
 * ==============================
 * CXF allows setting PW_CALLBACK_CLASS directly as a property:
 *   properties.put(PW_CALLBACK_CLASS, ClientPasswordCallbackHandler.class.getName())
 *
 * Spring-WS requires this workaround because its interceptor doesn't expose
 * the WSS4J handler options directly (until version 4.1.0+).
 */
public class PasswordCallbackInterceptor implements ClientInterceptor {

    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        // Place our CallbackHandler on the message context where WSS4J can find it.
        // WSHandler.getCallbackHandler() checks getProperty(msgContext, PW_CALLBACK_REF)
        // and will use this handler instead of falling back to the single securementPassword.
        messageContext.setProperty(
                ConfigurationConstants.PW_CALLBACK_REF,
                new ClientPasswordCallbackHandler()
        );
        return true; // continue processing the interceptor chain
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        return true; // no action needed on response
    }

    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        return true; // no action needed on fault
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex)
            throws WebServiceClientException {
        // no cleanup needed
    }
}
