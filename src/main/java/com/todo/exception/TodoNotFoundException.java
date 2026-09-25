package com.todo.exception;

public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(Long id) {
        super("ID가 " + id + "인 할 일을 찾을 수 없습니다.");
    }
}