package com.github.vladioeroonda.tasktracker.controller;

import com.github.vladioeroonda.tasktracker.dto.request.ReleaseRequestDto;
import com.github.vladioeroonda.tasktracker.dto.response.ReleaseResponseDto;
import com.github.vladioeroonda.tasktracker.service.ReleaseService;
import com.github.vladioeroonda.tasktracker.service.impl.ProjectManagementServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Релиз", description = "Отвечает за CRUD операции, связанные с Релизом")
@RestController
@RequestMapping("/api/tracker/release")
public class ReleaseController {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseController.class);

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @Operation(summary = "Получение списка всех Релизов")
    @GetMapping
    public ResponseEntity<List<ReleaseResponseDto>> getAllReleases() {
        logger.info("GET /api/tracker/release");
        List<ReleaseResponseDto> releases = releaseService.getAllReleases();
        return new ResponseEntity<>(releases, HttpStatus.OK);
    }

    @Operation(summary = "Получение конкретного Релиза по его id")
    @GetMapping(value = "/{id}")
    public ResponseEntity<ReleaseResponseDto> getReleaseById(@PathVariable Long id) {
        logger.info("GET /api/tracker/release/{id}");
        ReleaseResponseDto release = releaseService.getReleaseByIdAndReturnResponseDto(id);
        return new ResponseEntity<>(release, HttpStatus.OK);
    }

    @Operation(summary = "Добавление нового Релиза")
    @PostMapping
    public ResponseEntity<ReleaseResponseDto> addNewRelease(@RequestBody ReleaseRequestDto requestDto) {
        logger.info("POST /api/tracker/release");
        ReleaseResponseDto release = releaseService.addRelease(requestDto);
        return new ResponseEntity<>(release, HttpStatus.CREATED);
    }

    @Operation(summary = "Изменение Релиза")
    @PutMapping
    public ResponseEntity<ReleaseResponseDto> updateRelease(@RequestBody ReleaseRequestDto requestDto) {
        logger.info("PUT /api/tracker/release");
        ReleaseResponseDto release = releaseService.updateRelease(requestDto);
        return new ResponseEntity<>(release, HttpStatus.OK);
    }

    @Operation(summary = "Удаление конкретного Релиза по его id")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<String> deleteReleaseById(@PathVariable Long id) {
        logger.info("DELETE /api/tracker/release/{id}");
        releaseService.deleteRelease(id);
        return new ResponseEntity<>(
                String.format("Релиз с id #%d был успешно удалён", id),
                HttpStatus.OK
        );
    }
}
