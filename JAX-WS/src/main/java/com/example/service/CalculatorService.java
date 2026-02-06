package com.example.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

/**
 * SOAP Web Service Interface (SEI - Service Endpoint Interface)
 * 
 * This interface defines the contract for our SOAP web service.
 * The @WebService annotation marks this as a JAX-WS web service.
 * 
 * KEY CONCEPTS:
 * - @WebService: Marks this interface as a web service endpoint
 * - @WebMethod: Marks a method as a web service operation
 * - @WebParam: Customizes the parameter name in the WSDL/SOAP message
 * - @WebResult: Customizes the return value name in the WSDL/SOAP message
 */
@WebService(
    name = "CalculatorService",           // Name of the service in WSDL
    targetNamespace = "http://service.example.com/"  // XML namespace
)
public interface CalculatorService {

    /**
     * Adds two numbers together.
     * 
     * @param a First number
     * @param b Second number
     * @return Sum of a and b
     */
    @WebMethod(operationName = "add")
    @WebResult(name = "sum")
    int add(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );

    /**
     * Subtracts second number from first.
     * 
     * @param a First number
     * @param b Second number
     * @return Difference (a - b)
     */
    @WebMethod(operationName = "subtract")
    @WebResult(name = "difference")
    int subtract(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );

    /**
     * Multiplies two numbers.
     * 
     * @param a First number
     * @param b Second number
     * @return Product of a and b
     */
    @WebMethod(operationName = "multiply")
    @WebResult(name = "product")
    int multiply(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );

    /**
     * Divides first number by second.
     * 
     * @param a Dividend
     * @param b Divisor
     * @return Quotient (a / b)
     * @throws ArithmeticException if b is zero
     */
    @WebMethod(operationName = "divide")
    @WebResult(name = "quotient")
    double divide(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );
}
