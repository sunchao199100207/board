package cn.game.board.model.dual;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DualPush {
    private String type;
    private String error;
    private DualRoomView state;
}
