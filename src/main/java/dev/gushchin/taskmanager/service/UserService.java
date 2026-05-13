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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

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

    public User create(String email, String passwordHash) {
        User userExisting = userRepository.findByEmail(email);
        if (userExisting != null) {
            throw new UserAlreadyExistsException(email);
        }

        Instant now = Instant.now();

        User user = new User(UUID.randomUUID(), email, email, passwordHash, now, now, false);

        return userRepository.save(user);
    }

    public void deleteById(UUID id) {
        User user = findById(id);
        user.setDeleted(true);
        user.setUpdatedAt(Instant.now());
        userRepository.update(user);
    }
}
