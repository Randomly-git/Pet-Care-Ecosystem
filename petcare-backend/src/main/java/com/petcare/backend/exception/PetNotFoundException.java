package com.petcare.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PetNotFoundException extends RuntimeException {
    public PetNotFoundException(Long petId) {
        super("未找到ID为 " + petId + " 的宠物");
    }

    public PetNotFoundException(String message) {
        super(message);
    }
}