package cn.game.board.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * 全局异常处理
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatTakenException.class)
    public ResponseEntity<Map<String, String>> handleSeatTakenException(SeatTakenException ex) {
        Map<String, String> errorResponse = Map.of("error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> errorResponse = Map.of("error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
