package com.example.backendtest.graphql;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.backendtest.order.Order;
import com.example.backendtest.order.OrderRepository;
import com.example.backendtest.order.OrderRequests;
import com.example.backendtest.order.OrderService;
import com.example.backendtest.order.OrderStatus;
import com.example.backendtest.user.User;
import com.example.backendtest.user.UserRepository;
import com.example.backendtest.user.UserService;

import com.example.backendtest.common.ApiException;

/**
 * The same data as the REST API, over GraphQL — so query shaping, partial selection and
 * GraphQL's "errors with a 200" convention can be tested side by side with REST.
 */
@Controller
public class GraphQlApi {

    private final UserRepository users;
    private final UserService userService;
    private final OrderRepository orders;
    private final OrderService orderService;

    public GraphQlApi(UserRepository users, UserService userService, OrderRepository orders,
                      OrderService orderService) {
        this.users = users;
        this.userService = userService;
        this.orders = orders;
        this.orderService = orderService;
    }

    @QueryMapping
    public List<User> users(@Argument Integer page, @Argument Integer size) {
        return users.findAll(PageRequest.of(page == null ? 0 : page, size == null ? 10 : size)).getContent();
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return userService.get(id);
    }

    @QueryMapping
    public List<Order> orders(@Argument OrderStatus status, @Argument Integer page, @Argument Integer size) {
        PageRequest pageRequest = PageRequest.of(page == null ? 0 : page, size == null ? 10 : size);
        return status == null
                ? orders.findAll(pageRequest).getContent()
                : orders.findByStatus(status, pageRequest).getContent();
    }

    @QueryMapping
    public Order order(@Argument Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order", id));
    }

    @MutationMapping
    public Order createOrder(@Argument Long userId, @Argument BigDecimal amount, @Argument String currency,
                             @Argument String comment) {
        userService.get(userId);
        return orderService.create(userId, new OrderRequests.Create(amount, currency, comment), null).order();
    }

    @MutationMapping
    public Order cancelOrder(@Argument Long id) {
        Order order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order", id));
        return orderService.transition(id, OrderStatus.CANCELLED, order.getUserId(), User.ROLE_ADMIN);
    }
}
