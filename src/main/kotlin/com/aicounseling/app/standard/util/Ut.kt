package com.aicounseling.app.standard.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 유틸리티 클래스
 * 자주 사용하는 기능들을 모아둔 헬퍼
 */
@Component
class Ut {
    
    companion object {
        @Autowired
        private lateinit var objectMapper: ObjectMapper
        
        /**
         * JSON 관련 유틸
         */
        object json {
            fun toStr(obj: Any): String = objectMapper.writeValueAsString(obj)
            fun <T> toObj(json: String, clazz: Class<T>): T = objectMapper.readValue(json, clazz)
        }
        
        /**
         * 날짜 관련 유틸
         */
        object date {
            fun format(dateTime: LocalDateTime, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
                return dateTime.format(DateTimeFormatter.ofPattern(pattern))
            }
            
            fun parse(dateStr: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): LocalDateTime {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern))
            }
        }
        
        /**
         * 문자열 관련 유틸
         */
        object str {
            fun isEmpty(str: String?): Boolean = str.isNullOrBlank()
            fun hasLength(str: String?): Boolean = !isEmpty(str)
            
            fun truncate(str: String, maxLength: Int, suffix: String = "..."): String {
                return if (str.length <= maxLength) str
                else str.substring(0, maxLength - suffix.length) + suffix
            }
        }
    }
}