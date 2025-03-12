package springapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import springapp.model.User;
import springapp.repository.UserRespository;

@Service
public class UserService {

    @Autowired
    private UserRespository userRepository;

    public User saveUser(User user) {
        validateUser(user);
        User existingUserByUsername = userRepository.findByUsernameIgnoreCase(user.getUsername());

        User existingUserByEmail = userRepository.findByEmailIgnoreCase(user.getEmail());

        if (existingUserByUsername != null && existingUserByEmail != null) {
            throw new IllegalArgumentException("this user already exists with this email so you can update your email.");
        } else if (existingUserByUsername != null) {
            existingUserByUsername.setEmail(user.getEmail());
            return userRepository.save(existingUserByUsername);
        } else if (existingUserByEmail != null) {
            throw new IllegalArgumentException("Email already exists with another user. Please change your email.");
        }
        return userRepository.save(user);
    }

    private void validateUser(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }

        if (user.getUsername().length() < 3 || user.getUsername().length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters.");
        }

        if (!isValidEmailFormat(user.getEmail())) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        if (containsWhitespace(user.getEmail())) {
            throw new IllegalArgumentException("Email cannot contain whitespace.");
        }

        if (hasMultipleAtSymbols(user.getEmail())) {
            throw new IllegalArgumentException("Email cannot contain more than one '@' symbol.");
        }

        if (isInvalidEmailDomain(user.getEmail())) {
            throw new IllegalArgumentException("Email domain is invalid.");
        }

        if (user.getEmail().length() > 254) {
            throw new IllegalArgumentException("Email is too long. Maximum length is 254 characters.");
        }

        if (startsWithNumber(user.getEmail())) {
            throw new IllegalArgumentException("Email cannot start with a number.");
        }

        if (emailContainsSequentialRepeatingCharacters(user.getEmail())) {
            throw new IllegalArgumentException("Email cannot contain sequential repeating characters (e.g., 'aaa').");
        }

        if (emailContainsConsecutiveDots(user.getEmail())) {
            throw new IllegalArgumentException("Email cannot have consecutive dots (e.g., 'john..doe@example.com').");
        }

        if (containsSpaces(user.getUsername())) {
            throw new IllegalArgumentException("Username cannot contain spaces.");
        }

        if (containsSpecialCharacters(user.getUsername())) {
            throw new IllegalArgumentException("Username cannot contain special characters.");
        }

        if (!startsWithLetter(user.getUsername())) {
            throw new IllegalArgumentException("Username must start with a letter.");
        }

        if (usernameContainsConsecutiveRepeatingCharacters(user.getUsername())) {
            throw new IllegalArgumentException("Username cannot contain sequential repeating characters (e.g., 'aaa').");
        }

        if (usernameContainsDigitsOnly(user.getUsername())) {
            throw new IllegalArgumentException("Username cannot contain only numbers.");
        }

        if (usernameContainsUnderscoreAtStartOrEnd(user.getUsername())) {
            throw new IllegalArgumentException("Username cannot start or end with an underscore.");
        }
    }

    // Validate email format using regex
    private boolean isValidEmailFormat(String email) {
        String emailRegex = "^[A-Za-z][A-Za-z0-9._%+-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean containsWhitespace(String email) {
        return email.contains(" ");
    }

    private boolean hasMultipleAtSymbols(String email) {
        int atCount = email.length() - email.replace("@", "").length();
        return atCount > 1;
    }

    private boolean isInvalidEmailDomain(String email) {
        String domainPart = email.substring(email.indexOf('@') + 1);
        return !domainPart.contains(".");
    }

    private boolean startsWithNumber(String email) {
        String localPart = email.split("@")[0];  
        return localPart.length() > 0 && Character.isDigit(localPart.charAt(0));
    }

    private boolean emailContainsSequentialRepeatingCharacters(String email) {
        return email.matches(".*(.)\\1{2,}.*");
    }

    private boolean emailContainsConsecutiveDots(String email) {
        return email.contains("..");
    }

    private boolean containsSpaces(String username) {
        return username.contains(" ");
    }

    private boolean containsSpecialCharacters(String username) {
        String specialCharacters = "[^a-zA-Z0-9_]"; 
        return username.matches(".*" + specialCharacters + ".*");
    }

    private boolean startsWithLetter(String username) {
        return Character.isLetter(username.charAt(0));
    }

    private boolean usernameContainsConsecutiveRepeatingCharacters(String username) {
        return username.matches(".*(.)\\1{2,}.*");
    }

    private boolean usernameContainsDigitsOnly(String username) {
        return username.matches("\\d+");
    }

    private boolean usernameContainsUnderscoreAtStartOrEnd(String username) {
        return username.startsWith("_") || username.endsWith("_");
    }
}
