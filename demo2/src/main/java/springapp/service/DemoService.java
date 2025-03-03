package springapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.List;
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
    //uploading file--------------------------------------------------
    public String uploadFileAndCreateAttachment(MultipartFile file, Long userId) throws IOException {
        
        String fileType = file.getContentType();
        if (!fileType.equals("image/jpeg") && !fileType.equals("image/png") && !fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new IllegalArgumentException("you entered Invalid file type enter Only JPG, PNG, and DOCX files");
        }

        Optional<User> ById = userRepository.findById(userId);
        if (!ById.isPresent()) {
            throw new IllegalArgumentException("Invalid User ID.");
        }

        String fileName = file.getOriginalFilename();
        try (var inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder() 
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        }

        UserAttachment attachment = new UserAttachment();
        attachment.setFileName(fileName);
        attachment.setFileType(fileType);
        attachment.setLastUpdatedOn(LocalDateTime.now());
        attachment.setUser(ById.get());
        attachmentRepo.save(attachment);

        return "File uploaded to S3 bucket successfully. You can check in your bucket.";
    }
    //list with pagination and sorting----------------------------------------------------------------
    public List<UserAttachment> getFiles(int page, int size, String sortOrder) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("lastUpdatedOn").descending());
        return attachmentRepo.findAll(pageable).getContent();
    }
    //downloading file-------------------------------------------------------------------------------
    public byte[] downloadFileForUser(String filename, Long userId) throws IOException {
        
        Optional<UserAttachment> attachmentIdOptional = attachmentRepo.findByFileName(filename);
        if (!attachmentIdOptional.isPresent()) {
            return null;
        }
        UserAttachment userAttachmentId = attachmentIdOptional.get();
        if (!userAttachmentId.getUser().getId().equals(userId)) {
            return null; 
        }
        try (InputStream inputStream = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(userAttachmentId.getFileName())
                    .build())) {
                        return inputStream.readAllBytes();  
        } catch (S3Exception e) {
            throw new IOException("facing problem while retrieving file from S3: " + e.awsErrorDetails().errorMessage());
        }
    }

 

}
