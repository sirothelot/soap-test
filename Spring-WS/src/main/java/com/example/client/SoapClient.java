package com.example.client;

import com.example.security.SecurityConstants;
import com.example.service.gen.*;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;

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

            // Step 3: Add WS-Security to outgoing requests
            // This interceptor automatically adds a <wsse:Security> header
            // with our username and password to every SOAP message we send.
            //
            // COMPARISON WITH JAX-WS:
            // =======================
            // JAX-WS:    Write a ClientSecurityHandler that manually builds
            //            <wsse:Security> XML elements (~100 lines of code).
            // Spring-WS: Configure a Wss4jSecurityInterceptor with 3 properties.
            //            WSS4J builds the XML header automatically.
            //
            // "securementActions" = what to ADD to outgoing messages:
            //   "UsernameToken" = add username/password header
            //   "Timestamp"     = add timestamp (server can reject old messages)
            //   "Signature"     = digitally sign the message
            //   "Encrypt"       = encrypt the message body
            System.out.println("Step 3: Configuring WS-Security credentials...");
            Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
            securityInterceptor.setSecurementActions("UsernameToken");
            securityInterceptor.setSecurementUsername(SecurityConstants.USERNAME);
            securityInterceptor.setSecurementPassword(SecurityConstants.PASSWORD);
            securityInterceptor.afterPropertiesSet();

            template.setInterceptors(new ClientInterceptor[]{securityInterceptor});

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
