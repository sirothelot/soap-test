package com.example.client;

import com.example.security.ClientPasswordCallbackHandler;
import com.example.security.SecurityConstants;
import com.example.service.CalculatorService;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * SOAP Web Service Client (CXF version with WS-Security).
 *
 * HOW CXF CLIENT WS-SECURITY WORKS:
 * ===================================
 * Just like the server, the client uses a WSS4J interceptor configured
 * via a properties Map. The difference:
 *
 *   Server uses WSS4JInInterceptor  (processes INCOMING messages)
 *   Client uses WSS4JOutInterceptor (processes OUTGOING messages)
 *
 * For every outgoing request, WSS4J automatically:
 *   1. Adds UsernameToken (username + password)
 *   2. Signs the body with client's private key
 *   3. Encrypts the body with server's public key
 *
 * COMPARISON:
 *   Old Metro approach: Write a ClientSecurityHandler with ~180 lines
 *                       of WSS4J API calls (WSSecUsernameToken, WSSecSignature, etc.)
 *   CXF approach:       Configure a Map with ~10 properties, done.
 *   Spring-WS:          Configure Wss4jSecurityInterceptor with ~10 setter calls, done.
 *
 * All three use the same WSS4J engine; the only difference is the configuration style.
 */
public class SoapClient {

    private static final String SERVICE_URL = "http://localhost:8080/calculator";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   SOAP Web Service Client Demo");
        System.out.println("   (Apache CXF + WSS4J Security)");
        System.out.println("========================================");
        System.out.println();

        try {
            // Step 1: Create a CXF proxy factory
            // This replaces the old Service.create(wsdlUrl, serviceName) approach.
            // CXF creates a proxy that looks like a normal Java interface.
            System.out.println("Step 1: Creating CXF proxy factory...");
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(CalculatorService.class);
            factory.setAddress(SERVICE_URL);

            // Step 2: Configure WS-Security for outgoing messages
            // =====================================================
            System.out.println("Step 2: Configuring WS-Security...");
            Map<String, Object> outProps = new HashMap<>();

            // ACTION: What security operations to APPLY to outgoing messages.
            // Order: UsernameToken -> Sign -> Encrypt
            // (Encryption is last so the signature is also encrypted)
            outProps.put(WSHandlerConstants.ACTION,
                    WSHandlerConstants.USERNAME_TOKEN + " "
                    + WSHandlerConstants.SIGNATURE + " "
                    + WSHandlerConstants.ENCRYPT);

            // USERNAME for the UsernameToken header
            outProps.put(WSHandlerConstants.USER, SecurityConstants.USERNAME);

            // PASSWORD CALLBACK: Provides passwords for:
            //   - UsernameToken (alice's password)
            //   - Signing (client's private key password)
            outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS,
                    ClientPasswordCallbackHandler.class.getName());

            // SIGNATURE: Which private key to sign with, and where to find it.
            //   SIG_PROP_FILE -> points to client-crypto.properties
            //                    which points to client-keystore.jks
            //   SIGNATURE_USER -> alias of the key in the keystore
            //
            // COMPARISON WITH SPRING-WS:
            //   CXF:       SIG_PROP_FILE + SIGNATURE_USER properties
            //   Spring-WS: interceptor.setSecurementSignatureCrypto(crypto)
            //              interceptor.setSecurementSignatureUser("client")
            outProps.put(WSHandlerConstants.SIG_PROP_FILE,
                    SecurityConstants.CLIENT_CRYPTO_PROPERTIES);
            outProps.put(WSHandlerConstants.SIGNATURE_USER,
                    SecurityConstants.CLIENT_KEY_ALIAS);

            // ENCRYPTION: Which public key to encrypt FOR, and where to find it.
            //   ENC_PROP_FILE    -> points to client-crypto.properties
            //                      which points to client-truststore.jks
            //   ENCRYPTION_USER  -> alias of the server's cert in the truststore
            //
            // The message body will be encrypted with the server's public key.
            // Only the server (with its private key) can decrypt it.
            //
            // COMPARISON WITH SPRING-WS:
            //   CXF:       ENC_PROP_FILE + ENCRYPTION_USER properties
            //   Spring-WS: interceptor.setSecurementEncryptionCrypto(crypto)
            //              interceptor.setSecurementEncryptionUser("server")
            outProps.put(WSHandlerConstants.ENC_PROP_FILE,
                    SecurityConstants.CLIENT_CRYPTO_PROPERTIES);
            outProps.put(WSHandlerConstants.ENCRYPTION_USER,
                    SecurityConstants.SERVER_KEY_ALIAS);

            // Add the WSS4J interceptor to the outbound (outgoing request) chain
            factory.getOutInterceptors().add(new WSS4JOutInterceptor(outProps));

            System.out.println("         Actions = UsernameToken + Signature + Encrypt");

            // Step 3: Create the proxy
            // This gives us a CalculatorService that looks like a normal Java interface,
            // but every method call automatically creates a SOAP message,
            // applies WS-Security (sign + encrypt), sends it, and parses the response.
            System.out.println("Step 3: Creating service proxy...");
            CalculatorService calculator = (CalculatorService) factory.create();

            System.out.println("[OK] Connected successfully!");
            System.out.println();

            // Step 4: Call the service methods
            System.out.println("========================================");
            System.out.println("   Calling Web Service Operations");
            System.out.println("========================================");
            System.out.println();

            System.out.println("Calling: add(10, 5)");
            int sum = calculator.add(10, 5);
            System.out.println("Result:  " + sum);
            System.out.println();

            System.out.println("Calling: subtract(10, 5)");
            int difference = calculator.subtract(10, 5);
            System.out.println("Result:  " + difference);
            System.out.println();

            System.out.println("Calling: multiply(10, 5)");
            int product = calculator.multiply(10, 5);
            System.out.println("Result:  " + product);
            System.out.println();

            System.out.println("Calling: divide(10, 5)");
            double quotient = calculator.divide(10, 5);
            System.out.println("Result:  " + quotient);
            System.out.println();

            System.out.println("========================================");
            System.out.println("   More Examples");
            System.out.println("========================================");
            System.out.println();

            System.out.println("Calling: add(100, 200)");
            System.out.println("Result:  " + calculator.add(100, 200));
            System.out.println();

            System.out.println("Calling: multiply(7, 8)");
            System.out.println("Result:  " + calculator.multiply(7, 8));
            System.out.println();

            System.out.println("Calling: divide(22, 7)");
            System.out.println("Result:  " + calculator.divide(22, 7));
            System.out.println();

            System.out.println("========================================");
            System.out.println("   Demo Complete!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println("Make sure the server is running first!");
            System.err.println("Run: .\\mvnw.ps1 exec:java \"-Dexec.mainClass=com.example.server.SoapServer\"");
            e.printStackTrace();
        }
    }
}
