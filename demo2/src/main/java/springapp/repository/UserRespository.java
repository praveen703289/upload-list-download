package springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import springapp.model.User;


public interface UserRespository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
