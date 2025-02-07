package com.github.vladioeroonda.tasktracker.service.impl;

import com.github.vladioeroonda.tasktracker.dto.request.ProjectRequestDto;
import com.github.vladioeroonda.tasktracker.dto.response.ProjectResponseDto;
import com.github.vladioeroonda.tasktracker.exception.ProjectBadDataException;
import com.github.vladioeroonda.tasktracker.exception.ProjectNotFoundException;
import com.github.vladioeroonda.tasktracker.feign.PaymentClient;
import com.github.vladioeroonda.tasktracker.model.Project;
import com.github.vladioeroonda.tasktracker.model.ProjectStatus;
import com.github.vladioeroonda.tasktracker.model.User;
import com.github.vladioeroonda.tasktracker.repository.ProjectRepository;
import com.github.vladioeroonda.tasktracker.service.ProjectService;
import com.github.vladioeroonda.tasktracker.service.UserService;
import com.github.vladioeroonda.tasktracker.util.Translator;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final PaymentClient paymentClient;

    @Value("${payment-service.developer-account-id}")
    private String devAccountId;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            UserService userService,
            ModelMapper modelMapper,
            PaymentClient paymentClient
    ) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.paymentClient = paymentClient;
    }

    @Transactional
    @Override
    public List<ProjectResponseDto> getAllProjects() {
        logger.info("Получение списка всех Проектов");

        List<Project> projects = projectRepository.findAll();
        return projects.stream()
                .map(entity -> convertFromEntityToResponse(entity))
                .collect(Collectors.toList()
                );
    }

    @Transactional
    @Override
    public ProjectResponseDto getProjectByIdAndReturnResponseDto(Long id) {
        logger.info(String.format("Получение Проекта с id #%d", id));

        Project project = projectRepository
                .findById(id)
                .orElseThrow(() -> {
                    ProjectNotFoundException exception =
                            new ProjectNotFoundException(String.format(Translator.toLocale("exception.project.not-found-by-id"), id));
                    logger.error(exception.getMessage(), exception);
                    return exception;
                });

        return convertFromEntityToResponse(project);
    }

    @Transactional
    @Override
    public Project getProjectByIdAndReturnEntity(Long id) {
        return projectRepository
                .findById(id)
                .orElseThrow(() -> {
                    ProjectNotFoundException exception =
                            new ProjectNotFoundException(String.format(Translator.toLocale("exception.project.not-found-by-id"), id));
                    logger.error(exception.getMessage(), exception);
                    return exception;
                });
    }

    @Transactional
    @Override
    public void checkProjectExistsById(Long id) {
        if (projectRepository.findById(id).isEmpty()) {
            ProjectNotFoundException exception =
                    new ProjectNotFoundException(String.format(Translator.toLocale("exception.project.not-found-by-id"), id));
            logger.error(exception.getMessage(), exception);
            throw exception;
        }
    }

    @Transactional
    @Override
    public ProjectResponseDto addProject(ProjectRequestDto projectRequestDto) {
        logger.info("Добавление нового Проекта");

        if (Objects.isNull(projectRequestDto.getCustomer())) {
            ProjectBadDataException exception =
                    new ProjectBadDataException(Translator.toLocale("exception.project.bad-data.customer-is-empty"));
            logger.error(exception.getMessage(), exception);
            throw exception;
        }

        User customer = userService.getUserByIdAndReturnEntity(projectRequestDto.getCustomer().getId());

        if (Objects.isNull(customer.getBankAccountId())) {
            ProjectBadDataException exception =
                    new ProjectBadDataException(Translator.toLocale("exception.project.bad-data.not-found-customer-bank-account-id"));
            logger.error(exception.getMessage(), exception);
            throw exception;
        }

        Project projectForSave = convertFromRequestToEntity(projectRequestDto);
        projectForSave.setId(null);
        projectForSave.setStatus(ProjectStatus.IN_PROGRESS);
        projectForSave.setCustomer(customer);

        boolean isPaid = paymentClient.getPaymentCheckResult(
                projectForSave.getCustomer().getBankAccountId(),
                devAccountId,
                projectRequestDto.getPrice(),
                projectRequestDto.getName());

        if (!isPaid) {
            ProjectBadDataException exception =
                    new ProjectBadDataException(Translator.toLocale("exception.project.bad-data.not-found-payment-info"));
            logger.error(exception.getMessage(), exception);
            throw exception;
        }

        Project savedProject = projectRepository.save(projectForSave);
        return convertFromEntityToResponse(savedProject);
    }

    @Transactional
    @Override
    public ProjectResponseDto updateProject(ProjectRequestDto projectRequestDto) {
        logger.info(String.format("Обновление Проекта с id #%d", projectRequestDto.getId()));

        if (projectRequestDto.getStatus() == ProjectStatus.FINISHED) {
            ProjectBadDataException exception =
                    new ProjectBadDataException(Translator.toLocale("exception.project.bad-data.wrong-status"));
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

        Project projectForSave = convertFromRequestToEntity(projectRequestDto);
        projectForSave.setTasks(projectFromBD.getTasks());

        User customer = userService.getUserByIdAndReturnEntity(projectRequestDto.getCustomer().getId());
        projectForSave.setCustomer(customer);

        Project updatedProject = projectRepository.save(projectForSave);
        return convertFromEntityToResponse(updatedProject);
    }

    @Transactional
    @Override
    public void deleteProject(Long id) {
        logger.info(String.format("Удаление Проекта с id #%d", id));

        Project project = projectRepository
                .findById(id)
                .orElseThrow(() -> {
                    ProjectNotFoundException exception =
                            new ProjectNotFoundException(String.format(Translator.toLocale("exception.project.not-found-by-id"), id));
                    logger.error(exception.getMessage(), exception);
                    return exception;
                });
        projectRepository.delete(project);
    }

    private Project convertFromRequestToEntity(ProjectRequestDto requestDto) {
        return modelMapper.map(requestDto, Project.class);
    }

    private ProjectResponseDto convertFromEntityToResponse(Project project) {
        return modelMapper.map(project, ProjectResponseDto.class);
    }
}
