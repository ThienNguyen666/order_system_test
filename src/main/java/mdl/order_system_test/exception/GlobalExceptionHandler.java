package mdl.order_system_test.exception;

import mdl.order_system_test.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
/**
     * 1. Xử lý lỗi Validation (Khi @Valid trong Controller bị tịt)
     * Trả về HTTP 400 Bad Request
     */
      @ExceptionHandler(MethodArgumentNotValidException.class)
      @ResponseStatus(HttpStatus.BAD_REQUEST)
      public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
            Map<String, String> errors = new HashMap<>();
            
            // Lọc ra các field bị lỗi và message tương ứng
            ex.getBindingResult().getAllErrors().forEach((error) -> {
                  String fieldName = ((FieldError) error).getField();
                  String errorMessage = error.getDefaultMessage();
                  errors.put(fieldName, errorMessage);
            });

            log.warn("Validation failed: {}", errors);

            return ErrorResponse.builder()
                  .timestamp(LocalDateTime.now())
                  .status(HttpStatus.BAD_REQUEST.value())
                  .error("Bad Request")
                  .message("Dữ liệu đầu vào không hợp lệ")
                  .fieldErrors(errors) // Đẩy map lỗi ra cho Frontend đọc
                  .build();
      }

      /**
       * 2. Xử lý lỗi tìm không thấy dữ liệu (Custom Exception anh em mình vừa tạo)
       * Trả về HTTP 404 Not Found
       */
      @ExceptionHandler(OrderNotFoundException.class)
      @ResponseStatus(HttpStatus.NOT_FOUND)
      public ErrorResponse handleOrderNotFoundException(OrderNotFoundException ex) {
            log.error("Order not found: {}", ex.getMessage());
            return ErrorResponse.builder()
                  .timestamp(LocalDateTime.now())
                  .status(HttpStatus.NOT_FOUND.value())
                  .error("Not Found")
                  .message(ex.getMessage())
                  .build();
      }

      /**
       * 3. Chốt chặn cuối cùng: Xử lý tất cả các Exception chưa được dự liệu trước
       * Trả về HTTP 500 Internal Server Error
       */
      @ExceptionHandler(Exception.class)
      @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
      public ErrorResponse handleGlobalException(Exception ex) {
            // Lỗi 500 là lỗi nghiêm trọng, phải log stacktrace ra để dev fix
            log.error("Hệ thống xảy ra lỗi không mong muốn: ", ex); 
            
            return ErrorResponse.builder()
                  .timestamp(LocalDateTime.now())
                  .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                  .error("Internal Server Error")
                  .message("Hệ thống đang bảo trì hoặc gặp sự cố, vui lòng thử lại sau!") 
                  // Tuyệt đối không ném chi tiết lỗi SQL hay code logic ra cho user xem
                  .build();
      }
}
