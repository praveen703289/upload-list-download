package springapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import springapp.model.UserAttachment;

public interface AttachmentRepository extends JpaRepository<UserAttachment, Long> {
    
	Optional<UserAttachment> findByFileName(String filename);
}