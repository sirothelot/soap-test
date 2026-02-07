package com.example.config;

import com.example.security.SecurityConstants;
import com.example.security.ServerPasswordCallbackHandler;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.server.endpoint.interceptor.PayloadLoggingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.util.List;
import java.util.Properties;

/**
 * Spring Web Services Configuration.
 *
 * COMPARISON WITH JAX-WS:
 * =======================
 * JAX-WS:    Configuration is done via @WebService annotations on the interface/impl.
 *            The WSDL is auto-generated from Java annotations (code-first).
 *
 * Spring-WS: Configuration is done via @Configuration class.
 *            The WSDL is auto-generated from the XSD schema (contract-first).
 *            You explicitly define the servlet, WSDL definition, and schema beans.
 *
 * KEY BEANS:
 * - MessageDispatcherServlet: Routes SOAP messages to @Endpoint classes
 *   (similar to how JAX-WS routes to @WebService implementations)
 * - DefaultWsdl11Definition: Generates WSDL from XSD
 *   (in JAX-WS, the runtime generates WSDL from Java annotations)
 * - XsdSchema: Loads the XSD that defines request/response types
 *   (in JAX-WS, types are inferred from Java method signatures)
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    /**
     * Register the MessageDispatcherServlet.
     * This servlet handles all SOAP messages coming to /ws/*
     *
     * JAX-WS equivalent: Endpoint.publish("http://localhost:8080/calculator", impl)
     */
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    /**
     * Define the WSDL - generated automatically from the XSD schema.
     *
     * The bean name ("calculator") determines the WSDL URL:
     * http://localhost:8080/ws/calculator.wsdl
     *
     * JAX-WS equivalent: WSDL is auto-generated at ?wsdl URL from @WebService annotations.
     */
    @Bean(name = "calculator")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema calculatorSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("CalculatorPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://service.example.com/");
        wsdl.setSchema(calculatorSchema);
        return wsdl;
    }

    /**
     * Load the XSD schema that defines request/response types.
     *
     * JAX-WS equivalent: Not needed - types come from Java method signatures.
     */
    @Bean
    public XsdSchema calculatorSchema() {
        return new SimpleXsdSchema(new ClassPathResource("calculator.xsd"));
    }

    /**
     * Payload logging for educational visibility.
     *
     * NOTE ON SCHEMA VALIDATION WITH ENCRYPTION:
     * ===========================================
     * PayloadValidatingInterceptor is NOT used here because it's incompatible
     * with the DecryptingEndpointMapping flow. After WS-Security decryption,
     * the DOM tree contains residual security artifacts that cause
     * IndexOutOfBoundsException during XSD validation.
     *
     * Schema conformance is still enforced by:
     *   1. JAXB unmarshalling (rejects invalid XML structure)
     *   2. The XSD-generated WSDL (clients validate before sending)
     *
     * This is similar to JAX-WS/CXF where @SchemaValidation works because
     * CXF's pipeline fully cleans up the DOM after decryption, whereas
     * Spring-WS's interceptor-based approach doesn't.
     */
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        PayloadLoggingInterceptor loggingInterceptor = new PayloadLoggingInterceptor();
        loggingInterceptor.setLogRequest(true);
        loggingInterceptor.setLogResponse(true);

        interceptors.add(loggingInterceptor);
    }

    // =========================================================================
    //  WS-SECURITY CONFIGURATION
    //  Three layers: UsernameToken + Signature + Encryption
    // =========================================================================

    /**
     * Custom endpoint mapping that decrypts BEFORE resolving the endpoint.
     *
     * IMPORTANT - WHY addInterceptors() WON'T WORK WITH ENCRYPTION:
     * ==============================================================
     * Spring-WS resolves the endpoint FIRST by looking at the SOAP body's
     * root element (via @PayloadRoot). But with encryption, the body is
     * <EncryptedData> — NOT <addRequest>. So no endpoint matches, and
     * Spring-WS returns a 404 before interceptors ever run.
     *
     * This is the SAME problem JAX-WS had with Metro (we had to switch to CXF).
     *
     * THE FIX: A custom EndpointMapping (DecryptingEndpointMapping) that:
     *   1. Has HIGHEST PRIORITY (runs before PayloadRootAnnotationMethodEndpointMapping)
     *   2. Calls securityInterceptor.handleRequest() to decrypt the body
     *   3. Delegates to PayloadRootAnnotationMethodEndpointMapping to find the endpoint
     *   4. Returns the endpoint with a response-only interceptor for signing the reply
     *
     * COMPARISON WITH JAX-WS (CXF):
     *   CXF uses an interceptor pipeline: decrypt -> dispatch -> process
     *   Spring-WS uses custom mapping:    decrypt -> delegate -> resolve
     *   Both solve the same problem: body must be readable before routing.
     *
     * See DecryptingEndpointMapping.java for the full explanation.
     */
    @Bean
    public DecryptingEndpointMapping decryptingEndpointMapping(
            Wss4jSecurityInterceptor securityInterceptor,
            PayloadRootAnnotationMethodEndpointMapping payloadRootAnnotationMethodEndpointMapping) {
        return new DecryptingEndpointMapping(securityInterceptor, payloadRootAnnotationMethodEndpointMapping);
    }

    /**
     * WS-Security Interceptor (server-side) - Decrypt + Verify + Authenticate.
     *
     * This single bean replaces the entire ServerSecurityHandler.java from JAX-WS.
     *
     * COMPARISON WITH JAX-WS:
     * =======================
     * JAX-WS (CXF):
     *   - Configure WSS4JInInterceptor with properties
     *   - Write a CallbackHandler class
     *   - ~30 lines of configuration
     *
     * Spring-WS (this bean):
     *   - Set validationActions, crypto, and callback handler
     *   - ~15 lines of configuration
     *   - Spring-WS calls WSSecurityEngine for you under the hood
     *
     * WHAT "validationActions" MEANS:
     *   These are the security operations to EXPECT in incoming messages.
     *   The server says "I expect these actions, reject if missing."
     *
     *   "UsernameToken" = expect and validate username/password
     *   "Signature"     = expect and verify digital signature
     *   "Encrypt"       = expect and decrypt encrypted content
     *
     *   Order matters: WSS4J processes them RIGHT-TO-LEFT in the XML,
     *   so "UsernameToken Signature Encrypt" means:
     *     1. Decrypt first  2. Verify signature  3. Check credentials
     */
    @Bean
    public Wss4jSecurityInterceptor securityInterceptor() throws Exception {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();

        // --- INBOUND (validating incoming messages) ---
        // Tell WSS4J to expect all three security layers
        interceptor.setValidationActions("UsernameToken Signature Encrypt");

        // Callback handler for UsernameToken validation + decryption key access.
        //
        // KEY FIX: We use our own ServerPasswordCallbackHandler instead of
        // Spring-WS's SimplePasswordValidationCallbackHandler. The Simple handler
        // only supports USERNAME_TOKEN usage, but decryption requires DECRYPT usage
        // to unlock the server's private key.
        //
        // Our handler handles BOTH:
        //   - USERNAME_TOKEN (id="alice") -> returns "secret123"
        //   - DECRYPT (id="server")      -> returns "serverpass"
        interceptor.setValidationCallbackHandler(new ServerPasswordCallbackHandler());

        // Crypto for SIGNATURE VERIFICATION (needs client's public cert from truststore)
        // and DECRYPTION (needs server's private key from keystore)
        interceptor.setValidationSignatureCrypto(serverCrypto());
        interceptor.setValidationDecryptionCrypto(serverCrypto());

        return interceptor;
    }

    /**
     * Server-side Crypto bean.
     *
     * Loads crypto configuration from server-crypto.properties on the classpath.
     * This tells WSS4J where to find:
     *   - server-keystore.p12   (server's private key, for decryption)
     *   - server-truststore.p12 (client's public cert, for signature verification)
     *
     * JAX-WS equivalent: CryptoFactory.getInstance(props) in the handler constructor.
     */
    @Bean
    public Crypto serverCrypto() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getClassLoader()
                .getResourceAsStream(SecurityConstants.SERVER_CRYPTO_PROPERTIES));
        return CryptoFactory.getInstance(props);
    }

}
