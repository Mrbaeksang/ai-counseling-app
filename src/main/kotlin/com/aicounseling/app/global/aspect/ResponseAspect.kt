package com.aicounseling.app.global.aspect

import com.aicounseling.app.global.rsData.RsData
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class ResponseAspect {
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    fun handleResponse(joinPoint: ProceedingJoinPoint): Any? {
        val result = joinPoint.proceed()

        return when (result) {
            is RsData<*> -> {
                val httpStatus = getHttpStatusFromCode(result.resultCode)
                setResponseStatus(httpStatus)
                ResponseEntity.status(httpStatus).body(result)
            }
            is ResponseEntity<*> -> result
            else -> result
        }
    }

    private fun getHttpStatusFromCode(code: String): HttpStatus {
        return when (code) {
            "200" -> HttpStatus.OK
            "201" -> HttpStatus.CREATED
            "204" -> HttpStatus.NO_CONTENT
            "400" -> HttpStatus.BAD_REQUEST
            "401" -> HttpStatus.UNAUTHORIZED
            "403" -> HttpStatus.FORBIDDEN
            "404" -> HttpStatus.NOT_FOUND
            "409" -> HttpStatus.CONFLICT
            "422" -> HttpStatus.UNPROCESSABLE_ENTITY
            "500" -> HttpStatus.INTERNAL_SERVER_ERROR
            "502" -> HttpStatus.BAD_GATEWAY
            "503" -> HttpStatus.SERVICE_UNAVAILABLE
            else -> {
                val numericCode = code.toIntOrNull() ?: 200
                HttpStatus.valueOf(numericCode)
            }
        }
    }

    private fun setResponseStatus(status: HttpStatus) {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        if (requestAttributes is ServletRequestAttributes) {
            val response = requestAttributes.response
            response?.status = status.value()
        }
    }
}
