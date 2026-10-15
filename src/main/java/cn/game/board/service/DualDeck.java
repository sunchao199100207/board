package cn.game.board.service;

import cn.game.board.model.dual.DualCardView;
import cn.game.board.model.dual.DualConfig;
import cn.game.board.model.dual.DualRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 双身份发牌。先只生成合法的两张牌组合，再检查金宝宝和狼阵营人数。
 */
public final class DualDeck {

    private DualDeck() {
    }

    public static DualConfig preset(int players) {
        if (players < 6 || players > 8) {
            throw new IllegalArgumentException("双身份只支持 6 到 8 人");
        }
        DualConfig config = new DualConfig();
        config.setPlayers(players);
        return config;
    }

    /**
     * 6 人：预言家、女巫、猎人、守卫、狼人×2、平民×6。
     * 7 人：再加禁言长老、狼王，普通狼人减到 1 张，平民×7。
     * 8 人：再加骑士、狼王，狼人×2，平民×8。
     */
    public static List<String> pool(DualConfig config) {
        int players = config == null ? 0 : config.getPlayers();
        if (players < 6 || players > 8) {
            throw new IllegalArgumentException("双身份只支持 6 到 8 人");
        }
        List<String> cards = new ArrayList<>();
        add(cards, DualRole.SEER, 1);
        add(cards, DualRole.WITCH, 1);
        add(cards, DualRole.HUNTER, 1);
        add(cards, DualRole.GUARD, 1);
        if (players == 7) {
            add(cards, DualRole.SILENCE_ELDER, 1);
            add(cards, DualRole.WOLF_KING, 1);
            add(cards, DualRole.WEREWOLF, 1);
        } else if (players == 8) {
            add(cards, DualRole.KNIGHT, 1);
            add(cards, DualRole.WOLF_KING, 1);
            add(cards, DualRole.WEREWOLF, 2);
        } else {
            add(cards, DualRole.WEREWOLF, 2);
        }
        int villagers = players * 2 - cards.size();
        if (villagers < 1) {
            throw new IllegalArgumentException("这套配置平民不够");
        }
        add(cards, DualRole.VILLAGER, villagers);
        if (cards.size() != players * 2) {
            throw new IllegalStateException("牌数和人数对不上");
        }
        return cards;
    }

    public static String describe(DualConfig config) {
        List<String> cards = pool(config);
        List<String> parts = new ArrayList<>();
        for (DualRole role : DualRole.values()) {
            int count = 0;
            for (String card : cards) {
                if (role.getKey().equals(card)) {
                    count++;
                }
            }
            if (count == 1) {
                parts.add(role.getName());
            } else if (count > 1) {
                parts.add(role.getName() + "×" + count);
            }
        }
        return config.getPlayers() + "人 " + (config.getPlayers() * 2) + "张：" + String.join("、", parts);
    }

    /**
     * 返回每人两张牌。组合本身不含双狼、不含预言家配狼。
     * 金宝宝不足或狼阵营人数不在 2 到 3 人时整副重发。
     */
    public static List<String[]> deal(DualConfig config, Random random) {
        List<String> cards = pool(config);
        for (int attempt = 0; attempt < 500; attempt++) {
            List<String[]> pairs = pairLegally(cards, random);
            if (pairs == null) {
                continue;
            }
            if (goldenBabies(pairs) < 1) {
                continue;
            }
            int wolves = wolfPlayers(pairs);
            if (wolves < 2 || wolves > 3) {
                continue;
            }
            return pairs;
        }
        throw new IllegalStateException("这套牌发不出合法组合，请调整配置后再发");
    }

    public static boolean legal(String left, String right) {
        DualRole a = DualRole.fromKey(left);
        DualRole b = DualRole.fromKey(right);
        if (a.wolf() && b.wolf()) {
            return false;
        }
        if ((a == DualRole.SEER && b.wolf()) || (b == DualRole.SEER && a.wolf())) {
            return false;
        }
        return true;
    }

    public static int goldenBabies(List<String[]> pairs) {
        int count = 0;
        for (String[] pair : pairs) {
            if (DualRole.VILLAGER.getKey().equals(pair[0]) && DualRole.VILLAGER.getKey().equals(pair[1])) {
                count++;
            }
        }
        return count;
    }

    public static int wolfPlayers(List<String[]> pairs) {
        int count = 0;
        for (String[] pair : pairs) {
            if (DualRole.fromKey(pair[0]).wolf() || DualRole.fromKey(pair[1]).wolf()) {
                count++;
            }
        }
        return count;
    }

    public static DualCardView toCard(String roleKey) {
        DualRole role = DualRole.fromKey(roleKey);
        return DualCardView.builder()
                .role(role.getKey())
                .roleName(role.getName())
                .emoji(role.getEmoji())
                .camp(role.getCamp())
                .hint(role.getHint())
                .imageUrl(role.getImageUrl())
                .build();
    }

    public static String campName(String left, String right) {
        if (DualRole.fromKey(left).wolf() || DualRole.fromKey(right).wolf()) {
            return "狼人阵营";
        }
        return "好人阵营";
    }

    public static String campTip(String left, String right) {
        boolean golden = DualRole.VILLAGER.getKey().equals(left) && DualRole.VILLAGER.getKey().equals(right);
        if (golden) {
            return "两张都是平民，你是金宝宝，属于好人阵营。";
        }
        if (DualRole.fromKey(left).wolf() || DualRole.fromKey(right).wolf()) {
            return "两张里有狼人牌，你是狼人阵营。就算狼在下面，阵营也是狼。";
        }
        return "两张都不是狼人牌，你是好人阵营。";
    }

    /**
     * 回溯配对：每次只把第一张未用的牌和一张合法的牌配在一起。
     */
    static List<String[]> pairLegally(List<String> source, Random random) {
        List<String> cards = new ArrayList<>(source);
        Collections.shuffle(cards, random);
        boolean[] used = new boolean[cards.size()];
        List<String[]> pairs = new ArrayList<>();
        if (!search(cards, used, pairs, random)) {
            return null;
        }
        Collections.shuffle(pairs, random);
        for (String[] pair : pairs) {
            if (random.nextBoolean()) {
                String swap = pair[0];
                pair[0] = pair[1];
                pair[1] = swap;
            }
        }
        return pairs;
    }

    private static boolean search(List<String> cards, boolean[] used, List<String[]> pairs, Random random) {
        int first = -1;
        for (int index = 0; index < used.length; index++) {
            if (!used[index]) {
                first = index;
                break;
            }
        }
        if (first < 0) {
            return true;
        }
        List<Integer> partners = new ArrayList<>();
        for (int index = first + 1; index < used.length; index++) {
            if (!used[index] && legal(cards.get(first), cards.get(index))) {
                partners.add(index);
            }
        }
        Collections.shuffle(partners, random);
        for (int partner : partners) {
            used[first] = true;
            used[partner] = true;
            pairs.add(new String[]{cards.get(first), cards.get(partner)});
            if (search(cards, used, pairs, random)) {
                return true;
            }
            pairs.remove(pairs.size() - 1);
            used[first] = false;
            used[partner] = false;
        }
        return false;
    }

    private static void add(List<String> cards, DualRole role, int count) {
        for (int i = 0; i < count; i++) {
            cards.add(role.getKey());
        }
    }
}
