/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.exception.SeatTakenException
 *  cn.game.board.model.avalon.dto.IdentityRequest
 *  cn.game.board.model.avalon.dto.IdentityResponse
 *  cn.game.board.service.GameService
 *  cn.game.board.service.GameService$RoleInfo
 *  lombok.Generated
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Service
 */
package cn.game.board.service;

import cn.game.board.exception.SeatTakenException;
import cn.game.board.model.avalon.dto.IdentityRequest;
import cn.game.board.model.avalon.dto.IdentityResponse;
import cn.game.board.service.GameService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Generated;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final StringRedisTemplate redisTemplate;
    private static final Map<Integer, List<String>> IDENTITIES_MAP = Map.ofEntries((Map.Entry[])new Map.Entry[]{Map.entry((Object)5, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2")), Map.entry((Object)6, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3")), Map.entry((Object)7, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u5965\u4f2f\u4f26")), Map.entry((Object)8, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259")), Map.entry((Object)9, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u5fb7\u96f7\u5fb7")), Map.entry((Object)10, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)"\u5965\u4f2f\u4f26"))});
    private static final Map<String, RoleInfo> ROLE_INFO_MAP = Map.of((Object)"\u6885\u6797", (Object)new RoleInfo("good", "\ud83e\uddd9\u200d\u2642\ufe0f", "\u80fd\u770b\u5230\u6240\u6709\u574f\u4eba\uff08\u83ab\u5fb7\u96f7\u5fb7\u9664\u5916\uff09\u3002", "img/\u6885\u6797.webp"), (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)new RoleInfo("good", "\ud83d\udd75\ufe0f\u200d\u2640\ufe0f", "\u80fd\u770b\u5230\u6885\u6797\u548c\u83ab\u7518\u5a1c\uff08\u5206\u4e0d\u6e05\u8c01\u662f\u8c01\uff09\u3002", "img/\u6d3e\u897f\u7ef4\u5c14.webp"), (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)new RoleInfo("good", "\ud83d\udc51", "\u6ca1\u6709\u7279\u6b8a\u6280\u80fd\uff0c\u51ed\u903b\u8f91\u5224\u65ad\u3002", "img/\u4e9a\u745f\u7684\u5fe0\u81e3.webp"), (Object)"\u83ab\u7518\u5a1c", (Object)new RoleInfo("evil", "\ud83d\ude08", "\u5047\u626e\u6885\u6797\uff0c\u8ff7\u60d1\u6d3e\u897f\u7ef4\u5c14\u3002\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/\u83ab\u7518\u5a1c.webp"), (Object)"\u523a\u5ba2", (Object)new RoleInfo("evil", "\ud83d\udde1\ufe0f", "\u597d\u4eba\u8d62\u540e\u53ef\u523a\u6740\u6885\u6797\u3002\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/\u523a\u5ba2.webp"), (Object)"\u5965\u4f2f\u4f26", (Object)new RoleInfo("evil", "\ud83d\udc7b", "\u574f\u4eba\uff0c\u4f46\u4e0e\u961f\u53cb\u4e92\u4e0d\u76f8\u8bc6\uff08\u6885\u6797\u80fd\u770b\u5230\u4ed6\uff09\u3002", "img/\u5965\u4f2f\u4f26.webp"), (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)new RoleInfo("evil", "\ud83d\udc79", "\u574f\u4eba\uff0c\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259.webp"), (Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)new RoleInfo("evil", "\ud83d\udc80", "\u574f\u4eba\u9886\u8896\uff0c\u6885\u6797\u770b\u4e0d\u5230\u4ed6\u3002\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/\u83ab\u5fb7\u96f7\u5fb7.webp"));
    private static final RoleInfo UNKNOWN_ROLE = new RoleInfo("good", "\u2753", "", "/images/roles/unknown.png");

    public IdentityResponse getIdentity(IdentityRequest request) {
        String key = String.format("avalon:game:%d:seat:%d", request.getGameNumber(), request.getSeatNumber());
        Boolean wasSet = this.redisTemplate.opsForValue().setIfAbsent((Object)key, (Object)"viewed", 24L, TimeUnit.HOURS);
        if (wasSet == null || !wasSet.booleanValue()) {
            throw new SeatTakenException("\u6b64\u5ea7\u4f4d\u5df2\u88ab\u67e5\u770b\uff01");
        }
        List baseIdentities = (List)IDENTITIES_MAP.get(request.getPlayersNumber());
        if (baseIdentities == null) {
            throw new IllegalArgumentException("\u65e0\u6548\u7684\u603b\u4eba\u6570");
        }
        ArrayList shuffledIdentities = new ArrayList(baseIdentities);
        long seed = request.getGameNumber() + LocalDate.now().toEpochDay();
        Random seededRandom = new Random(seed);
        Collections.shuffle(shuffledIdentities, seededRandom);
        String myIdentity = (String)shuffledIdentities.get(request.getSeatNumber() - 1);
        RoleInfo myRoleInfo = ROLE_INFO_MAP.getOrDefault(myIdentity, UNKNOWN_ROLE);
        ArrayList<Integer> seenPlayers = new ArrayList<Integer>();
        String seenTitle = "\u4f60\u4ec0\u4e48\u4e5f\u6ca1\u770b\u5230";
        String seenSubtitle = "\u4e0d\u662f\u5148\u77e5\uff0c\u80dc\u4f3c\u5148\u77e5";
        Set evilTeammates = Set.of((Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)"\u83ab\u5fb7\u96f7\u5fb7");
        switch (myIdentity) {
            case "\u6885\u6797": {
                seenTitle = "\u4f60\u770b\u5230\u4e86 (\u574f\u4eba):";
                seenSubtitle = "\u4f60\u65e0\u6cd5\u770b\u5230 \u83ab\u5fb7\u96f7\u5fb7\u3002";
                int i = 0;
                while (i < shuffledIdentities.size()) {
                    if (Set.of((Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u5965\u4f2f\u4f26", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259").contains(shuffledIdentities.get(i))) {
                        seenPlayers.add(i + 1);
                    }
                    ++i;
                }
                break;
            }
            case "\u6d3e\u897f\u7ef4\u5c14": {
                seenTitle = "\u4f60\u770b\u5230\u4e86 (\u6885\u6797/\u83ab\u7518\u5a1c):";
                seenSubtitle = "\u4ed6\u4eec\u4e4b\u4e2d\u4e00\u4e2a\u662f\u6885\u6797\uff0c\u4e00\u4e2a\u662f\u83ab\u7518\u5a1c\u3002";
                int i = 0;
                while (i < shuffledIdentities.size()) {
                    if (Set.of((Object)"\u6885\u6797", (Object)"\u83ab\u7518\u5a1c").contains(shuffledIdentities.get(i))) {
                        seenPlayers.add(i + 1);
                    }
                    ++i;
                }
                break;
            }
            case "\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259": 
            case "\u523a\u5ba2": 
            case "\u83ab\u7518\u5a1c": 
            case "\u83ab\u5fb7\u96f7\u5fb7": {
                seenTitle = "\u4f60\u770b\u5230\u4e86 (\u4f60\u7684\u961f\u53cb):";
                seenSubtitle = "\u4f60\u65e0\u6cd5\u770b\u5230 \u5965\u4f2f\u4f26\u3002";
                int i = 0;
                while (i < shuffledIdentities.size()) {
                    if (evilTeammates.contains(shuffledIdentities.get(i)) && i + 1 != request.getSeatNumber()) {
                        seenPlayers.add(i + 1);
                    }
                    ++i;
                }
                break;
            }
        }
        return IdentityResponse.builder().identity(myIdentity).emoji(myRoleInfo.emoji()).camp(myRoleInfo.camp()).seenTitle(seenTitle).seenPlayers(seenPlayers).seenSubtitle(seenSubtitle).imageUrl(myRoleInfo.imageUrl()).build();
    }

    @Generated
    public GameService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}

