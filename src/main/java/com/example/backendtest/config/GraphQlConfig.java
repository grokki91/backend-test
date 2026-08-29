package com.example.backendtest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.scalars.ExtendedScalars;

/** Money stays a BigDecimal in GraphQL too, rather than degrading to a float. */
@Configuration
public class GraphQlConfig {

    @Bean
    RuntimeWiringConfigurer bigDecimalScalar() {
        return wiring -> wiring.scalar(ExtendedScalars.GraphQLBigDecimal);
    }
}
