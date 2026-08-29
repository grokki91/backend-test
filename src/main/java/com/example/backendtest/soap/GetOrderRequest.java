package com.example.backendtest.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "GetOrderRequest", namespace = SoapNamespace.URI)
@XmlAccessorType(XmlAccessType.FIELD)
public class GetOrderRequest {

    @XmlElement(namespace = SoapNamespace.URI)
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
