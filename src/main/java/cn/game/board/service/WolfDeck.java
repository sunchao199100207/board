package cn.game.board.service;

import cn.game.board.model.werewolf.WolfCardView;
import cn.game.board.model.werewolf.WolfRole;
import cn.game.board.model.werewolf.WolfRoleConfig;
import cn.game.board.model.werewolf.WolfScriptStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 牌组与法官口述词。洗牌只打乱身份，不推进夜晚流程。
 */
public final class WolfDeck {

    public static final int MIN_PLAYERS = 6;
    public static final int MAX_PLAYERS = 12;

    private WolfDeck() {
    }

    /** 默认 10 人：3 平民、2 狼人、狼王、预言家、女巫、猎人、守卫。 */
    public static WolfRoleConfig defaultConfig() {
        WolfRoleConfig config = new WolfRoleConfig();
        config.setCivilian(3);
        config.setWerewolf(2);
        config.setWolfKing(true);
        config.setSeer(true);
        config.setWitch(true);
        config.setHunter(true);
        config.setGuard(true);
        return config;
    }

    /**
     * 按配置生成未洗牌的牌组。狼人不少于 1，且必须少于好人。
     */
    public static List<String> build(WolfRoleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("请先配置身份");
        }
        if (config.getCivilian() < 0 || config.getWerewolf() < 0) {
            throw new IllegalArgumentException("身份数量不能为负");
        }

        int wolves = config.getWerewolf() + (config.isWolfKing() ? 1 : 0) + (config.isWhiteWolfKing() ? 1 : 0);
        int gods = (config.isSeer() ? 1 : 0) + (config.isWitch() ? 1 : 0)
                + (config.isHunter() ? 1 : 0) + (config.isGuard() ? 1 : 0)
                + (config.isThief() ? 1 : 0) + (config.isKnight() ? 1 : 0)
                + (config.isSilenceElder() ? 1 : 0);
        int total = config.getCivilian() + wolves + gods;

        if (wolves < 1) {
            throw new IllegalArgumentException("至少要有一名狼人");
        }
        if (total < MIN_PLAYERS || total > MAX_PLAYERS) {
            throw new IllegalArgumentException("人数需在 " + MIN_PLAYERS + " 到 " + MAX_PLAYERS + " 人之间，当前 " + total + " 人");
        }
        if (wolves >= total - wolves) {
            throw new IllegalArgumentException("狼人不能多于或等于好人");
        }

