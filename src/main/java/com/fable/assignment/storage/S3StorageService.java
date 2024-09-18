package com.fable.assignment.storage;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.amazonaws.services.s3.AmazonS3;

@Service
public class S3StorageService {


	@Value("${aws.s3.bucket}")
    private String bucketName;

    public void uploadFile(File file, AmazonS3 amazonS3) {
        amazonS3.putObject(bucketName, file.getName(), file);
    }
}
