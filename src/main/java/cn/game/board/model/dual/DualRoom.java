package cn.game.board.model.dual;

import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class DualRoom {
    private final String roomNumber;
    private String hostPlayerId;
    private DualConfig config;
    private final Map<String, DualPlayer> players = new ConcurrentHashMap<>();
    private boolean dealt;
    private int round = 1;
    /** 法官标记的死亡牌，键为「座位:A」或「座位:B」。 */
    private final Set<String> deadCards = ConcurrentHashMap.newKeySet();
}
