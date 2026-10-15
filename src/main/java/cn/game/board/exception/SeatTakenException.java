package cn.game.board.exception;

/**
 * @author <a href="mailto:sunchao@gtmap.cn">sunchao</a
 * @version 1.0, 2025/10/27
 * @description 自定义异常
 */
public class SeatTakenException extends RuntimeException {
    public SeatTakenException(String message) {
        super(message);
    }
}
