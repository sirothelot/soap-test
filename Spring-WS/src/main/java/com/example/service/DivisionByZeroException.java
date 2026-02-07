package com.example.service;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Domain exception mapped to a SOAP Fault.
 *
 * This keeps error handling explicit for learners:
 * - We throw a normal Java exception
 * - Spring-WS maps it to a SOAP <Fault> response
 */
@SoapFault(faultCode = FaultCode.CLIENT)
public class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException(String message) {
        super(message);
    }
}
