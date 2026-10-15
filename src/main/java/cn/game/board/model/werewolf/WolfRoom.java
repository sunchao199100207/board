package cn.game.board.model.werewolf;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一局单身份狼人杀。法官不占号码，身份按座位各发一张。
 */
@Data
public class WolfRoom {
    private final String roomNumber;
    /** 坐在最上方法官位的人，空着表示还没有法官。 */
    private String hostPlayerId;
    private WolfRoleConfig config;
    private final Map<String, WolfPlayer> players = new ConcurrentHashMap<>();
    private boolean dealt;
    private int round = 1;
}
