/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.exception.GlobalExceptionHandler
 *  cn.game.board.exception.SeatTakenException
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.ControllerAdvice
 *  org.springframework.web.bind.annotation.ExceptionHandler
 */
package cn.game.board.exception;

import cn.game.board.exception.SeatTakenException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value={SeatTakenException.class})
    public ResponseEntity<Map<String, String>> handleSeatTakenException(SeatTakenException ex) {
        Map errorResponse = Map.of((Object)"error", (Object)ex.getMessage());
        return new ResponseEntity((Object)errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value={IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map errorResponse = Map.of((Object)"error", (Object)ex.getMessage());
        return new ResponseEntity((Object)errorResponse, HttpStatus.BAD_REQUEST);
    }
}

