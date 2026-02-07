package com.example.service;

import com.example.service.gen.*;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * SOAP Web Service Endpoint (Spring-WS).
 *
 * COMPARISON WITH JAX-WS:
 * =======================
 * JAX-WS uses:
 *   @WebService, @WebMethod, @WebParam, @WebResult
 *   - You write a Java interface + implementation
 *   - Methods take simple Java types (int, double)
 *   - JAX-WS auto-marshals to/from XML
 *
 * Spring-WS uses:
 *   @Endpoint, @PayloadRoot, @RequestPayload, @ResponsePayload
 *   - You write an endpoint class (no interface needed)
 *   - Methods take/return JAXB-generated request/response objects
 *   - Spring marshals to/from XML using JAXB
 *
 * ANNOTATION MAPPING:
 *   JAX-WS @WebService       -> Spring-WS @Endpoint
 *   JAX-WS @WebMethod        -> Spring-WS @PayloadRoot
 *   JAX-WS @WebParam         -> Spring-WS @RequestPayload
 *   JAX-WS @WebResult        -> Spring-WS @ResponsePayload
 *
 * When a SOAP request comes in:
 * 1. Spring-WS receives the XML SOAP message
 * 2. It matches the XML namespace + localPart to find the right @PayloadRoot method
 * 3. It unmarshals the XML body into a JAXB request object (e.g., AddRequest)
 * 4. It calls this method with the request object
 * 5. It marshals the response object (e.g., AddResponse) back to XML
 * 6. It sends the SOAP response back to the client
 */
@Endpoint
public class CalculatorEndpoint {

    private static final String NAMESPACE = "http://service.example.com/";

    /**
     * Handles the "add" SOAP operation.
     *
     * JAX-WS equivalent:
     *   @WebMethod(operationName = "add")
     *   @WebResult(name = "sum")
     *   int add(@WebParam(name = "a") int a, @WebParam(name = "b") int b);
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "addRequest")
    @ResponsePayload
    public AddResponse add(@RequestPayload AddRequest request) {
        System.out.println("Server: Received add request - a=" + request.getA() + ", b=" + request.getB());
        int result = request.getA() + request.getB();
        System.out.println("Server: Returning result = " + result);

        AddResponse response = new AddResponse();
        response.setSum(result);
        return response;
    }

    /**
     * Handles the "subtract" SOAP operation.
     *
     * JAX-WS equivalent:
     *   @WebMethod(operationName = "subtract")
     *   @WebResult(name = "difference")
     *   int subtract(@WebParam(name = "a") int a, @WebParam(name = "b") int b);
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "subtractRequest")
    @ResponsePayload
    public SubtractResponse subtract(@RequestPayload SubtractRequest request) {
        System.out.println("Server: Received subtract request - a=" + request.getA() + ", b=" + request.getB());
        int result = request.getA() - request.getB();
        System.out.println("Server: Returning result = " + result);

        SubtractResponse response = new SubtractResponse();
        response.setDifference(result);
        return response;
    }

    /**
     * Handles the "multiply" SOAP operation.
     *
     * JAX-WS equivalent:
     *   @WebMethod(operationName = "multiply")
     *   @WebResult(name = "product")
     *   int multiply(@WebParam(name = "a") int a, @WebParam(name = "b") int b);
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "multiplyRequest")
    @ResponsePayload
    public MultiplyResponse multiply(@RequestPayload MultiplyRequest request) {
        System.out.println("Server: Received multiply request - a=" + request.getA() + ", b=" + request.getB());
        int result = request.getA() * request.getB();
        System.out.println("Server: Returning result = " + result);

        MultiplyResponse response = new MultiplyResponse();
        response.setProduct(result);
        return response;
    }

    /**
     * Handles the "divide" SOAP operation.
     *
     * JAX-WS equivalent:
     *   @WebMethod(operationName = "divide")
     *   @WebResult(name = "quotient")
     *   double divide(@WebParam(name = "a") int a, @WebParam(name = "b") int b);
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "divideRequest")
    @ResponsePayload
    public DivideResponse divide(@RequestPayload DivideRequest request) {
        System.out.println("Server: Received divide request - a=" + request.getA() + ", b=" + request.getB());
        if (request.getB() == 0) {
            throw new DivisionByZeroException("Cannot divide by zero!");
        }
        double result = (double) request.getA() / request.getB();
        System.out.println("Server: Returning result = " + result);

        DivideResponse response = new DivideResponse();
        response.setQuotient(result);
        return response;
    }
}
