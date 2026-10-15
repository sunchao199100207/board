package cn.game.board.model.werewolf;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 狼人杀身份。只描述牌面，不包含夜间行动结算。
 */
public enum WolfRole {
    CIVILIAN("civilian", "平民", "🧑‍🌾", "good", "好人阵营",
            "你没有夜间技能。晚上闭眼，白天听法官安排发言和投票。"),
    WEREWOLF("werewolf", "狼人", "🐺", "wolf", "狼人阵营",
            "夜里和狼队友一起睁眼。听法官喊「狼人请睁眼」。"),
    WOLF_KING("wolfKing", "黑狼王", "👑", "wolf", "狼人阵营",
            "你和狼人一起睁眼。被投票放逐或被狼刀死可以开枪，被女巫毒死不能开枪。"),
    WHITE_WOLF_KING("whiteWolfKing", "白狼王", "🌕", "wolf", "狼人阵营",
            "你和狼人一起睁眼。白天可以自爆，带走一名玩家。"),
    SEER("seer", "预言家", "🔮", "good", "好人阵营",
            "听法官喊「预言家请睁眼」，用手势指出要查验的人。"),
    WITCH("witch", "女巫", "🧙‍♀️", "good", "好人阵营",
            "听法官喊「女巫请睁眼」。解药和毒药各一瓶，同一晚不能都用。"),
    HUNTER("hunter", "猎人", "🏹", "good", "好人阵营",
            "听法官喊「猎人请睁眼」。被女巫毒死不能开枪。"),
    GUARD("guard", "守卫", "🛡️", "good", "好人阵营",
            "听法官喊「守卫请睁眼」。不能连续两晚守护同一个人。"),
    THIEF("thief", "盗贼", "🦊", "good", "好人阵营",
            "听法官喊「盗贼请睁眼」。你可以和一名玩家交换身份，也可以不换。"),
    KNIGHT("knight", "骑士", "🏇", "good", "好人阵营",
            "白天可以决斗。猜中狼人则对方出局，猜错则你出局。"),
    SILENCE_ELDER("silenceElder", "禁言长老", "🤫", "good", "好人阵营",
            "听法官喊「禁言长老请睁眼」，指出明天不能发言的玩家。");

    private static final Map<String, WolfRole> BY_KEY = Stream.of(values())
            .collect(Collectors.toMap(WolfRole::getKey, Function.identity()));

    private final String key;
    private final String name;
    private final String emoji;
    private final String camp;
    private final String campName;
    private final String hint;

    WolfRole(String key, String name, String emoji, String camp, String campName, String hint) {
        this.key = key;
        this.name = name;
        this.emoji = emoji;
        this.camp = camp;
        this.campName = campName;
        this.hint = hint;
    }

    public static WolfRole fromKey(String key) {
        WolfRole role = BY_KEY.get(key);
        if (role == null) {
            throw new IllegalArgumentException("未知身份: " + key);
        }
        return role;
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

    public String getCampName() {
        return campName;
    }

    public String getHint() {
        return hint;
    }
}
