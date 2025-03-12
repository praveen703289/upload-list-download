package springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import springapp.model.UserAttachment;

public interface AttachmentRepository extends JpaRepository<UserAttachment, Long> {

    // Method to find files uploaded by a specific user (without pagination)
	Page<UserAttachment> findByUserId(Long userId, Pageable pageable);

    // Method to find a file by its name (existing functionality)
    UserAttachment findByFileName(String filename);
    List<UserAttachment> findByFileNameStartingWith(String fileName);
}

