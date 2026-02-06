package com.example.config;

import com.example.security.SecurityConstants;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.callback.SimplePasswordValidationCallbackHandler;
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

    // =========================================================================
    //  WS-SECURITY CONFIGURATION
    // =========================================================================

    /**
     * Register the WS-Security interceptor in the endpoint chain.
     *
     * COMPARISON WITH JAX-WS:
     * =======================
     * JAX-WS:    Handlers are added to a Binding's handler chain manually.
     *              Binding binding = endpoint.getBinding();
     *              binding.getHandlerChain().add(new ServerSecurityHandler());
     *
     * Spring-WS: Interceptors are registered by overriding addInterceptors().
     *            Spring automatically applies them to all @Endpoint methods.
     */
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(securityInterceptor());
    }

    /**
     * WS-Security Interceptor (server-side) - validates incoming credentials.
     *
     * This is the Spring-WS equivalent of our JAX-WS ServerSecurityHandler,
     * but instead of manually parsing XML, we just set properties:
     *
     * COMPARISON:
     * ===========
     * JAX-WS (ServerSecurityHandler.java):
     *   - Manually iterates through SOAP header elements
     *   - Finds <wsse:Security> -> <wsse:UsernameToken> -> <wsse:Username>/<wsse:Password>
     *   - Compares values against expected credentials
     *   - ~130 lines of XML parsing code
     *
     * Spring-WS (this bean):
     *   - Set validationActions = "UsernameToken"
     *   - Provide a callback handler with valid username/password pairs
     *   - WSS4J handles ALL the XML parsing automatically
     *   - ~15 lines of configuration code
     *
     * WHAT "validationActions" MEANS:
     *   "UsernameToken" = check for username/password in the security header
     *   "Timestamp"     = check for timestamp (prevents replay attacks)
     *   "Signature"     = verify digital signature
     *   "Encrypt"       = decrypt encrypted content
     *   You can combine them: "UsernameToken Timestamp"
     */
    @Bean
    public Wss4jSecurityInterceptor securityInterceptor() {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();

        // Tell WSS4J to look for a UsernameToken in incoming messages
        interceptor.setValidationActions("UsernameToken");

        // Provide the callback handler that knows valid username/password pairs
        interceptor.setValidationCallbackHandler(securityCallbackHandler());

        return interceptor;
    }

    /**
     * Password validation callback handler.
     *
     * When WSS4J finds a UsernameToken in an incoming message, it calls this
     * callback to check if the credentials are valid.
     *
     * SimplePasswordValidationCallbackHandler takes a Properties object
     * where keys=usernames, values=passwords.
     *
     * JAX-WS equivalent: The if-statement in ServerSecurityHandler.handleMessage()
     *   that compares username.equals("alice") && password.equals("secret123")
     *
     * IN PRODUCTION: Replace this with a callback handler that checks
     * a database, LDAP, or calls an identity provider.
     */
    @Bean
    public SimplePasswordValidationCallbackHandler securityCallbackHandler() {
        SimplePasswordValidationCallbackHandler handler = new SimplePasswordValidationCallbackHandler();
        Properties users = new Properties();
        users.setProperty(SecurityConstants.USERNAME, SecurityConstants.PASSWORD);
        handler.setUsers(users);
        return handler;
    }
}
