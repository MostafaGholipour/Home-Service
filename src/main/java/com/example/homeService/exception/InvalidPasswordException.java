package com.example.homeService.exception;

public class InvalidPasswordException extends RuntimeException{
    public InvalidPasswordException(String s) {
        super(s);
    }
}
