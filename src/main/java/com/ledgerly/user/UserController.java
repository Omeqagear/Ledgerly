package com.ledgerly.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createUser(request.username(), request.password(), request.role()));
    }

    @GetMapping
    public Page<UserResponse> list(Pageable pageable) {
        return userService.findAll(pageable).map(UserResponse::from);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(userService.findById(id).orElseThrow(() -> new UserNotFoundException(id)));
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable UUID id, @Valid @RequestBody ChangePasswordRequest request) {
        userService.updatePassword(id, request.password());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.deleteUser(id);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<String> notFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserException.class)
    ResponseEntity<String> duplicate(DuplicateUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    public record CreateUserRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 6, max = 128) String password,
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "ADMIN|USER", message = "role must be ADMIN or USER") String role
    ) {}

    public record ChangePasswordRequest(
        @NotBlank @Size(min = 6, max = 128) String password
    ) {}
}
