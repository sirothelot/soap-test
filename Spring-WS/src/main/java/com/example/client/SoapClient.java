package com.example.client;

import com.example.security.SecurityConstants;
import com.example.service.gen.*;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

/**
 * SOAP Web Service Client (Spring-WS).
 *
 * COMPARISON WITH JAX-WS:
 * =======================
 * JAX-WS client:
 *   1. Points to WSDL URL
 *   2. Creates a Service object from WSDL
 *   3. Gets a proxy (port) that implements the service interface
 *   4. Calls methods on the proxy like normal Java methods
 *   5. The proxy auto-creates SOAP XML and parses responses
 *
 * Spring-WS client:
 *   1. Creates a Jaxb2Marshaller (converts Java <-> XML)
 *   2. Creates a WebServiceTemplate (like RestTemplate, but for SOAP)
 *   3. Creates JAXB request objects and calls marshalSendAndReceive()
 *   4. Gets back JAXB response objects
 *
 * KEY DIFFERENCES:
 *   JAX-WS:    Uses a proxy interface  -> calculator.add(10, 5)
 *   Spring-WS: Uses request/response objects -> template.marshalSendAndReceive(addRequest)
 *
 *   JAX-WS:    Needs WSDL URL at runtime to create the proxy
 *   Spring-WS: Needs the service URL + JAXB classes (WSDL not required at runtime)
 *
 * SOAP MESSAGE FLOW (identical for both):
 *   Client creates request -> marshalled to XML -> sent via HTTP POST
 *   -> Server processes -> response XML -> unmarshalled to Java object
 */
public class SoapClient {

    // The service endpoint URL (not the WSDL URL)
    private static final String SERVICE_URL = "http://localhost:8080/ws";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   SOAP Web Service Client Demo");
        System.out.println("   (Spring-WS version)");
        System.out.println("========================================");
        System.out.println();

