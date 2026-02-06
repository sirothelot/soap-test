package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application entry point.
 *
 * COMPARISON WITH JAX-WS:
 * =======================
 * JAX-WS:    Uses Endpoint.publish() to start a lightweight HTTP server manually.
 * Spring-WS: Uses Spring Boot's embedded Tomcat (auto-configured).
 *            Just annotate with @SpringBootApplication and run.
 *
 * Spring Boot automatically:
 * - Starts an embedded web server (Tomcat)
 * - Scans for @Endpoint, @Configuration, and other Spring annotations
 * - Wires everything together
 */
@SpringBootApplication
public class SoapApplication {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   Spring-WS SOAP Server Starting");
        System.out.println("========================================");
        System.out.println();

        SpringApplication.run(SoapApplication.class, args);

        System.out.println();
        System.out.println("[OK] Service published successfully!");
        System.out.println();
        System.out.println("Service URL: http://localhost:8080/ws");
        System.out.println("WSDL URL:    http://localhost:8080/ws/calculator.wsdl");
        System.out.println();
        System.out.println("Server is running... Press Ctrl+C to stop.");
        System.out.println("========================================");
    }
}
