package com.example.backendtest.user;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backendtest.auth.AuthInterceptor;
import com.example.backendtest.common.ApiException;
import com.example.backendtest.common.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(required = false) String name,
                                           @ParameterObject @PageableDefault(sort = "id",
                                                   direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(userService.list(name, pageable), UserResponse::from);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getView(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequests.Create request,
                                               @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireAdmin(role);
        User user = userService.create(request.name(), request.email(), request.password(), request.age(),
                User.ROLE_USER);
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId())).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    public UserResponse replace(@PathVariable Long id, @Valid @RequestBody UserRequests.Replace request,
                                @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireSelfOrAdmin(id, callerId, role);
        return UserResponse.from(userService.replace(id, request));
    }

    @PatchMapping("/{id}")
    public UserResponse patch(@PathVariable Long id, @Valid @RequestBody UserRequests.Patch request,
                              @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                              @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireSelfOrAdmin(id, callerId, role);
        return UserResponse.from(userService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireAdmin(role);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(String role) {
        if (!User.ROLE_ADMIN.equals(role)) {
            throw ApiException.forbidden("Requires role ADMIN");
        }
    }

    /** Editing someone else's user is the broken-access-control case worth probing. */
    private void requireSelfOrAdmin(Long targetId, Long callerId, String role) {
        if (!User.ROLE_ADMIN.equals(role) && !targetId.equals(callerId)) {
            throw ApiException.forbidden("You may only modify your own user");
        }
    }
}
