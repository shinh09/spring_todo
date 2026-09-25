package com.todo.service;

import com.todo.dto.request.TodoCompletionRequest;
import com.todo.dto.request.TodoCreateRequest;
import com.todo.dto.request.TodoUpdateRequest;
import com.todo.dto.response.PageResponse;
import com.todo.dto.response.TodoResponse;
import com.todo.entity.Todo;
import com.todo.exception.TodoNotFoundException;
import com.todo.repository.TodoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional
    public TodoResponse create(TodoCreateRequest request) {
        Todo todo = new Todo(
                request.title().trim(),
                normalizeDescription(request.description())
        );

        return TodoResponse.from(todoRepository.save(todo));
    }

    public PageResponse<TodoResponse> findAll(
            Boolean completed,
            Pageable pageable
    ) {
        Page<Todo> todoPage = completed == null
                ? todoRepository.findAll(pageable)
                : todoRepository.findAllByCompleted(completed, pageable);

        Page<TodoResponse> responsePage = todoPage.map(TodoResponse::from);

        return PageResponse.from(responsePage);
    }

    public TodoResponse findById(Long id) {
        return TodoResponse.from(getTodo(id));
    }

    @Transactional
    public TodoResponse update(Long id, TodoUpdateRequest request) {
        Todo todo = getTodo(id);

        todo.update(
                request.title().trim(),
                normalizeDescription(request.description())
        );

        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse updateCompleted(
            Long id,
            TodoCompletionRequest request
    ) {
        Todo todo = getTodo(id);
        todo.changeCompleted(request.completed());

        return TodoResponse.from(todo);
    }

    @Transactional
    public void delete(Long id) {
        Todo todo = getTodo(id);
        todoRepository.delete(todo);
    }

    private Todo getTodo(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}