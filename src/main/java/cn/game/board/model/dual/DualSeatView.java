package cn.game.board.model.dual;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DualSeatView {
    private int seat;
    private boolean occupied;
    private boolean ready;
    private boolean self;
    private String cardAName;
    private String cardBName;
    private String cardAEmoji;
    private String cardBEmoji;
    private Integer upperIndex;
    private boolean confirmed;
    private String campName;
}
