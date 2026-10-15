package cn.game.board.model.dual;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 双身份狼人杀的单张身份牌。阵营看两张牌里有没有狼，不看这张牌单独站在哪。
 */
public enum DualRole {
    SEER("seer", "预言家", "🔮", "good", "只能查验对方的上身份。", "img/lrs/预言家.jpg"),
    WITCH("witch", "女巫", "🧙‍♀️", "good", "只能用上身份的技能，解药毒药各一瓶。", "img/lrs/女巫.jpg"),
    HUNTER("hunter", "猎人", "🏹", "good", "只能用上身份的技能。被女巫毒死不能开枪。", "img/lrs/猎人.jpg"),
    GUARD("guard", "守卫", "🛡️", "good", "只能用上身份的技能。不能连续两晚守同一人。", "img/lrs/守卫.jpg"),
    SILENCE_ELDER("silenceElder", "禁言长老", "🤫", "good", "只能用上身份的技能，指出明天白天不能发言的玩家。", "img/lrs/禁言长老.jpg"),
    KNIGHT("knight", "骑士", "🏇", "good", "只能用上身份的技能。白天可以决斗，猜中狼人则对方出局，猜错则自己出局。", "img/lrs/骑士.jpg"),
    WOLF_KING("wolfKing", "狼王", "👑", "wolf", "狼人牌。上身份才能和狼队友一起刀人，出局时可以开枪，被女巫毒死不能开枪。", "img/lrs/黑狼王.jpg"),
    WEREWOLF("werewolf", "狼人", "🐺", "wolf", "夜里狼牌在上面才能睁眼刀人。", "img/lrs/狼人.jpg"),
    VILLAGER("villager", "平民", "🧑‍🌾", "good", "没有夜间技能。两张都是平民就是金宝宝。", "img/lrs/平民.jpg");

    private static final Map<String, DualRole> BY_KEY = Stream.of(values())
            .collect(Collectors.toMap(DualRole::getKey, Function.identity()));

    private final String key;
    private final String name;
    private final String emoji;
    private final String camp;
    private final String hint;
    private final String imageUrl;

    DualRole(String key, String name, String emoji, String camp, String hint, String imageUrl) {
        this.key = key;
        this.name = name;
        this.emoji = emoji;
        this.camp = camp;
        this.hint = hint;
        this.imageUrl = imageUrl;
    }

    public static DualRole fromKey(String key) {
        DualRole role = BY_KEY.get(key);
        if (role == null) {
            throw new IllegalArgumentException("未知身份: " + key);
        }
        return role;
    }

    public boolean wolf() {
        return "wolf".equals(camp);
    }

    public boolean god() {
        return this != VILLAGER && !wolf();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getCamp() {
        return camp;
    }

    public String getHint() {
        return hint;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
