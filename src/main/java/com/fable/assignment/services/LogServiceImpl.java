package com.fable.assignment.services;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fable.assignment.model.LogData;
import com.fable.assignment.storage.S3StorageService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Service
@Slf4j
public class LogServiceImpl implements LogService {

	private static final double MAX_FILE_SIZE_MB = 10.0;
	private static final long FLUSH_INTERVAL_MS = 60000;
	private static final String LOG_FILE_PATH = "maheshlogs/maheshlogfile.json";
	private final Object fileLock = new Object();
	private final AmazonS3 amazonS3;

	@Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.access-key-id}")
    private String awsAccessKeyId;

    @Value("${aws.secret-access-key}")
    private String awsSecretAccessKey;

	@Autowired
	private S3StorageService s3StorageService;

	public LogServiceImpl() {
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
        this.amazonS3 = AmazonS3ClientBuilder.standard()
                           .withRegion(awsRegion)
                           .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                           .build();
	}

	@Override
	public void bufferLog(LogData log) {
		synchronized (fileLock) {
			File logFile = new File(LOG_FILE_PATH);
            File logDir = logFile.getParentFile();
            if (logDir != null && !logDir.exists()) {
                logDir.mkdirs();
            }

			try {
				if (logFile.exists()) {
					double fileSizeInMB = logFile.length() / (1024.0 * 1024.0);
					System.out.println(fileSizeInMB+" "+MAX_FILE_SIZE_MB);
					if(fileSizeInMB >= MAX_FILE_SIZE_MB) {
	                    flushLogs();
	                }
	            }

				try (FileWriter writer = new FileWriter(logFile, true)) {
                    String logWithTimestamp = String.format("{ \"unix_timestamp\": %d, \"data\": \"%s\" }",
                            Instant.now().getEpochSecond(), log.getEventName());
                    writer.write(logWithTimestamp + System.lineSeparator());
                }

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Scheduled(fixedRate = FLUSH_INTERVAL_MS)
	private void flushLogs() throws IOException {
		log.info("Flushing logs...");
		synchronized (fileLock) {
			File logFile = new File(LOG_FILE_PATH);
			if (logFile.exists()) {
				double fileSizeInMB = logFile.length() / (1024.0 * 1024.0);
				log.info("Current log file size: " + String.valueOf(fileSizeInMB) + " MB");
				Path path = Paths.get(LOG_FILE_PATH);
				long lineCount = Files.lines(path).count();
				log.info("Number of logs before flush: {}", lineCount);
				try {
					s3StorageService.uploadFile(logFile, amazonS3);
				} catch (Exception e) {
					log.info("Exception while pushing logs file content to AWS", e);
				}
				try (FileWriter writer = new FileWriter(logFile, false)) {
					writer.write("");
					log.info("Log file deleted successfully.");
				} catch (IOException e) {
					log.info("Exception while clearing log file content", e);
				}
			}
		}
	}
}
