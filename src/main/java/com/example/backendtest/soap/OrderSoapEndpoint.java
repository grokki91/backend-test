package com.example.backendtest.soap;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.example.backendtest.order.Order;
import com.example.backendtest.order.OrderRepository;

/** The SOAP face of the order data: same rows, XML envelope, WSDL contract. */
@Endpoint
public class OrderSoapEndpoint {

    private final OrderRepository orders;

    public OrderSoapEndpoint(OrderRepository orders) {
        this.orders = orders;
    }

    @PayloadRoot(namespace = SoapNamespace.URI, localPart = "GetOrderRequest")
    @ResponsePayload
    public GetOrderResponse getOrder(@RequestPayload GetOrderRequest request) {
        Order order = orders.findById(request.getId()).orElseThrow(() -> new OrderNotFoundFault(request.getId()));

        SoapOrder payload = new SoapOrder();
        payload.setId(order.getId());
        payload.setUserId(order.getUserId());
        payload.setStatus(order.getStatus().name());
        payload.setAmount(order.getAmount());
        payload.setCurrency(order.getCurrency());

        GetOrderResponse response = new GetOrderResponse();
        response.setOrder(payload);
        return response;
    }
}
