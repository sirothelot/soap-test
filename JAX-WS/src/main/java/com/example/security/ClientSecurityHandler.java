package com.example.security;

import jakarta.xml.soap.*;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.namespace.QName;
import java.util.Set;

/**
 * CLIENT-SIDE WS-Security Handler (JAX-WS).
 *
 * HOW THE CLIENT-SIDE HANDLER WORKS:
 * ===================================
 * This is the mirror image of the ServerSecurityHandler.
 * While the server READS credentials from the SOAP header,
 * this handler WRITES credentials INTO the SOAP header.
 *
 *   Your code calls:  calculator.add(10, 5)
 *                         |
 *                         v
 *   JAX-WS creates a SOAP message (no security header yet)
 *                         |
 *                         v
 *   THIS HANDLER intercepts and adds the security header:
 *     <wsse:Security>
 *       <wsse:UsernameToken>
 *         <wsse:Username>alice</wsse:Username>
 *         <wsse:Password>secret123</wsse:Password>
 *       </wsse:UsernameToken>
 *     </wsse:Security>
 *                         |
 *                         v
 *   The message (now WITH credentials) is sent to the server
 *
 * COMPARISON WITH SPRING-WS:
 * ==========================
 * JAX-WS:    You manually build the XML elements in a SOAPHandler.
 * Spring-WS: You set properties on the Wss4jSecurityInterceptor:
 *              interceptor.setSecurementActions("UsernameToken");
 *              interceptor.setSecurementUsername("alice");
 *              interceptor.setSecurementPassword("secret123");
 *            WSS4J builds the header for you automatically.
 */
public class ClientSecurityHandler implements SOAPHandler<SOAPMessageContext> {

    /**
     * Called for every outgoing/incoming SOAP message.
     * We add credentials to OUTGOING messages only.
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // Only add security header to OUTGOING requests (not incoming responses)
        Boolean isOutbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (!isOutbound) {
            return true; // Don't modify incoming responses
        }

        System.out.println("Security: Adding WS-Security header to request...");

        try {
            SOAPMessage message = context.getMessage();
            SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();

            // Create the <soap:Header> if it doesn't exist
            SOAPHeader header = envelope.getHeader();
            if (header == null) {
                header = envelope.addHeader();
            }

            // Build the WS-Security header structure:
            //
            // <wsse:Security xmlns:wsse="...">
            //   <wsse:UsernameToken>
            //     <wsse:Username>alice</wsse:Username>
            //     <wsse:Password>secret123</wsse:Password>
            //   </wsse:UsernameToken>
            // </wsse:Security>

            // Step 1: Create <wsse:Security>
            SOAPElement security = header.addChildElement("Security", SecurityConstants.WSSE_PREFIX, SecurityConstants.WSSE_NS);

            // Step 2: Create <wsse:UsernameToken> inside Security
            SOAPElement usernameToken = security.addChildElement("UsernameToken", SecurityConstants.WSSE_PREFIX);

            // Step 3: Add <wsse:Username>
            SOAPElement username = usernameToken.addChildElement("Username", SecurityConstants.WSSE_PREFIX);
            username.addTextNode(SecurityConstants.USERNAME);

            // Step 4: Add <wsse:Password>
            SOAPElement password = usernameToken.addChildElement("Password", SecurityConstants.WSSE_PREFIX);
            password.addTextNode(SecurityConstants.PASSWORD);

            // Save changes to the message
            message.saveChanges();

            System.out.println("Security: Header added (user: " + SecurityConstants.USERNAME + ")");
            return true;

        } catch (SOAPException e) {
            throw new RuntimeException("Error adding WS-Security header: " + e.getMessage());
        }
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
        return Set.of();
    }
}
