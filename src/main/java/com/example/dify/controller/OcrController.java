package com.example.dify.controller;

import com.example.dify.service.DegreeDTO;
import com.example.dify.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dify")
public class OcrController {
    private final WorkflowService workflowService;


    @PostMapping("/workflow/ocr")
    public Mono<DegreeDTO> runWorkflow(@RequestPart("file") MultipartFile file) {
        return workflowService.executeOcrWorkflow(file);
    }
}
