package springapp.config;


import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoConfiguration {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of("ap-south-1"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                "AKIAZVCNVINWPVMMM66C", 
                                "oH8WYXs4yFY2CFYdqb3GV0JM7mzHvsH5M6nKEo2r"))
                )
                .build();
    }
}

