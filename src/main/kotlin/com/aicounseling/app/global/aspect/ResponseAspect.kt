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
    companion object {
        private const val DEFAULT_STATUS_CODE = 200

        private val STATUS_CODE_MAP =
            mapOf(
                "200" to HttpStatus.OK,
                "201" to HttpStatus.CREATED,
                "204" to HttpStatus.NO_CONTENT,
                "400" to HttpStatus.BAD_REQUEST,
                "401" to HttpStatus.UNAUTHORIZED,
                "403" to HttpStatus.FORBIDDEN,
                "404" to HttpStatus.NOT_FOUND,
                "409" to HttpStatus.CONFLICT,
                "422" to HttpStatus.UNPROCESSABLE_ENTITY,
                "500" to HttpStatus.INTERNAL_SERVER_ERROR,
                "502" to HttpStatus.BAD_GATEWAY,
                "503" to HttpStatus.SERVICE_UNAVAILABLE,
            )
    }

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
        return STATUS_CODE_MAP[code] ?: run {
            val numericCode = code.toIntOrNull() ?: DEFAULT_STATUS_CODE
            HttpStatus.valueOf(numericCode)
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
