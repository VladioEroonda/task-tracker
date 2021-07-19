package com.github.vladioeroonda.tasktracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Сущность Пользователя(ответ)")
public class UserResponseDto {
    @Schema(description = "ID Пользователя")
    private Long id;
    @Schema(description = "Логин Пользователя")
    private String login;
    @Schema(description = "ФИО Пользователя")
    private String name;

    public UserResponseDto() {
    }

    public UserResponseDto(
            Long id,
            String login,
            String name
    ) {
        this.id = id;
        this.login = login;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
