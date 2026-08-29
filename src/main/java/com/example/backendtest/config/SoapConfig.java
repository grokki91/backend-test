package com.example.backendtest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import com.example.backendtest.soap.SoapNamespace;

/**
 * Serves a generated WSDL at /soap/orders.wsdl.
 *
 * <p>Deliberately no {@code @EnableWs} here: declaring it would define a
 * WsConfigurationSupport bean, which makes Boot's WebServicesAutoConfiguration back off
 * and leaves MessageDispatcherServlet unregistered. Boot already enables WS itself.
 */
@Configuration
public class SoapConfig {

    @Bean
    XsdSchema ordersSchema() {
        return new SimpleXsdSchema(new ClassPathResource("soap/orders.xsd"));
    }

    @Bean(name = "orders")
    DefaultWsdl11Definition ordersWsdl(XsdSchema ordersSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("OrdersPort");
        definition.setLocationUri("/soap");
        definition.setTargetNamespace(SoapNamespace.URI);
        definition.setSchema(ordersSchema);
        return definition;
    }
}
