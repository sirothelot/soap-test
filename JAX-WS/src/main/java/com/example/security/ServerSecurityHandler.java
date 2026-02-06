package com.example.security;

import jakarta.xml.soap.*;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Set;

/**
 * SERVER-SIDE WS-Security Handler (JAX-WS).
 *
 * HOW JAX-WS HANDLERS WORK:
 * ==========================
 * Handlers are like filters/interceptors that sit between the network and
 * your service implementation. Every SOAP message passes through the handler
 * chain BEFORE reaching your @WebService class.
 *
 *   Client -> [SOAP Message] -> Handler Chain -> @WebService method
 *                                    ^
 *                              This handler runs here.
 *                              It checks for valid credentials
 *                              BEFORE the request reaches your code.
 *
 * WHAT THIS HANDLER DOES:
 * =======================
 * 1. Intercepts every incoming SOAP request
 * 2. Looks for a <wsse:Security> header in the SOAP envelope
 * 3. Extracts <wsse:UsernameToken> from the security header
 * 4. Validates the username and password
 * 5. If valid: allows the request through to the service
 * 6. If invalid: rejects the request with a SOAP Fault
 *
 * COMPARISON WITH SPRING-WS:
 * ==========================
 * JAX-WS:    You write a SOAPHandler and manually parse the XML header.
 * Spring-WS: You configure a Wss4jSecurityInterceptor bean (no XML parsing needed).
 *
 * JAX-WS gives you more control but more boilerplate.
 * Spring-WS uses Apache WSS4J library which handles the parsing for you.
 */
public class ServerSecurityHandler implements SOAPHandler<SOAPMessageContext> {

    /**
     * Called for every SOAP message (both requests and responses).
     * Return true to continue processing, false to stop.
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // Only check security on INCOMING requests (not outgoing responses)
        Boolean isOutbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (isOutbound) {
            return true; // Don't check outgoing responses
        }

        System.out.println("Security: Checking WS-Security credentials...");

        try {
            SOAPMessage message = context.getMessage();
            SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
            SOAPHeader header = envelope.getHeader();

            // Step 1: Check if the SOAP message has a Header
            if (header == null) {
                throw new SecurityException("No SOAP header found. "
                    + "The client must include a WS-Security header with credentials.");
            }

            // Step 2: Find the <wsse:Security> element in the header
            // This is the standard WS-Security header element
            Iterator<?> securityHeaders = header.getChildElements(
                new QName(SecurityConstants.WSSE_NS, "Security", SecurityConstants.WSSE_PREFIX)
            );

            if (!securityHeaders.hasNext()) {
                throw new SecurityException("No WS-Security header found. "
                    + "Expected <wsse:Security> element with UsernameToken.");
            }

            SOAPElement securityElement = (SOAPElement) securityHeaders.next();

            // Step 3: Find the <wsse:UsernameToken> inside the Security header
            Iterator<?> usernameTokens = securityElement.getChildElements(
                new QName(SecurityConstants.WSSE_NS, "UsernameToken", SecurityConstants.WSSE_PREFIX)
            );

            if (!usernameTokens.hasNext()) {
                throw new SecurityException("No UsernameToken found inside Security header. "
                    + "Expected <wsse:UsernameToken> with Username and Password.");
            }

            SOAPElement usernameToken = (SOAPElement) usernameTokens.next();

            // Step 4: Extract the username and password values
            String username = getChildElementValue(usernameToken, "Username");
            String password = getChildElementValue(usernameToken, "Password");

            // Step 5: Validate the credentials
            if (!SecurityConstants.USERNAME.equals(username)
                    || !SecurityConstants.PASSWORD.equals(password)) {
                System.out.println("Security: REJECTED - invalid credentials for user: " + username);
                throw new SecurityException("Authentication failed: invalid username or password.");
            }

            System.out.println("Security: ACCEPTED - authenticated user: " + username);
            return true; // Allow the request through

        } catch (SecurityException e) {
            throw new RuntimeException(e.getMessage());
        } catch (SOAPException e) {
            throw new RuntimeException("Error processing WS-Security header: " + e.getMessage());
        }
    }

    /**
     * Helper: Get the text content of a child element by local name.
     * e.g., from <wsse:UsernameToken>, get the value of <wsse:Username>
     */
    private String getChildElementValue(SOAPElement parent, String localName) throws SOAPException {
        Iterator<?> children = parent.getChildElements(
            new QName(SecurityConstants.WSSE_NS, localName, SecurityConstants.WSSE_PREFIX)
        );
        if (!children.hasNext()) {
            throw new SecurityException("Missing <wsse:" + localName + "> in UsernameToken");
        }
        return ((SOAPElement) children.next()).getTextContent();
    }

    // --- Required by SOAPHandler interface but not needed for this demo ---

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        // Declare that this handler understands the WS-Security header
        // This tells the runtime we can process <wsse:Security> elements
        return Set.of(new QName(SecurityConstants.WSSE_NS, "Security"));
    }
}
