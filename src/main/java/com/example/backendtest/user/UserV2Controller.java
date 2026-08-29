package com.example.backendtest.user;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backendtest.common.PageResponse;

/** Same data as v1 under a renamed contract — the target for compatibility tests. */
@RestController
@RequestMapping("/api/v2/users")
public class UserV2Controller {

    private final UserService userService;

    public UserV2Controller(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserV2Response> list(@RequestParam(required = false) String name,
                                             @ParameterObject @PageableDefault(sort = "id",
                                                     direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(userService.list(name, pageable), UserV2Response::from);
    }

    @GetMapping("/{id}")
    public UserV2Response getById(@PathVariable Long id) {
        return UserV2Response.from(userService.getView(id));
    }
}
