package com.example.dify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class WorkflowService {

    @Value("${dify.api.key}")
    private String apiKey;

    @Value("${dify.api.base-url}")
    private String baseUrl;

    public Mono<DegreeDTO> executeOcrWorkflow(MultipartFile file) {
        return executeWorkflow(file)
                .doOnNext(jsonResponse -> System.out.println("Response: " + jsonResponse))
                .flatMap(this::parseAndMapToDegreeDTO);
    }

    private Mono<String> executeWorkflow(MultipartFile file) {
        return uploadFile(file)
                .flatMap(fileId -> {
                    Map<String, Object> requestBody = new HashMap<>();
                    Map<String, Object> inputs = new HashMap<>();
                    Map<String, Object> fileInput = new HashMap<>();
                    fileInput.put("type", "image");
                    fileInput.put("transfer_method", "local_file");
                    fileInput.put("upload_file_id", fileId);
                    inputs.put("files", fileInput);
                    requestBody.put("inputs", inputs);
                    requestBody.put("response_mode", "blocking");
                    requestBody.put("user", "abc-123");
                    try {
                        return WebClient.create()
                                .post()
                                .uri(baseUrl + "/workflows/run")
                                .header("Authorization", "Bearer " + apiKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToMono(String.class);
                    } catch (Exception e) {
                        log.error("Error executing workflow: " + e.getMessage());
                        return Mono.error(e);
                    }
                });
    }

    private Mono<String> uploadFile(MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource());

        try {
            return WebClient.create()
                    .post()
                    .uri(baseUrl + "/files/upload")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> (String) response.get("id"));
        } catch (Exception e) {
            log.error("Error uploading file: " + e.getMessage());
            return Mono.error(e);
        }
    }

    private Mono<DegreeDTO> parseAndMapToDegreeDTO(String jsonResponse) {
        return Mono.fromCallable(() -> {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode outputs = root.get("data").get("outputs");
            String text = outputs.get("text").asText();

            // Extract information using regex patterns
            Pattern namePattern = Pattern.compile("-\\s*\\*\\*姓名\\*\\*\\s*：\\s*([\\u4e00-\\u9fa5]+)");
            Pattern birthdayPattern = Pattern.compile("-\\s*\\*\\*出生日期\\*\\*\\s*：\\s*(\\d{4}年\\d{1,2}月\\d{1,2}日)");
            Pattern universityPattern = Pattern.compile("-\\s*\\*\\*毕业院校\\*\\*\\s*：\\s*([\\u4e00-\\u9fa5]+大学)");
            Pattern degreeNoPattern = Pattern.compile("-\\s*\\*\\*证书编号\\*\\*\\s*：\\s*(\\d+)");

            String name = extractValue(text, namePattern);
            String birthday = extractValue(text, birthdayPattern);
            String university = extractValue(text, universityPattern);
            String degreeNo = extractValue(text, degreeNoPattern);

            return DegreeDTO.builder()
                    .name(name)
                    .birthday(birthday)
                    .university(university)
                    .degreeNo(degreeNo)
                    .build();
        });
    }

    private String extractValue(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
