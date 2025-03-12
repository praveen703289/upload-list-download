package springapp.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import springapp.model.UserAttachment;
import springapp.repository.AttachmentRepository;
import springapp.service.DemoService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping
public class MainController {

    private final DemoService s3Service;
    private final AttachmentRepository attachmentRepo;

    public MainController(DemoService s3Service,AttachmentRepository attachmentRepo) {
        this.s3Service = s3Service;
		this.attachmentRepo = attachmentRepo;
    }
    //uploading file----------------------------------------------------------------------------------------------------------------
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "userId",required = false) Long userId) {
        try {
            String message = s3Service.uploadFileAndCreateAttachment(file, userId);
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @RestControllerAdvice
	public class GlobalExceptionHandler {
 
	    @ExceptionHandler(MaxUploadSizeExceededException.class)
	    public ResponseEntity<String> handleFileSizeLimitExceeded(MaxUploadSizeExceededException ex) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size exceeds the 5 MB of limit");
	    }
	}
   //list of files------------------------------------------------------------------------------------------------------------------------
    @GetMapping("/list")
    public ResponseEntity<Object> listFiles(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return s3Service.getFiles(userId, page, size);
    }

    //downloading files-----------------------------------------------------------------------------------------------------------------------
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String filename,
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "attachmentId", required = true) Long attachmentId) {

        try {
            return s3Service.downloadFileForUser(filename, userId, attachmentId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Internal server error: " + e.getMessage()).getBytes());
        }
    }

}
