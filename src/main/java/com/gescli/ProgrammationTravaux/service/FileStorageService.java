package com.gescli.ProgrammationTravaux.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public byte[] load(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    public String store(MultipartFile file, String subDir) throws IOException {
        Path dir = Paths.get(uploadDir, subDir);
        Files.createDirectories(dir);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = UUID.randomUUID() + "_" + safeFilename;
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }
}
