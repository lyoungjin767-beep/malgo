package com.malgo.backend.exception;


// 요청한 번역 기록을 찾을 수 없을 때 사용하는 예외
public class TranslationNotFoundException extends RuntimeException {

    public TranslationNotFoundException(Long id) {
        super("번역 기록을 찾을 수 없습니다. id=" + id);
    }
}