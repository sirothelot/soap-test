package com.example.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

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
}
