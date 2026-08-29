package com.example.backendtest.graphql;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import com.example.backendtest.common.ApiException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

/**
 * Without this every failure surfaces as INTERNAL_ERROR. GraphQL reports errors in the
 * body with HTTP 200, so the classification is the only signal a client gets.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (!(exception instanceof ApiException apiException)) {
            return null;
        }
        ErrorType type = switch (apiException.getStatus()) {
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            case FORBIDDEN -> ErrorType.FORBIDDEN;
            case UNAUTHORIZED -> ErrorType.UNAUTHORIZED;
            case BAD_REQUEST, CONFLICT, PRECONDITION_FAILED -> ErrorType.BAD_REQUEST;
            default -> ErrorType.INTERNAL_ERROR;
        };
        return GraphqlErrorBuilder.newError(environment)
                .errorType(type)
                .message(apiException.getMessage())
                .build();
    }
}
