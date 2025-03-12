package springapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import springapp.model.User;
import springapp.model.UserAttachment;
import springapp.repository.AttachmentRepository;
import springapp.repository.UserRespository;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DemoService {

    private final S3Client s3Client;
    private final UserRespository userRepository;
    private final AttachmentRepository attachmentRepo;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    public DemoService(S3Client s3Client, UserRespository userRepository, AttachmentRepository attachmentRepo) {
        this.s3Client = s3Client;
        this.userRepository = userRepository;
        this.attachmentRepo = attachmentRepo;
    }
    //uploading file------------------------------------------------------------------------------------------------------------------------
    public String uploadFileAndCreateAttachment(MultipartFile file, Long userId) throws IOException {
        if (userId == null) {
            throw new IllegalArgumentException("Enter userId");
        }
        if (userId < 1) {
            throw new IllegalArgumentException("Enter a valid userId");
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new IllegalArgumentException("Invalid User ID.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        String fileType = file.getContentType();
        if (!fileType.equals("image/jpeg") && 
            !fileType.equals("image/png") && 
            !fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new IllegalArgumentException("Invalid file type. Enter only JPG, PNG, and DOCX files.");
        }

        UserAttachment attachment = new UserAttachment();
        attachment.setFileName(fileName);
        attachment.setFileType(fileType);
        attachment.setLastUpdatedOn(LocalDateTime.now());
        attachment.setUser(userOptional.get());

        // Save attachment first to get the generated attachmentId
        attachment = attachmentRepo.save(attachment);
        Long attachmentId = attachment.getId(); 
        String s3FileName = attachmentId + "_" + fileName;
        try (var inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3FileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        }

        return "File uploaded successfully. Attachment ID: " + attachmentId+ " and filename "+fileName;
    }


    //list with pagination and sorting------------------------------------------------------------------------------------------------------------------
    public ResponseEntity<Object> getFiles(Long userId, Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 15;
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("lastUpdatedOn")));
        Page<UserAttachment> userAttachments;

        if (userId == null) {
            userAttachments = attachmentRepo.findAll(pageable);
        } else {
            if (userId < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId should be positive");
            }

            userAttachments = attachmentRepo.findByUserId(userId, pageable);

            if (userAttachments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attachments found for the specified userId");
            }
        }

        long totalFiles = userAttachments.getTotalElements();
        int filesReceived = userAttachments.getNumberOfElements();
        int totalPages = userAttachments.getTotalPages();
        int remainingFiles = (int) Math.max(totalFiles - (page * size), 0); 

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalFiles", totalFiles);
        response.put("filesReceived", filesReceived);
        response.put("currentPage", page);
        response.put("totalPages", totalPages);
        response.put("remainingFiles", remainingFiles);
        response.put("files", userAttachments.getContent());

        return ResponseEntity.ok(response);
    }

    //downloading file---------------------------------------------------------------------------------------------------------------------------------
    public ResponseEntity<byte[]> downloadFileForUser(String filename, Long userId, Long attachmentId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You did not enter userId. Please enter userId to download the file.".getBytes());
        }
        if (userId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("UserId should not be a negative value".getBytes());
        }
        if (attachmentId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You did not enter attachmentId. Please enter attachmentId to download the file.".getBytes());
        }
        if (attachmentId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("AttachmentId should be a positive value".getBytes());
        }

        Optional<UserAttachment> userAttachmentOptional = attachmentRepo.findById(attachmentId);

        if (userAttachmentOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Attachment ID " + attachmentId + " does not exist").getBytes());
        }

        UserAttachment userAttachment = userAttachmentOptional.get();

        if (!userAttachment.getUser().getId().equals(userId) || !userAttachment.getFileName().equals(filename)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(("No matching file found for User ID " + userId + " with the filename: " + filename +
                            " and attachment ID: " + attachmentId).getBytes());
        }

        String s3FileName = attachmentId + "_" + filename;

        try (InputStream inputStream = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3FileName)
                        .build())) {

            byte[] fileContent = inputStream.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + s3FileName);
            headers.add("Content-Type", "application/octet-stream");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (S3Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Problem retrieving file from S3: " + e.awsErrorDetails().errorMessage()).getBytes());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Internal server error: " + e.getMessage()).getBytes());
        }
    }

}
