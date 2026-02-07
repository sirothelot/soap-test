package com.example.server;

import com.example.security.SecurityConstants;
import com.example.security.ServerPasswordCallbackHandler;
import com.example.service.CalculatorService;
import com.example.service.CalculatorServiceImpl;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * SOAP Web Service Server/Publisher (CXF version).
 *
 * WHY APACHE CXF?
 * ================
 * The JDK's built-in JAX-WS (Metro) works for basic SOAP services but cannot
 * handle WS-Security ENCRYPTION properly. Metro tries to determine which
 * @WebService method to call by reading the SOAP body BEFORE handlers run.
 * When the body is encrypted, it sees <EncryptedData> instead of <add> and fails.
 *
 * Apache CXF uses an INTERCEPTOR PIPELINE that processes security BEFORE dispatch:
 *
 *   Request arrives
 *     -> WSS4JInInterceptor DECRYPTS the body          (Phase: PRE_PROTOCOL)
 *     -> WSS4JInInterceptor VERIFIES the signature     (Phase: PRE_PROTOCOL)
 *     -> WSS4JInInterceptor VALIDATES credentials      (Phase: PRE_PROTOCOL)
 *     -> CXF reads the now-decrypted body              (Phase: UNMARSHAL)
 *     -> CXF dispatches to add(), subtract(), etc.     (Phase: INVOKE)
 *
 * HOW CXF WS-SECURITY CONFIGURATION WORKS:
 * =========================================
 * Instead of writing SOAPHandler classes, you configure WSS4J through a
 * simple properties Map. CXF passes these to the WSS4J engine automatically.
 *
 * COMPARISON WITH SPRING-WS:
 * ==========================
 * CXF:       Properties Map -> WSS4JInInterceptor -> added to server factory
 * Spring-WS: Properties     -> Wss4jSecurityInterceptor -> registered as bean
 *
 * Both use the exact same WSS4J engine underneath.
 * CXF uses a Map<String,Object>, Spring-WS uses setter methods.
 */
public class SoapServer {

    private static final String SERVICE_URL = "http://localhost:8080/calculator";

    private org.apache.cxf.endpoint.Server server;

    /**
     * Starts the SOAP web service server with WS-Security.
     */
    public void start() {
        System.out.println("========================================");
        System.out.println("   SOAP Web Service Server Starting");
        System.out.println("   (Apache CXF + WSS4J Security)");
        System.out.println("========================================");
        System.out.println();

        // Create the CXF server factory
        // This is the CXF equivalent of Endpoint.publish()
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(CalculatorService.class);
        factory.setAddress(SERVICE_URL);
        factory.setServiceBean(new CalculatorServiceImpl());

        // Log raw SOAP messages for learning/debugging
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());

        // =====================================================
        // WS-SECURITY CONFIGURATION (server-side / inbound)
        // =====================================================
        // Configure what security operations to EXPECT and VALIDATE
        // on incoming messages.
        //
        // This replaces our hand-written ServerSecurityHandler.java.
        // Instead of manually calling WSSecurityEngine, we just set
        // properties and CXF handles everything.
        Map<String, Object> inProps = new HashMap<>();

        // ACTION: What security operations to expect in incoming messages.
        //
        // "UsernameToken" = expect username/password credentials
        // "Signature"     = expect and verify a digital signature
        // "Encrypt"       = expect and decrypt encrypted content
        //
        // Order matters! WSS4J processes right-to-left in the security header:
        //   1. Decrypt  2. Verify signature  3. Validate credentials
        inProps.put(WSHandlerConstants.ACTION,
                WSHandlerConstants.USERNAME_TOKEN + " "
                + WSHandlerConstants.SIGNATURE + " "
                + WSHandlerConstants.ENCRYPT);

        // PASSWORD CALLBACK: WSS4J calls this to get passwords for:
        //   - Decryption (server's private key password)
        //   - UsernameToken validation (expected password for the user)
        //
        // COMPARISON WITH SPRING-WS:
        //   CXF:       PW_CALLBACK_CLASS = "com.example.security.ServerPasswordCallbackHandler"
        //   Spring-WS: SimplePasswordValidationCallbackHandler bean with Properties
        inProps.put(WSHandlerConstants.PW_CALLBACK_CLASS,
                ServerPasswordCallbackHandler.class.getName());

        // SIGNATURE VERIFICATION: Where to find the CLIENT's public certificate
        // to verify that the digital signature is genuine.
        //
        // This properties file points to server-truststore.p12 which contains
        // the client's certificate. (We trust the client's public key.)
        //
        // COMPARISON WITH SPRING-WS:
        //   CXF:       SIG_VER_PROP_FILE = "server-crypto.properties"
        //   Spring-WS: interceptor.setValidationSignatureCrypto(crypto)
        inProps.put(WSHandlerConstants.SIG_VER_PROP_FILE,
                SecurityConstants.SERVER_CRYPTO_PROPERTIES);

        // DECRYPTION: Where to find the SERVER's private key
        // to decrypt the message body.
        //
        // This properties file points to server-keystore.p12 which contains
        // the server's private key. Only we can decrypt messages encrypted
        // with our public key.
        //
        // COMPARISON WITH SPRING-WS:
        //   CXF:       DEC_PROP_FILE = "server-crypto.properties"
        //   Spring-WS: interceptor.setValidationDecryptionCrypto(crypto)
        inProps.put(WSHandlerConstants.DEC_PROP_FILE,
                SecurityConstants.SERVER_CRYPTO_PROPERTIES);

        // Add the WSS4J interceptor to the inbound (incoming request) chain
        factory.getInInterceptors().add(new WSS4JInInterceptor(inProps));

        System.out.println("Security: WSS4J interceptor configured");
        System.out.println("Security: Actions = UsernameToken + Signature + Encrypt");

        // Start the server
        server = factory.create();

        System.out.println();
        System.out.println("[OK] Service published successfully!");
        System.out.println();
        System.out.println("Service URL: " + SERVICE_URL);
        System.out.println("WSDL URL:    " + SERVICE_URL + "?wsdl");
        System.out.println();
        System.out.println("Server is running... Press Ctrl+C to stop.");
        System.out.println("========================================");
        System.out.println();
    }

    /**
     * Stops the server.
     */
    public void stop() {
        if (server != null) {
            server.stop();
            server.destroy();
            System.out.println("Server stopped.");
        }
    }

    public static void main(String[] args) {
        SoapServer server = new SoapServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
