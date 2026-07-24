package com.ledgerly.user;

import com.ledgerly.user.internal.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserWithEncodedPassword() {
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        User user = userService.createUser("admin", "secret", "ADMIN");
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(user.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> userService.createUser("admin", "secret", "ADMIN"))
            .isInstanceOf(DuplicateUserException.class).hasMessageContaining("Username already taken: admin");
    }

    @Test
    void shouldFindUserByUsername() {
        User user = new User("encoded", "hash", "USER");
        when(userRepository.findByUsername("joe")).thenReturn(Optional.of(user));
        Optional<User> result = userService.findByUsername("joe");
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("encoded");
    }

    @Test
    void shouldUpdatePassword() {
        UUID id = UUID.randomUUID();
        User user = new User("joe", "old-hash", "USER");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        User updated = userService.updatePassword(id, "newpass");
        assertThat(updated.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void shouldThrowWhenUpdatingPasswordForMissingUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updatePassword(id, "newpass")).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldDeleteUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);
        userService.deleteUser(id);
        verify(userRepository).deleteById(id);
    }

    @Test
    void shouldThrowWhenDeletingMissingUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);
        assertThatThrownBy(() -> userService.deleteUser(id)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldFindAllUsers() {
        User u1 = new User("a", "h", "USER");
        User u2 = new User("b", "h", "ADMIN");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        List<User> users = userService.findAll();
        assertThat(users).hasSize(2);
    }
}