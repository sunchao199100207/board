package cn.game.board.service;

import cn.game.board.model.dual.DualConfig;
import cn.game.board.model.dual.DualRole;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DualDeckTest {

    @Test
    void sixPlayerDeckExcludesIllegalPairsAndKeepsAGoldenBaby() {
        assertDeals(DualDeck.preset(6), 40);
    }

    @Test
    void sevenAndEightUseFixedRolePools() {
        List<String> seven = DualDeck.pool(DualDeck.preset(7));
        assertEquals(14, seven.size());
        assertEquals(1, count(seven, DualRole.SILENCE_ELDER));
        assertEquals(1, count(seven, DualRole.WOLF_KING));
        assertEquals(1, count(seven, DualRole.WEREWOLF));
        assertEquals(1, count(seven, DualRole.GUARD));
        assertEquals(7, count(seven, DualRole.VILLAGER));
        assertEquals(0, count(seven, DualRole.KNIGHT));
        assertDeals(DualDeck.preset(7), 20);

        List<String> eight = DualDeck.pool(DualDeck.preset(8));
        assertEquals(16, eight.size());
        assertEquals(1, count(eight, DualRole.KNIGHT));
        assertEquals(1, count(eight, DualRole.WOLF_KING));
        assertEquals(2, count(eight, DualRole.WEREWOLF));
        assertEquals(8, count(eight, DualRole.VILLAGER));
        assertEquals(0, count(eight, DualRole.SILENCE_ELDER));
        assertDeals(DualDeck.preset(8), 20);

        List<String> six = DualDeck.pool(DualDeck.preset(6));
        assertEquals(2, count(six, DualRole.WEREWOLF));
        assertEquals(0, count(six, DualRole.WOLF_KING));
        assertEquals(6, count(six, DualRole.VILLAGER));
    }

    @Test
    void illegalPairsAreRejectedBeforeTheyCanBeDealt() {
        assertFalse(DualDeck.legal(DualRole.WEREWOLF.getKey(), DualRole.WEREWOLF.getKey()));
        assertFalse(DualDeck.legal(DualRole.WEREWOLF.getKey(), DualRole.WOLF_KING.getKey()));
        assertFalse(DualDeck.legal(DualRole.SEER.getKey(), DualRole.WEREWOLF.getKey()));
        assertFalse(DualDeck.legal(DualRole.WOLF_KING.getKey(), DualRole.SEER.getKey()));
        assertTrue(DualDeck.legal(DualRole.WEREWOLF.getKey(), DualRole.WITCH.getKey()));
        assertTrue(DualDeck.legal(DualRole.HUNTER.getKey(), DualRole.WOLF_KING.getKey()));
        assertTrue(DualDeck.legal(DualRole.VILLAGER.getKey(), DualRole.VILLAGER.getKey()));
    }

    private void assertDeals(DualConfig config, int times) {
        List<String> expected = DualDeck.pool(config);
        for (int i = 0; i < times; i++) {
            List<String[]> pairs = DualDeck.deal(config, new Random(i + 11));
            assertEquals(config.getPlayers(), pairs.size());
            Map<String, Integer> used = new HashMap<>();
            for (String[] pair : pairs) {
                assertTrue(DualDeck.legal(pair[0], pair[1]));
                used.merge(pair[0], 1, Integer::sum);
                used.merge(pair[1], 1, Integer::sum);
            }
            Map<String, Integer> pool = new HashMap<>();
            for (String card : expected) {
                pool.merge(card, 1, Integer::sum);
            }
            assertEquals(pool, used);
            assertTrue(DualDeck.goldenBabies(pairs) >= 1);
            int wolves = DualDeck.wolfPlayers(pairs);
            assertTrue(wolves >= 2 && wolves <= 3);
        }
    }

    private int count(List<String> cards, DualRole role) {
        int count = 0;
        for (String card : cards) {
            if (role.getKey().equals(card)) {
                count++;
            }
        }
        return count;
    }
}
