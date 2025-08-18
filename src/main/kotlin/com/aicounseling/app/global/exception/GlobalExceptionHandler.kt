package com.aicounseling.app.global.exception

import com.aicounseling.app.global.rsData.RsData
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<RsData<Map<String, String>>> {
        val errors =
            e.bindingResult.allErrors
                .filterIsInstance<FieldError>()
                .associate { it.field to (it.defaultMessage ?: "Invalid value") }

        return ResponseEntity
            .badRequest()
            .body(RsData("400", "입력값이 올바르지 않습니다", errors))
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<RsData<Nothing>> {
        return ResponseEntity
            .status(e.status)
            .body(RsData(e.status.value().toString(), e.message))
    }
    
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFoundException(e: NoSuchElementException): ResponseEntity<RsData<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RsData("404", e.message ?: "리소스를 찾을 수 없습니다"))
    }
    
    @ExceptionHandler(IllegalStateException::class)
    fun handleConflictException(e: IllegalStateException): ResponseEntity<RsData<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(RsData("409", e.message ?: "요청이 충돌합니다"))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<RsData<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RsData("500", "서버 오류가 발생했습니다"))
    }
}

open class BusinessException(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)

class UnauthorizedException(message: String = "인증이 필요합니다") :
    BusinessException(HttpStatus.UNAUTHORIZED, message)

class ForbiddenException(message: String = "권한이 없습니다") :
    BusinessException(HttpStatus.FORBIDDEN, message)

class NotFoundException(message: String = "리소스를 찾을 수 없습니다") :
    BusinessException(HttpStatus.NOT_FOUND, message)

class BadRequestException(message: String = "잘못된 요청입니다") :
    BusinessException(HttpStatus.BAD_REQUEST, message)
