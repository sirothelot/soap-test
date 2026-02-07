package com.example.service;

import jakarta.jws.WebService;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;

import javax.xml.namespace.QName;

/**
 * SOAP Web Service Implementation (SIB - Service Implementation Bean)
 * 
 * This class provides the actual implementation of the web service operations.
 * The @WebService annotation links this implementation to the interface.
 * 
 * KEY CONCEPTS:
 * - endpointInterface: Points to the service interface (SEI)
 * - serviceName: The name of the service as it appears in the WSDL
 * - portName: The name of the port in the WSDL
 * 
 * When a SOAP request comes in:
 * 1. The JAX-WS runtime receives the XML SOAP message
 * 2. It unmarshals (converts) the XML to Java objects
 * 3. It calls the appropriate method on this implementation
 * 4. It marshals (converts) the return value back to XML
 * 5. It sends the SOAP response back to the client
 */
@WebService(
    endpointInterface = "com.example.service.CalculatorService",
    serviceName = "CalculatorWebService",
    portName = "CalculatorPort",
    targetNamespace = "http://service.example.com/"
)
@SchemaValidation(type = SchemaValidationType.BOTH)
public class CalculatorServiceImpl implements CalculatorService {

    @Override
    public int add(int a, int b) {
        System.out.println("Server: Received add request - a=" + a + ", b=" + b);
        int result = a + b;
        System.out.println("Server: Returning result = " + result);
        return result;
    }

    @Override
    public int subtract(int a, int b) {
        System.out.println("Server: Received subtract request - a=" + a + ", b=" + b);
        int result = a - b;
        System.out.println("Server: Returning result = " + result);
        return result;
    }

    @Override
    public int multiply(int a, int b) {
        System.out.println("Server: Received multiply request - a=" + a + ", b=" + b);
        int result = a * b;
        System.out.println("Server: Returning result = " + result);
        return result;
    }

    @Override
    public double divide(int a, int b) {
        System.out.println("Server: Received divide request - a=" + a + ", b=" + b);
        if (b == 0) {
            throw createSoapFault("Cannot divide by zero!", "DIVIDE_BY_ZERO");
        }
        double result = (double) a / b;
        System.out.println("Server: Returning result = " + result);
        return result;
    }

    /**
     * Create a SOAP Fault that is visible to clients.
     */
    private SOAPFaultException createSoapFault(String message, String code) {
        try {
            SOAPFault fault = SOAPFactory.newInstance().createFault(
                    message,
                    new QName(SOAPConstants.URI_NS_SOAP_ENVELOPE, "Client")
            );
            fault.addDetail()
                    .addDetailEntry(new QName("http://service.example.com/", "error"))
                    .addTextNode(code);
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new RuntimeException("Failed to create SOAP Fault", e);
        }
    }
}
