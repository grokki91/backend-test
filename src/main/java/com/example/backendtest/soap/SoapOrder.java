package com.example.backendtest.soap;

import java.math.BigDecimal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "Order", namespace = SoapNamespace.URI)
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapOrder {

    @XmlElement(namespace = SoapNamespace.URI)
    private long id;

    @XmlElement(namespace = SoapNamespace.URI)
    private long userId;

    @XmlElement(namespace = SoapNamespace.URI)
    private String status;

    @XmlElement(namespace = SoapNamespace.URI)
    private BigDecimal amount;

    @XmlElement(namespace = SoapNamespace.URI)
    private String currency;

    public void setId(long id) {
        this.id = id;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
