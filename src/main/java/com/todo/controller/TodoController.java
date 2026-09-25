package com.todo.controller;

import com.todo.dto.request.TodoCompletionRequest;
import com.todo.dto.request.TodoCreateRequest;
import com.todo.dto.request.TodoUpdateRequest;
import com.todo.dto.response.PageResponse;
import com.todo.dto.response.TodoResponse;
import com.todo.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(
            @Valid @RequestBody TodoCreateRequest request
    ) {
        TodoResponse response = todoService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TodoResponse>> findAll(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                todoService.findAll(completed, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(todoService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        return ResponseEntity.ok(todoService.update(id, request));
    }

    @PatchMapping("/{id}/completed")
    public ResponseEntity<TodoResponse> updateCompleted(
            @PathVariable Long id,
            @Valid @RequestBody TodoCompletionRequest request
    ) {
        return ResponseEntity.ok(
                todoService.updateCompleted(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}