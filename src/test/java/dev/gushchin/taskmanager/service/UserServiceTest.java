package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gushchin.taskmanager.exception.UserAlreadyExistsException;
import dev.gushchin.taskmanager.exception.UserNotFoundByEmailException;
import dev.gushchin.taskmanager.exception.UserNotFoundByIdException;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = new UserService(userRepository, passwordEncoder);

    @Test
    void createShouldReturnSavedUser() {
        // given
        String email = "user@test.com";
        String name = "Mike";
        String password = "qwerty";

        when(userRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn(password);
        when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        User createdUser = userService.create(email, name, password);

        // then
        assertNotNull(createdUser.getId());
        assertEquals(email, createdUser.getEmail());
        assertEquals(name, createdUser.getName());
        assertEquals(password, createdUser.getPasswordHash());
        assertFalse(createdUser.isDeleted());

        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserWithNameNullShouldReturnSavedUser() {
        // given
        String email = "user@test.com";
        String name = null;
        String password = "qwerty";

        when(userRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn(password);
        when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        User createdUser = userService.create(email, name, password);

        // then
        assertNotNull(createdUser.getId());
        assertEquals(email, createdUser.getEmail());
        assertEquals(email, createdUser.getName());
        assertEquals(password, createdUser.getPasswordHash());
        assertFalse(createdUser.isDeleted());

        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserWithNameIsBlankShouldReturnSavedUser() {
        // given
        String email = "user@test.com";
        String name = "    ";
        String password = "qwerty";

        when(userRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn(password);
        when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        User createdUser = userService.create(email, name, password);

        // then
        assertNotNull(createdUser.getId());
        assertEquals(email, createdUser.getEmail());
        assertEquals(email, createdUser.getName());
        assertEquals(password, createdUser.getPasswordHash());
        assertFalse(createdUser.isDeleted());

        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createShouldThrowUserAlreadyExists() {
        String email = "user23@test.com";
        String name = "Mike";
        String password = "qwerty01";

        when(userRepository.findByEmail(email)).thenReturn(new User());

        assertThrows(UserAlreadyExistsException.class, () -> userService.create(email, name, password));
        verify(userRepository).findByEmail(email);
    }

    @Test
    void findByIdShouldReturnUser() {
        UUID id = UUID.randomUUID();
        User expectedUser = new User();
        expectedUser.setId(id);
        expectedUser.setEmail("user@test.com");
        expectedUser.setName("Mike");
        expectedUser.setPasswordHash("qwerty");
        expectedUser.setDeleted(false);

        when(userRepository.findById(id)).thenReturn(expectedUser);

        User actualUser = userService.findById(id);

        assertEquals(expectedUser, actualUser);
        verify(userRepository).findById(id);
    }

    @Test
    void findByIdShouldThrowUserNotFoundById() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(null);

        assertThrows(UserNotFoundByIdException.class, () -> userService.findById(id));
        verify(userRepository).findById(id);
    }

    @Test
    void findByEmailShouldReturnUser() {
        String email = "find@test.com";
        User expectedUser = new User();
        expectedUser.setId(UUID.randomUUID());
        expectedUser.setEmail(email);
        expectedUser.setName(email);

        when(userRepository.findByEmail(email)).thenReturn(expectedUser);

        User actualUser = userService.findByEmail(email);

        assertEquals(expectedUser, actualUser);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void findByEmailShouldThrowUserNotFoundByEmail() {
        String email = "missing@test.com";

        when(userRepository.findByEmail(email)).thenReturn(null);

        assertThrows(UserNotFoundByEmailException.class, () -> userService.findByEmail(email));
        verify(userRepository).findByEmail(email);
    }

    @Test
    void findAllShouldReturnOnlyNotDeletedUsers() {
        User activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("active@test.com");
        activeUser.setDeleted(false);

        User deletedUser = new User();
        deletedUser.setId(UUID.randomUUID());
        deletedUser.setEmail("deleted@test.com");
        deletedUser.setDeleted(true);

        when(userRepository.findAll()).thenReturn(List.of(activeUser, deletedUser));

        List<User> users = userService.findAll();

        assertEquals(1, users.size());
        assertEquals("active@test.com", users.getFirst().getEmail());
        verify(userRepository).findAll();
    }

    @Test
    void deleteByIdShouldMarkUserAsDeletedAndCallUpdate() {
        UUID id = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("user@test.com");
        existingUser.setDeleted(false);

        when(userRepository.findById(id)).thenReturn(existingUser);
        when(userRepository.update(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        userService.deleteById(id);

        assertTrue(existingUser.isDeleted());
        verify(userRepository).findById(id);
        verify(userRepository).update(existingUser);
    }
}
