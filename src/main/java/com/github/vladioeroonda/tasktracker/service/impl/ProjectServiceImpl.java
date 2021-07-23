package com.github.vladioeroonda.tasktracker.service.impl;

import com.github.vladioeroonda.tasktracker.dto.request.ProjectRequestDto;
import com.github.vladioeroonda.tasktracker.dto.response.ProjectResponseDto;
import com.github.vladioeroonda.tasktracker.exception.ProjectNotFoundException;
import com.github.vladioeroonda.tasktracker.exception.UserNotFoundException;
import com.github.vladioeroonda.tasktracker.model.Project;
import com.github.vladioeroonda.tasktracker.model.ProjectStatus;
import com.github.vladioeroonda.tasktracker.model.User;
import com.github.vladioeroonda.tasktracker.repository.ProjectRepository;
import com.github.vladioeroonda.tasktracker.repository.UserRepository;
import com.github.vladioeroonda.tasktracker.service.ProjectService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            ModelMapper modelMapper,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public List<ProjectResponseDto> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream()
                .map(entity -> convertFromEntityToResponse(entity))
                .collect(Collectors.toList()
                );
    }

    @Transactional
    @Override
    public ProjectResponseDto getProjectById(Long id) {
        Project project = projectRepository
                .findById(id)
                .orElseThrow(
                        () -> new ProjectNotFoundException(String.format("Проект с id #%s не существует", id))
                );

        return convertFromEntityToResponse(project);
    }

    @Transactional
    @Override
    public ProjectResponseDto addProject(ProjectRequestDto projectRequestDto) {
        Project projectForSave = convertFromRequestToEntity(projectRequestDto);
        projectForSave.setId(null);
        projectForSave.setStatus(ProjectStatus.IN_PROGRESS);

        User customer = userRepository
                .findById(projectRequestDto.getCustomer().getId())
                .orElseThrow(
                        () -> new UserNotFoundException(
                                String.format("Пользователь с id #%s не существует", projectRequestDto.getCustomer().getId())
                        )
                );

        projectForSave.setCustomer(customer);

        Project savedProject = projectRepository.save(projectForSave);
        return convertFromEntityToResponse(savedProject);
    }

    @Transactional
    @Override
    public ProjectResponseDto updateProject(ProjectRequestDto projectRequestDto) {

        Project projectFromBD = projectRepository
                .findById(projectRequestDto.getId())
                .orElseThrow(
                        () -> new ProjectNotFoundException(
                                String.format("Проект с id #%s не существует", projectRequestDto.getId()))
                );

        Project projectForSave = convertFromRequestToEntity(projectRequestDto);
        projectForSave.setTasks(projectFromBD.getTasks());

        User customer = userRepository
                .findById(projectRequestDto.getCustomer().getId())
                .orElseThrow(
                        () -> new UserNotFoundException(
                                String.format("Пользователь с id #%s не существует", projectRequestDto.getCustomer().getId())
                        )
                );

        projectForSave.setCustomer(customer);

        Project updatedProject = projectRepository.save(projectForSave);
        return convertFromEntityToResponse(updatedProject);
    }

    @Transactional
    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository
                .findById(id)
                .orElseThrow(
                        () -> new ProjectNotFoundException(
                                String.format("Проект с id #%s не существует. Удаление невозможно", id)
                        )
                );
        projectRepository.delete(project);
    }

    private Project convertFromRequestToEntity(ProjectRequestDto requestDto) {
        return modelMapper.map(requestDto, Project.class);
    }

    private ProjectResponseDto convertFromEntityToResponse(Project project) {
        return modelMapper.map(project, ProjectResponseDto.class);
    }
}
