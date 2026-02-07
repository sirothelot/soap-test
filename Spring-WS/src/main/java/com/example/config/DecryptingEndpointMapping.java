package com.example.config;

import org.springframework.core.Ordered;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.EndpointInvocationChain;
import org.springframework.ws.server.EndpointMapping;
import org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;

/**
 * Custom EndpointMapping that decrypts the SOAP message BEFORE endpoint resolution.
 *
 * ============================================================================
 *  WHY THIS CLASS EXISTS - THE ENCRYPTION + DISPATCH PROBLEM
 * ============================================================================
 *
 * Spring-WS uses a "resolve endpoint, THEN run interceptors" architecture:
 *
 *   1. MessageDispatcher.dispatch() calls getEndpoint(messageContext)
 *   2. getEndpoint() iterates all EndpointMapping beans looking for a match
 *   3. PayloadRootAnnotationMethodEndpointMapping looks at the SOAP body's
 *      root element name (e.g. <addRequest>) to find the @PayloadRoot match
 *   4. If found, it returns an EndpointInvocationChain WITH interceptors
 *   5. ONLY THEN does dispatch() run interceptor.handleRequest()
 *
 * With encryption, step 3 fails because the SOAP body contains:
 *   <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
 *     ... binary cipher text ...
 *   </xenc:EncryptedData>
 *
 * Instead of <addRequest>. So no @PayloadRoot matches, getEndpoint() returns
 * null, and Spring-WS throws NoEndpointFoundException -> HTTP 404.
 *
 * This is the SAME problem that JAX-WS had with Metro (we switched to CXF).
 * CXF solved it with an interceptor pipeline: decrypt -> dispatch -> process.
 *
 * ============================================================================
 *  THE SOLUTION: DECRYPT-THEN-DELEGATE
 * ============================================================================
 *
 * This custom EndpointMapping has the HIGHEST PRIORITY (Ordered.HIGHEST_PRECEDENCE)
 * so it runs before PayloadRootAnnotationMethodEndpointMapping.
 *
 * When getEndpoint() is called:
 *   1. Run Wss4jSecurityInterceptor.handleRequest() -> decrypts the body
 *   2. Now the SOAP body contains the real <addRequest> element
 *   3. Delegate to PayloadRootAnnotationMethodEndpointMapping to find the endpoint
 *   4. Return the found endpoint WITH the security interceptor in the chain
 *      (so handleResponse can sign/encrypt the reply)
 *
 * This way, decryption happens DURING endpoint resolution, not after.
 *
 * COMPARISON:
 *   CXF (JAX-WS):    interceptor pipeline      -> decrypt -> dispatch -> process
 *   Spring-WS (this): custom EndpointMapping    -> decrypt -> delegate -> resolve
 *   Both solve:       body must be readable before routing
 */
public class DecryptingEndpointMapping implements EndpointMapping, Ordered {

    private final Wss4jSecurityInterceptor securityInterceptor;
    private final PayloadRootAnnotationMethodEndpointMapping delegate;

    public DecryptingEndpointMapping(
            Wss4jSecurityInterceptor securityInterceptor,
            PayloadRootAnnotationMethodEndpointMapping delegate) {
        this.securityInterceptor = securityInterceptor;
        this.delegate = delegate;
    }

    /**
     * Highest priority so this mapping runs BEFORE the standard one.
     * MessageDispatcher iterates mappings sorted by Ordered.getOrder().
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Decrypt the message, then delegate endpoint resolution to the real mapping.
     *
     * Flow:
     *   1. securityInterceptor.handleRequest() decrypts + verifies + authenticates
     *   2. SOAP body is now readable (<addRequest> instead of <EncryptedData>)
     *   3. Delegate to PayloadRootAnnotationMethodEndpointMapping
     *   4. Wrap the result with the security interceptor for response handling
     */
    @Override
    public EndpointInvocationChain getEndpoint(MessageContext messageContext) throws Exception {
        // Step 1: Decrypt/verify/authenticate the incoming message
        // This modifies the SOAP message in-place, replacing <EncryptedData>
        // with the actual decrypted payload
        boolean continueProcessing = securityInterceptor.handleRequest(messageContext, null);
        if (!continueProcessing) {
            // Security validation failed - interceptor already set a fault response
            return null;
        }

        // Step 2: Now the body is decrypted, delegate to the real mapping
        // This calls PayloadRootAnnotationMethodEndpointMapping.getEndpoint()
        // which can now see <addRequest> and match it to @PayloadRoot
        EndpointInvocationChain chain = delegate.getEndpoint(messageContext);
        if (chain == null) {
            return null;
        }

        // Step 3: Wrap with a response-only interceptor
        // handleRequest already ran above, so we need an interceptor that:
        //   - skips handleRequest (already done)
        //   - runs handleResponse (to sign/encrypt the response)
        //   - runs handleFault (to handle errors properly)
        EndpointInterceptor[] existingInterceptors = chain.getInterceptors();
        EndpointInterceptor responseOnlyInterceptor = new ResponseOnlySecurityInterceptor(securityInterceptor);

        // Build new interceptor array: response-only security + existing interceptors
        EndpointInterceptor[] allInterceptors;
        if (existingInterceptors != null && existingInterceptors.length > 0) {
            allInterceptors = new EndpointInterceptor[existingInterceptors.length + 1];
            allInterceptors[0] = responseOnlyInterceptor;
            System.arraycopy(existingInterceptors, 0, allInterceptors, 1, existingInterceptors.length);
        } else {
            allInterceptors = new EndpointInterceptor[]{responseOnlyInterceptor};
        }

        return new EndpointInvocationChain(chain.getEndpoint(), allInterceptors);
    }

    /**
     * Wrapper interceptor that only runs handleResponse/handleFault.
     *
     * Since we already called securityInterceptor.handleRequest() during
     * endpoint resolution (in getEndpoint above), we must NOT call it again
     * during the normal dispatch flow. But we still need handleResponse()
     * to sign/encrypt the outgoing response.
     */
    private static class ResponseOnlySecurityInterceptor implements EndpointInterceptor {
        private final Wss4jSecurityInterceptor delegate;

        ResponseOnlySecurityInterceptor(Wss4jSecurityInterceptor delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean handleRequest(MessageContext messageContext, Object endpoint) {
            // Already handled in getEndpoint() - skip
            return true;
        }

        @Override
        public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
            return delegate.handleResponse(messageContext, endpoint);
        }

        @Override
        public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
            return delegate.handleFault(messageContext, endpoint);
        }

        @Override
        public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) throws Exception {
            delegate.afterCompletion(messageContext, endpoint, ex);
        }
    }
}
