package cn.game.board.model.werewolf;

import lombok.Builder;
import lombok.Data;

/**
 * 法官口述用的一段词，不记录玩家选择。
 */
@Data
@Builder
public class WolfScriptStep {
    private String title;
    private String text;
}
