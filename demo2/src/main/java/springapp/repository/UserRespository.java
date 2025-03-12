package springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import springapp.model.User;

public interface UserRespository extends JpaRepository<User, Long> {

    // Custom query method to check for username existence, case-insensitive
    boolean existsByUsernameIgnoreCase(String username);

    // Custom query method to check for email existence, case-insensitive
    boolean existsByEmailIgnoreCase(String email);
    
    User findByUsernameIgnoreCase(String username);
    User findByEmailIgnoreCase(String email);


}
