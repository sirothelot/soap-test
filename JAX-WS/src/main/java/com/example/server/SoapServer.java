package com.example.server;

import com.example.service.CalculatorServiceImpl;
import jakarta.xml.ws.Endpoint;

/**
 * SOAP Web Service Server/Publisher
 * 
 * This class publishes (makes available) our web service so clients can access it.
 * 
 * HOW IT WORKS:
 * 1. We create an instance of our service implementation
 * 2. We use Endpoint.publish() to make it available at a URL
 * 3. JAX-WS automatically:
 *    - Creates a simple HTTP server
 *    - Generates the WSDL (Web Service Description Language) document
 *    - Handles incoming SOAP requests and routes them to our implementation
 * 
 * WSDL (Web Service Description Language):
 * - An XML document that describes the web service
 * - Defines what operations are available
 * - Defines the data types used
 * - Defines the endpoint URL
 * - Clients use the WSDL to understand how to call the service
 * 
 * Once started, you can view the WSDL at:
 * http://localhost:8080/calculator?wsdl
 */
public class SoapServer {

    // The URL where the web service will be published
    private static final String SERVICE_URL = "http://localhost:8080/calculator";
    
    private Endpoint endpoint;

    /**
     * Starts the SOAP web service server.
     */
    public void start() {
        System.out.println("========================================");
        System.out.println("   SOAP Web Service Server Starting");
        System.out.println("========================================");
        System.out.println();
        
        // Create an instance of our service implementation
        CalculatorServiceImpl serviceImpl = new CalculatorServiceImpl();
        
        // Publish the service at the specified URL
        // This creates an HTTP server and makes the service available
        endpoint = Endpoint.publish(SERVICE_URL, serviceImpl);
        
        System.out.println("[OK] Service published successfully!");
        System.out.println();
        System.out.println("Service URL: " + SERVICE_URL);
        System.out.println("WSDL URL:    " + SERVICE_URL + "?wsdl");
        System.out.println();
        System.out.println("The WSDL describes the service contract.");
        System.out.println("Open it in a browser to see the XML description.");
        System.out.println();
        System.out.println("Server is running... Press Ctrl+C to stop.");
        System.out.println("========================================");
        System.out.println();
    }

    /**
     * Stops the SOAP web service server.
     */
    public void stop() {
        if (endpoint != null) {
            endpoint.stop();
            System.out.println("Server stopped.");
        }
    }

    /**
     * Main entry point - starts the server.
     */
    public static void main(String[] args) {
        SoapServer server = new SoapServer();
        server.start();
        
        // Add shutdown hook to stop gracefully on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        // Keep the server running
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