        try {
            // Step 1: Create a JAXB marshaller
            // This converts Java objects <-> XML (like JAX-WS does internally)
            System.out.println("Step 1: Setting up JAXB marshaller...");
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setContextPath("com.example.service.gen");
            marshaller.afterPropertiesSet();

            // Step 2: Create the WebServiceTemplate
            // JAX-WS equivalent: Service.create(wsdlUrl, serviceName)
            System.out.println("Step 2: Creating WebServiceTemplate...");
            WebServiceTemplate template = new WebServiceTemplate(marshaller);
            template.setDefaultUri(SERVICE_URL);

            // Basic timeouts to keep demos from hanging forever
            HttpUrlConnectionMessageSender messageSender = new HttpUrlConnectionMessageSender();
            messageSender.setConnectionTimeout(Duration.ofSeconds(5));
            messageSender.setReadTimeout(Duration.ofSeconds(5));
            template.setMessageSender(messageSender);

            // Step 3: Add WS-Security to outgoing requests
            // This interceptor automatically adds three security layers:
            //   1. UsernameToken (authentication - who is calling)
            //   2. Digital Signature (integrity - proves message wasn't tampered)
            //   3. Encryption (confidentiality - prevents reading by intermediaries)
            //
            // COMPARISON WITH JAX-WS:
            // =======================
            // JAX-WS ClientSecurityHandler:
            //   - Create WSSecHeader, WSSecUsernameToken, WSSecSignature, WSSecEncrypt
            //   - Call build() on each one, passing the Crypto object
            //   - ~80 lines of WSS4J API calls
            //
            // Spring-WS (below):
            //   - Set securementActions and a few properties
            //   - ~15 lines of configuration
            //   - Spring-WS calls the same WSS4J classes under the hood
            //
            // "securementActions" = what to ADD to outgoing messages:
            //   "UsernameToken" = add username/password header
            //   "Signature"     = digitally sign the message body
            //   "Encrypt"       = encrypt the message body
            System.out.println("Step 3: Configuring WS-Security (UsernameToken + Signature + Encryption)...");

            // Load client crypto from properties file.
            // This loads BOTH the keystore (for signing) and truststore (for encrypting).
            // CryptoFactory reads client-crypto.properties which specifies:
            //   - keystore file/password/alias (client's private key for signing)
            //   - truststore file/password (server's public cert for encrypting)
            //
            // COMPARISON WITH JAX-WS (CXF):
            //   CXF:       SIG_PROP_FILE = "client-crypto.properties"  (string property)
            //   Spring-WS: CryptoFactory.getInstance("client-crypto.properties")  (Java object)
            //   Both use the SAME properties file format and the SAME WSS4J Crypto engine.
            Crypto clientCrypto = CryptoFactory.getInstance(SecurityConstants.CLIENT_CRYPTO_PROPERTIES);

            Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();

            // What security actions to apply to outgoing messages
            securityInterceptor.setSecurementActions("UsernameToken Signature Encrypt");

            // UsernameToken: credentials to send
            securityInterceptor.setSecurementUsername(SecurityConstants.USERNAME);

            // PASSWORD HANDLING - A KEY DIFFERENCE FROM JAX-WS (CXF):
            // ========================================================
            // Spring-WS's Wss4jSecurityInterceptor has setSecurementPassword()
            // which sets ONE password for ALL outgoing security actions.
            //
            // PROBLEM: When using BOTH UsernameToken AND Signature, WSS4J needs
            // TWO different passwords:
            //   - UsernameToken: alice's credential ("secret123")
            //   - Signature:     client private key password ("clientpass")
            //
            // SOLUTION: We use a PasswordCallbackInterceptor (runs BEFORE this
            // security interceptor) that places a PW_CALLBACK_REF on the
            // MessageContext. WSS4J finds this callback handler and calls it
            // for each security action with the appropriate identifier:
            //   - For UsernameToken: id="alice"   -> returns "secret123"
            //   - For Signature:     id="client"  -> returns "clientpass"
            //
            // WHY THIS WORKS:
            //   WSHandler.getPasswordCallbackHandler() checks:
            //     1. handler options for PW_CALLBACK_REF  (not accessible)
            //     2. MessageContext for PW_CALLBACK_REF   (our interceptor sets this!)
            //     3. handler options for PW_CALLBACK_CLASS (not accessible)
            //   When found, WSS4J uses our handler INSTEAD of the single password.
            //
            // JAX-WS (CXF) COMPARISON:
            //   CXF:       properties.put(PW_CALLBACK_CLASS, handler.class.getName())
            //   Spring-WS: PasswordCallbackInterceptor sets PW_CALLBACK_REF on MessageContext
            //   Both route to the SAME WSS4J CallbackHandler mechanism!
            // Signature: sign with client's private key
            //   "DirectReference" means the signed message includes the full X.509
            //   certificate, so the server can verify immediately without a lookup.
            securityInterceptor.setSecurementSignatureUser(SecurityConstants.CLIENT_KEY_ALIAS);
            securityInterceptor.setSecurementSignatureKeyIdentifier("DirectReference");
            securityInterceptor.setSecurementSignatureCrypto(clientCrypto);

            // Encryption: encrypt with server's public key (from client-truststore)
            //   ENCRYPTION_USER = alias of the server's cert in our truststore.
            //   The body will be encrypted so only the server can read it.
            securityInterceptor.setSecurementEncryptionUser(SecurityConstants.SERVER_KEY_ALIAS);
            securityInterceptor.setSecurementEncryptionCrypto(clientCrypto);

            securityInterceptor.afterPropertiesSet();

            // INTERCEPTOR CHAIN ORDER MATTERS!
            // The PasswordCallbackInterceptor MUST run BEFORE the Wss4jSecurityInterceptor.
            // It places a PW_CALLBACK_REF on the MessageContext, which WSS4J then picks up
            // when the security interceptor processes the outgoing message.
            template.setInterceptors(new ClientInterceptor[]{
                    new PasswordCallbackInterceptor(),  // 1st: injects callback handler
                    securityInterceptor                 // 2nd: applies security (uses the handler)
            });

            System.out.println("[OK] Connected successfully!");
            System.out.println();

            // Step 3: Call the service operations
            System.out.println("========================================");
            System.out.println("   Calling Web Service Operations");
            System.out.println("========================================");
            System.out.println();

            // Addition
            // JAX-WS:    int sum = calculator.add(10, 5);
            // Spring-WS: Create request object, send, get response object
            System.out.println("Calling: add(10, 5)");
            AddRequest addReq = new AddRequest();
            addReq.setA(10);
            addReq.setB(5);
            AddResponse addResp = (AddResponse) template.marshalSendAndReceive(addReq);
            System.out.println("Result:  " + addResp.getSum());
            System.out.println();

            // Subtraction
            System.out.println("Calling: subtract(10, 5)");
            SubtractRequest subReq = new SubtractRequest();
            subReq.setA(10);
            subReq.setB(5);
            SubtractResponse subResp = (SubtractResponse) template.marshalSendAndReceive(subReq);
            System.out.println("Result:  " + subResp.getDifference());
            System.out.println();

            // Multiplication
            System.out.println("Calling: multiply(10, 5)");
            MultiplyRequest mulReq = new MultiplyRequest();
            mulReq.setA(10);
            mulReq.setB(5);
            MultiplyResponse mulResp = (MultiplyResponse) template.marshalSendAndReceive(mulReq);
            System.out.println("Result:  " + mulResp.getProduct());
            System.out.println();

            // Division
            System.out.println("Calling: divide(10, 5)");
            DivideRequest divReq = new DivideRequest();
            divReq.setA(10);
            divReq.setB(5);
            DivideResponse divResp = (DivideResponse) template.marshalSendAndReceive(divReq);
            System.out.println("Result:  " + divResp.getQuotient());
            System.out.println();

            // More examples
            System.out.println("========================================");
            System.out.println("   More Examples");
            System.out.println("========================================");
            System.out.println();

            System.out.println("Calling: add(100, 200)");
            AddRequest addReq2 = new AddRequest();
            addReq2.setA(100);
            addReq2.setB(200);
            AddResponse addResp2 = (AddResponse) template.marshalSendAndReceive(addReq2);
            System.out.println("Result:  " + addResp2.getSum());
            System.out.println();

            System.out.println("Calling: multiply(7, 8)");
            MultiplyRequest mulReq2 = new MultiplyRequest();
            mulReq2.setA(7);
            mulReq2.setB(8);
            MultiplyResponse mulResp2 = (MultiplyResponse) template.marshalSendAndReceive(mulReq2);
            System.out.println("Result:  " + mulResp2.getProduct());
            System.out.println();

            System.out.println("Calling: divide(22, 7)");
            DivideRequest divReq2 = new DivideRequest();
            divReq2.setA(22);
            divReq2.setB(7);
            DivideResponse divResp2 = (DivideResponse) template.marshalSendAndReceive(divReq2);
            System.out.println("Result:  " + divResp2.getQuotient());
            System.out.println();

            // SOAP Fault demo: divide by zero
            // The server throws DivisionByZeroException, which Spring-WS maps
            // to a SOAP Fault via @SoapFault(faultCode = FaultCode.CLIENT).
            // This shows how SOAP reports errors — as structured XML Faults,
            // not HTTP error codes like REST.
            System.out.println("========================================");
            System.out.println("   SOAP Fault Demo (divide by zero)");
            System.out.println("========================================");
            System.out.println();

            System.out.println("Calling: divide(10, 0)");
            try {
                DivideRequest divByZero = new DivideRequest();
                divByZero.setA(10);
                divByZero.setB(0);
                template.marshalSendAndReceive(divByZero);
                System.out.println("ERROR: Should have thrown a SOAP Fault!");
            } catch (SoapFaultClientException e) {
                System.out.println("[OK] Got expected SOAP Fault:");
                System.out.println("  Fault code:   " + e.getSoapFault().getFaultCode());
                System.out.println("  Fault string: " + e.getFaultStringOrReason());
            }
            System.out.println();

            System.out.println("========================================");
            System.out.println("   Demo Complete!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println("Make sure the server is running first!");
            System.err.println("Run: mvnw.ps1 spring-boot:run");
            e.printStackTrace();
        }
    }
}
