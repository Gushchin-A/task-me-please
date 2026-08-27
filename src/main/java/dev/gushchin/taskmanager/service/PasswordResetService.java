package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.InvalidAccountTokenException;
import dev.gushchin.taskmanager.exception.InvalidPasswordResetRequestException;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.AccountTokenRepository;
import dev.gushchin.taskmanager.repository.PersistentLoginRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords не совпадают";
    private static final String PASSWORD_REQUIRED_MESSAGE = "Пароль не заполнен";

    private final AccountTokenRepository accountTokenRepository;
    private final AccountTokenService accountTokenService;
    private final PasswordEncoder passwordEncoder;
    private final PersistentLoginRepository persistentLoginRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public AccountToken findValid(String token) {
        AccountToken accountToken = accountTokenService.findValid(token, AccountTokenType.PASSWORD_RESET);
        findEligibleUser(accountToken);

        return accountToken;
    }

    @Transactional
    public User reset(String token, String password, String passwordConfirmation) {
        findValid(token);
        validatePasswords(password, passwordConfirmation);
        AccountToken accountToken = accountTokenService.consume(token, AccountTokenType.PASSWORD_RESET);
        User user = findEligibleUser(accountToken);
        Instant now = Instant.now();

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUpdatedAt(now);
        User updatedUser = userRepository.update(user);
        accountTokenRepository.invalidateActiveByUserIdAndType(user.getId(), AccountTokenType.PASSWORD_RESET, now);
        persistentLoginRepository.deleteByUsername(user.getEmail());

        return updatedUser;
    }

    private User findEligibleUser(AccountToken accountToken) {
        User user = userService.findById(accountToken.getUserId());
        if (user.isDeleted() || !user.isEmailVerified()) {
            throw new InvalidAccountTokenException();
        }

        return user;
    }

    private void validatePasswords(String password, String passwordConfirmation) {
        if (password == null || password.isBlank()) {
            throw new InvalidPasswordResetRequestException(PASSWORD_REQUIRED_MESSAGE);
        }

        if (!password.equals(passwordConfirmation)) {
            throw new InvalidPasswordResetRequestException(PASSWORD_MISMATCH_MESSAGE);
        }
    }
}
