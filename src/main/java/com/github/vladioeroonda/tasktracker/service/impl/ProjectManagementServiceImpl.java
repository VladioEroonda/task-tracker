package com.github.vladioeroonda.tasktracker.service.impl;

import com.github.vladioeroonda.tasktracker.dto.request.ProjectClosingRequestDto;
import com.github.vladioeroonda.tasktracker.dto.response.ProjectResponseDto;
import com.github.vladioeroonda.tasktracker.exception.ProjectClosingException;
import com.github.vladioeroonda.tasktracker.exception.ProjectNotFoundException;
import com.github.vladioeroonda.tasktracker.model.Project;
import com.github.vladioeroonda.tasktracker.model.ProjectStatus;
import com.github.vladioeroonda.tasktracker.model.Release;
import com.github.vladioeroonda.tasktracker.repository.ProjectRepository;
import com.github.vladioeroonda.tasktracker.service.ProjectManagementService;
import com.github.vladioeroonda.tasktracker.service.ReleaseService;
import com.github.vladioeroonda.tasktracker.service.TaskService;
import com.github.vladioeroonda.tasktracker.util.Translator;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectManagementServiceImpl implements ProjectManagementService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectManagementServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final TaskService taskService;
    private final ReleaseService releaseService;
    private final ModelMapper modelMapper;

    public ProjectManagementServiceImpl(
            ProjectRepository projectRepository,
            TaskService taskService,
            ReleaseService releaseService,
            ModelMapper modelMapper
    ) {
        this.projectRepository = projectRepository;
        this.taskService = taskService;
        this.releaseService = releaseService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    @Override
    public ProjectResponseDto closeProject(ProjectClosingRequestDto projectRequestDto) {
        logger.info("Закрытие проекта");

        if (projectRequestDto.getProjectStatus() != ProjectStatus.FINISHED) {
            ProjectClosingException exception =
                    new ProjectClosingException(Translator.toLocale("exception.project-management.wrong-status"));
            logger.error(exception.getMessage(), exception);
            throw exception;
        }

        Project projectFromBD = projectRepository
                .findById(projectRequestDto.getId())
                .orElseThrow(() -> {
                    ProjectNotFoundException exception =
                            new ProjectNotFoundException(
                                    String.format(Translator.toLocale("exception.project.not-found-by-id"), projectRequestDto.getId())
                            );
                    logger.error(exception.getMessage(), exception);
                    return exception;
                });

        if (projectFromBD.getStatus() == ProjectStatus.FINISHED) {
            ProjectClosingException exception = new ProjectClosingException(Translator.toLocale("exception.project-management.project-already-closed"));
            logger.error(exception.getMessage(), exception);
            throw exception;
        }

        List<Release> notClosedReleases =
                releaseService.getAllNotClosedReleasesByProjectId(projectRequestDto.getId()) == null ?
                        new ArrayList<>() : releaseService.getAllNotClosedReleasesByProjectId(projectRequestDto.getId());

        if (notClosedReleases.size() > 0) {
            ProjectClosingException exception =
                    new ProjectClosingException(
                            String.format(Translator.toLocale("exception.project-management.not-closed-releases"), notClosedReleases.size())
                    );
            logger.error(exception.getMessage(), exception);
            throw exception;
        }

        projectFromBD.setStatus(ProjectStatus.FINISHED);

        return convertFromEntityToResponse(projectFromBD);
    }

    private ProjectResponseDto convertFromEntityToResponse(Project project) {
        return modelMapper.map(project, ProjectResponseDto.class);
    }
}
