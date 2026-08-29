package com.example.backendtest.soap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/** Maps to a real SOAP Fault with faultcode Client, not an HTTP error code. */
@SoapFault(faultCode = FaultCode.CLIENT)
public class OrderNotFoundFault extends RuntimeException {

    public OrderNotFoundFault(long id) {
        super("Order not found: " + id);
    }
}
