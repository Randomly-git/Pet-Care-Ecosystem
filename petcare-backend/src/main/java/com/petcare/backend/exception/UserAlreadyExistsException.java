// UserAlreadyExistsException.java
package com.petcare.backend.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String username) {
        super("用户名 '" + username + "' 已存在");
    }
}