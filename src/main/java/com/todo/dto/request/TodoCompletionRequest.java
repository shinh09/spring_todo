package com.todo.dto.request;

import jakarta.validation.constraints.NotNull;

public record TodoCompletionRequest(

        @NotNull(message = "완료 여부는 필수입니다.")
        Boolean completed
) {
}