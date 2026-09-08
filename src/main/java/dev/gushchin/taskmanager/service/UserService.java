package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.UserAlreadyExistsException;
import dev.gushchin.taskmanager.exception.UserNotFoundByEmailException;
import dev.gushchin.taskmanager.exception.UserNotFoundByIdException;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String DELETED_EMAIL_PREFIX = "deleted-";
    private static final String DELETED_EMAIL_DOMAIN = "deleted.taskmeplease.invalid";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public User findById(UUID id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new UserNotFoundByIdException(id);
        }

        return user;
    }

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundByEmailException(email);
        }

        return user;
    }

    public List<User> findAll() {
        return userRepository.findAll().stream()
                .filter(Predicate.not(User::isDeleted))
                .toList();
    }

    public User create(String email, String name, String password) {
        User userExisting = userRepository.findByEmail(email);
        if (userExisting != null) {
            throw new UserAlreadyExistsException(email);
        }

        Instant now = Instant.now();
        String passwordHash = passwordEncoder.encode(password);
        String userName = name == null || name.isBlank() ? email : name;

        User user = new User(UUID.randomUUID(), email, userName, passwordHash, now, now, false, null, false);

        User savedUser = userRepository.save(user);
        notificationService.claimInvitations(savedUser.getId(), savedUser.getEmail());

        return savedUser;
    }

    public User create(String email, String password) {
        return create(email, null, password);
    }

    public User updateName(UUID id, String name) {
        User user = findById(id);
        user.setName(name == null ? "" : name.strip());
        user.setUpdatedAt(Instant.now());

        return userRepository.update(user);
    }

    public void deleteById(UUID id) {
        User user = findById(id);
        user.setEmail(DELETED_EMAIL_PREFIX + user.getId() + "@" + DELETED_EMAIL_DOMAIN);
        user.setDeleted(true);
        user.setUpdatedAt(Instant.now());
        userRepository.update(user);
    }
}
