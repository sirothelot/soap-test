package com.example.client;

import com.example.service.CalculatorService;
import jakarta.xml.ws.Service;
import javax.xml.namespace.QName;
import java.net.URL;

/**
 * SOAP Web Service Client
 * 
 * This client demonstrates how to consume a SOAP web service.
 * 
 * HOW SOAP COMMUNICATION WORKS:
 * 
 * 1. CLIENT SETUP:
 *    - Client gets the WSDL URL
 *    - JAX-WS reads the WSDL to understand the service
 *    - Creates a proxy object that implements the service interface
 * 
 * 2. MAKING A CALL (e.g., add(5, 3)):
 *    - Client calls add(5, 3) on the proxy
 *    - JAX-WS creates a SOAP XML request message:
 *    
 *      <?xml version="1.0" ?>
 *      <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
 *        <S:Body>
 *          <add xmlns="http://service.example.com/">
 *            <a>5</a>
 *            <b>3</b>
 *          </add>
 *        </S:Body>
 *      </S:Envelope>
 *    
 *    - This XML is sent via HTTP POST to the server
 * 
 * 3. SERVER PROCESSING:
 *    - Server receives the SOAP request
 *    - Extracts the operation name (add) and parameters (5, 3)
 *    - Calls the actual add(5, 3) method
 *    - Gets the result (8)
 * 
 * 4. RESPONSE:
 *    - Server creates a SOAP XML response:
 *    
 *      <?xml version="1.0" ?>
 *      <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
 *        <S:Body>
 *          <addResponse xmlns="http://service.example.com/">
 *            <sum>8</sum>
 *          </addResponse>
 *        </S:Body>
 *      </S:Envelope>
 *    
 *    - Sends it back to the client via HTTP
 * 
 * 5. CLIENT RECEIVES:
 *    - JAX-WS parses the response XML
 *    - Extracts the return value (8)
 *    - Returns it from the add() method call
 * 
 * All this XML handling is automatic - you just call Java methods!
 */
public class SoapClient {

    // The WSDL URL - describes the service contract
    private static final String WSDL_URL = "http://localhost:8080/calculator?wsdl";
    
    // QName (Qualified Name) identifies the service in the WSDL
    // Format: {namespace}localName
    private static final QName SERVICE_QNAME = new QName(
        "http://service.example.com/",  // Namespace (must match server)
        "CalculatorWebService"           // Service name (from @WebService)
    );

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   SOAP Web Service Client Demo");
        System.out.println("========================================");
        System.out.println();
        
        try {
            // Step 1: Create a URL pointing to the WSDL
            System.out.println("Step 1: Connecting to WSDL...");
            System.out.println("        URL: " + WSDL_URL);
            URL wsdlUrl = new URL(WSDL_URL);
            
            // Step 2: Create a Service object from the WSDL
            // This reads the WSDL and understands the service structure
            System.out.println("Step 2: Creating service from WSDL...");
            Service service = Service.create(wsdlUrl, SERVICE_QNAME);
            
            // Step 3: Get a proxy (port) that implements our service interface
            // This proxy converts method calls to SOAP messages
            System.out.println("Step 3: Getting service proxy (port)...");
            CalculatorService calculator = service.getPort(CalculatorService.class);
            
            System.out.println("[OK] Connected successfully!");
            System.out.println();
            
            // Step 4: Call the service methods
            // Each call creates a SOAP request, sends it, and parses the response
            System.out.println("========================================");
            System.out.println("   Calling Web Service Operations");
            System.out.println("========================================");
            System.out.println();
            
            // Addition
            System.out.println("Calling: add(10, 5)");
            int sum = calculator.add(10, 5);
            System.out.println("Result:  " + sum);
            System.out.println();
            
            // Subtraction
            System.out.println("Calling: subtract(10, 5)");
            int difference = calculator.subtract(10, 5);
            System.out.println("Result:  " + difference);
            System.out.println();
            
            // Multiplication
            System.out.println("Calling: multiply(10, 5)");
            int product = calculator.multiply(10, 5);
            System.out.println("Result:  " + product);
            System.out.println();
            
            // Division
            System.out.println("Calling: divide(10, 5)");
            double quotient = calculator.divide(10, 5);
            System.out.println("Result:  " + quotient);
            System.out.println();
            
            // More examples
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
            System.err.println("Run: mvn exec:java -Dexec.mainClass=\"com.example.server.SoapServer\"");
            e.printStackTrace();
        }
    }
}
