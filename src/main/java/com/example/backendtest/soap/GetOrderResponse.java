package com.example.backendtest.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "GetOrderResponse", namespace = SoapNamespace.URI)
@XmlAccessorType(XmlAccessType.FIELD)
public class GetOrderResponse {

    @XmlElement(namespace = SoapNamespace.URI)
    private SoapOrder order;

    public SoapOrder getOrder() {
        return order;
    }

    public void setOrder(SoapOrder order) {
        this.order = order;
    }
}
