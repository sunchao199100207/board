package cn.game.board.model.werewolf;

import lombok.Data;

/**
 * 一局的牌组配置。人数由各身份相加得到，不单独填写。
 */
@Data
public class WolfRoleConfig {
    private int civilian;
    private int werewolf;
    private boolean wolfKing;
    private boolean whiteWolfKing;
    private boolean seer;
    private boolean witch;
    private boolean hunter;
    private boolean guard;
    private boolean thief;
    private boolean knight;
    private boolean silenceElder;
}