        List<String> deck = new ArrayList<>(total);
        addCopies(deck, WolfRole.CIVILIAN.getKey(), config.getCivilian());
        addCopies(deck, WolfRole.WEREWOLF.getKey(), config.getWerewolf());
        if (config.isWolfKing()) {
            deck.add(WolfRole.WOLF_KING.getKey());
        }
        if (config.isWhiteWolfKing()) {
            deck.add(WolfRole.WHITE_WOLF_KING.getKey());
        }
        if (config.isSeer()) {
            deck.add(WolfRole.SEER.getKey());
        }
        if (config.isWitch()) {
            deck.add(WolfRole.WITCH.getKey());
        }
        if (config.isHunter()) {
            deck.add(WolfRole.HUNTER.getKey());
        }
        if (config.isGuard()) {
            deck.add(WolfRole.GUARD.getKey());
        }
        if (config.isThief()) {
            deck.add(WolfRole.THIEF.getKey());
        }
        if (config.isKnight()) {
            deck.add(WolfRole.KNIGHT.getKey());
        }
        if (config.isSilenceElder()) {
            deck.add(WolfRole.SILENCE_ELDER.getKey());
        }
        return deck;
    }

    public static List<String> shuffle(List<String> deck, Random random) {
        List<String> shuffled = new ArrayList<>(deck);
        Collections.shuffle(shuffled, random);
        return shuffled;
    }

    public static WolfCardView toCard(String roleKey) {
        WolfRole role = WolfRole.fromKey(roleKey);
        return WolfCardView.builder()
                .role(role.getKey())
                .roleName(role.getName())
                .emoji(role.getEmoji())
                .camp(role.getCamp())
                .campName(role.getCampName())
                .hint(role.getHint())
                .imageUrl("img/lrs/" + role.getName() + ".jpg")
                .build();
    }

    public static String describe(WolfRoleConfig config) {
        List<String> parts = new ArrayList<>();
        if (config.getCivilian() > 0) {
            parts.add("平民×" + config.getCivilian());
        }
        if (config.getWerewolf() > 0) {
            parts.add("狼人×" + config.getWerewolf());
        }
        if (config.isWolfKing()) {
            parts.add("黑狼王");
        }
        if (config.isWhiteWolfKing()) {
            parts.add("白狼王");
        }
        if (config.isSeer()) {
            parts.add("预言家");
        }
        if (config.isWitch()) {
            parts.add("女巫");
        }
        if (config.isHunter()) {
            parts.add("猎人");
        }
        if (config.isGuard()) {
            parts.add("守卫");
        }
        if (config.isThief()) {
            parts.add("盗贼");
        }
        if (config.isKnight()) {
            parts.add("骑士");
        }
        if (config.isSilenceElder()) {
            parts.add("禁言长老");
        }
        return String.join("、", parts);
    }

    /**
     * 单身份夜间口述顺序：盗贼、守卫、狼人、女巫、预言家、禁言长老、猎人。骑士在白天。
     */
    public static List<WolfScriptStep> script(WolfRoleConfig config) {
        List<WolfScriptStep> steps = new ArrayList<>();
        steps.add(step("天黑", "天黑请闭眼。"));
        if (config.isThief()) {
            steps.add(step("盗贼", "盗贼请睁眼。请指出要交换身份的玩家，也可以不换。交换后双方身份对调。这里仍显示最初发的牌，新身份请你自己记下。确认后请闭眼。"));
        }
        if (config.isGuard()) {
            steps.add(step("守卫台词", "守卫请睁眼。请用手势指出今晚要守护的玩家，确认后请闭眼。"));
        }
        if (config.getWerewolf() > 0 || config.isWolfKing() || config.isWhiteWolfKing()) {
            steps.add(step("狼人台词", "狼人请睁眼，互相确认队友。请统一意见，用手势指出今晚要刀的玩家，确认后请闭眼。"));
        }
        if (config.isWitch()) {
            steps.add(step("女巫台词", "女巫请睁眼。今晚被刀的是几号，问是否使用解药、是否使用毒药，确认后请闭眼。"));
        }
        if (config.isSeer()) {
            steps.add(step("预言家台词", "预言家请睁眼。请玩家用手势指出要查验的人，确认后请闭眼。"));
        }
        if (config.isSilenceElder()) {
            steps.add(step("禁言长老", "禁言长老请睁眼。请指出明天白天不能发言的玩家。确认后请闭眼。"));
        }
        if (config.isHunter()) {
            steps.add(step("猎人台词", "猎人请睁眼。你的开枪状态是，请问是否需要发动技能，确认后请闭眼。"));
        }
        steps.add(step("白天台词", "天亮了。请公布死讯，组织发言和投票。"));
        if (config.isKnight()) {
            steps.add(step("骑士", "骑士可以在白天决斗：猜中狼人则对方出局，猜错则骑士出局。"));
        }
        if (config.isWhiteWolfKing()) {
            steps.add(step("白狼王", "白狼王可以在白天自爆，带走一名玩家。"));
        }
        return steps;
    }

    private static WolfScriptStep step(String title, String text) {
        return WolfScriptStep.builder().title(title).text(text).build();
    }

    private static void addCopies(List<String> deck, String role, int count) {
        for (int i = 0; i < count; i++) {
            deck.add(role);
        }
    }
}
