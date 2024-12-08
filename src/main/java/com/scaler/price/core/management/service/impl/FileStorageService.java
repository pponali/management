package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.management.config.FileStorageProperties;
import com.scaler.price.core.management.exceptions.FileStorageException;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class FileStorageService {

    private  Path fileStorageLocation;
    private  Path errorReportLocation;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        try {
            this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                    .toAbsolutePath().normalize();
            this.errorReportLocation = Paths.get(fileStorageProperties.getErrorDir())
                    .toAbsolutePath().normalize();

            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.errorReportLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, String uploadId) {
        try {
            // Normalize file name
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());

            if (fileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + fileName);
            }

            // Create date-based directory structure
            LocalDate today = LocalDate.now();
            Path dateBasedDir = fileStorageLocation.resolve(
                    String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth())
            );
            Files.createDirectories(dateBasedDir);

            // Generate unique filename with upload ID
            String uniqueFileName = uploadId + "_" + fileName;
            Path targetLocation = dateBasedDir.resolve(uniqueFileName);

            // Copy file to target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file. Please try again!", ex);
        }
    }

    public String saveErrorReport(Workbook workbook, String uploadId) {
        if (workbook == null) {
            throw new FileStorageException("Workbook cannot be null");
        }
        
        try {
            // Create date-based directory structure for error reports
            LocalDate today = LocalDate.now();
            Path dateBasedDir = errorReportLocation.resolve(
                    String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth())
            );
            
            // Ensure directory exists
            try {
                Files.createDirectories(dateBasedDir);
            } catch (IOException e) {
                log.error("Failed to create directory structure: {}", dateBasedDir);
                throw new FileStorageException("Could not create directory for error report", e);
            }

            // Generate error report filename with timestamp to avoid conflicts
            String errorFileName = String.format("%s_errors_%s.xlsx", 
                uploadId, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            Path errorFilePath = dateBasedDir.resolve(errorFileName);

            // Write workbook to file
            try (FileOutputStream fos = new FileOutputStream(errorFilePath.toFile())) {
                workbook.write(fos);
                log.info("Successfully saved error report: {}", errorFilePath);
                return errorFilePath.toString();
            } catch (IOException e) {
                log.error("Failed to write error report to file: {}", errorFilePath);
                throw new FileStorageException("Could not write error report to file", e);
            }
        } catch (Exception ex) {
            log.error("Unexpected error while saving error report for upload {}: {}", uploadId, ex.getMessage());
            throw new FileStorageException("Could not store error report. Please try again!", ex);
        }
    }

    public Path getFilePath(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return path;
            }
            throw new FileStorageException("File not found: " + filePath);
        } catch (Exception ex) {
            throw new FileStorageException("Could not retrieve file: " + filePath, ex);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.error("Error deleting file: " + filePath, ex);
            // Don't throw exception as this is cleanup operation
        }
    }

    public void cleanup(int retentionDays) {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);

            // Cleanup upload files
            cleanupDirectory(fileStorageLocation, cutoffDate);

            // Cleanup error reports
            cleanupDirectory(errorReportLocation, cutoffDate);

        } catch (IOException ex) {
            log.error("Error during file cleanup", ex);
        }
    }

    private void cleanupDirectory(Path directory, LocalDate cutoffDate) throws IOException {
        Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(path -> isFileOlderThan(path, cutoffDate))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ex) {
                        log.error("Error deleting file: " + path, ex);
                    }
                });
    }

    private boolean isFileOlderThan(Path file, LocalDate cutoffDate) {
        try {
            LocalDate fileDate = LocalDate.ofInstant(
                    Files.getLastModifiedTime(file).toInstant(),
                    java.time.ZoneId.systemDefault()
            );
            return fileDate.isBefore(cutoffDate);
        } catch (IOException ex) {
            log.error("Error checking file date: " + file, ex);
            return false;
        }
    }

    public void saveWorkbook(Workbook workbook, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store workbook. Please try again!", ex);
        }
    }
}
