/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.controller.AwlGameSocketController
 *  cn.game.board.model.avalon.dto.ClientMessage
 *  cn.game.board.model.avalon.dto.GameRoom
 *  cn.game.board.model.avalon.dto.MissionRequest
 *  cn.game.board.model.avalon.dto.Player
 *  cn.game.board.model.avalon.dto.VoteRequest
 *  cn.game.board.service.MissionService
 *  cn.game.board.service.RoomService
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.context.event.EventListener
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.messaging.Message
 *  org.springframework.messaging.handler.annotation.MessageMapping
 *  org.springframework.messaging.handler.annotation.Payload
 *  org.springframework.messaging.simp.SimpMessageHeaderAccessor
 *  org.springframework.messaging.simp.SimpMessagingTemplate
 *  org.springframework.messaging.simp.stomp.StompHeaderAccessor
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.socket.messaging.SessionDisconnectEvent
 */
package cn.game.board.controller;

import cn.game.board.model.avalon.dto.ClientMessage;
import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.MissionRequest;
import cn.game.board.model.avalon.dto.Player;
import cn.game.board.model.avalon.dto.VoteRequest;
import cn.game.board.service.MissionService;
import cn.game.board.service.RoomService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class AwlGameSocketController {
    private static final Logger log = LoggerFactory.getLogger(AwlGameSocketController.class);
    private final RoomService roomService;
    private final MissionService missionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @MessageMapping(value={"/create-room"})
    public void createRoom(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("\u521b\u5efa\u623f\u95f4\u8bf7\u6c42: sessionId={}, roomNumber={}", (Object)sessionId, (Object)msg.getRoomNumber());
        try {
            GameRoom room = this.roomService.createRoom(msg.getRoomNumber(), msg.getTotalPlayers(), sessionId, msg.getPreferredRole());
            log.info("\u521b\u5efa\u623f\u95f4\u6210\u529f: sessionId={}, roomNumber={}, playersSize={}", new Object[]{sessionId, room.getRoomNumber(), room.getPlayers() != null ? room.getPlayers().size() : 0});
            this.messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-created", (Object)room);
            log.info("\u5df2\u53d1\u9001 room-created \u6d88\u606f: sessionId={}, roomNumber={}", (Object)sessionId, (Object)room.getRoomNumber());
            Player player = (Player)room.getPlayers().get(sessionId);
            if (player != null) {
                this.sendPlayerInfo(sessionId, player.getPlayerId());
                log.info("\u5df2\u53d1\u9001 player-info \u6d88\u606f: sessionId={}, playerId={}", (Object)sessionId, (Object)player.getPlayerId());
            } else {
                log.warn("\u521b\u5efa\u623f\u95f4\u540e\u672a\u627e\u5230\u73a9\u5bb6: sessionId={}, roomNumber={}", (Object)sessionId, (Object)room.getRoomNumber());
            }
        }
        catch (Exception e) {
            log.error("\u521b\u5efa\u623f\u95f4\u5931\u8d25: sessionId={}, error={}", new Object[]{sessionId, e.getMessage(), e});
            this.sendErrorToUser(sessionId, e.getMessage());
        }
    }

    @MessageMapping(value={"/join-room"})
    public void joinRoom(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("[\u52a0\u5165\u623f\u95f4] \u6536\u5230\u52a0\u5165\u623f\u95f4\u8bf7\u6c42: sessionId={}, roomNumber={}", (Object)sessionId, (Object)msg.getRoomNumber());
        try {
            GameRoom room = this.roomService.joinRoom(msg.getRoomNumber(), sessionId, msg.getPreferredRole());
            if (room != null && room.getPlayers().containsKey(sessionId)) {
                log.info("[\u52a0\u5165\u623f\u95f4] \u2705 \u73a9\u5bb6 {} \u6210\u529f\u52a0\u5165\u623f\u95f4 {}\uff0c\u53d1\u9001 room-created \u6d88\u606f", (Object)sessionId, (Object)msg.getRoomNumber());
                this.messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-created", (Object)room);
            } else {
                log.warn("[\u52a0\u5165\u623f\u95f4] \u26a0\ufe0f \u73a9\u5bb6 {} \u52a0\u5165\u623f\u95f4 {} \u540e\u672a\u5728\u623f\u95f4\u4e2d\u627e\u5230\uff0c\u53ef\u80fd\u5df2\u5b58\u5728", (Object)sessionId, (Object)msg.getRoomNumber());
                if (room != null) {
                    this.messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-created", (Object)room);
                }
            }
            Player player = (Player)room.getPlayers().get(sessionId);
            if (player != null) {
                this.sendPlayerInfo(sessionId, player.getPlayerId());
            }
        }
        catch (Exception e) {
            log.error("[\u52a0\u5165\u623f\u95f4] \u274c \u52a0\u5165\u623f\u95f4\u5931\u8d25: sessionId={}, roomNumber={}, error={}", new Object[]{sessionId, msg.getRoomNumber(), e.getMessage(), e});
            this.sendErrorToUser(sessionId, e.getMessage());
        }
    }

    @MessageMapping(value={"/rejoin-with-id"})
    public void rejoinWithId(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String newSessionId = headerAccessor.getSessionId();
        log.info("[\u91cd\u8fde\u8bf7\u6c42] \u6536\u5230\u91cd\u8fde\u8bf7\u6c42: sessionId={}, msg={}", (Object)newSessionId, (Object)msg);
        if (msg == null || msg.getRoomNumber() == null || msg.getPlayerId() == null || newSessionId == null) {
            log.error("[\u91cd\u8fde\u8bf7\u6c42] \u65e0\u6548\u7684\u91cd\u8fde\u8bf7\u6c42: sessionId={}, msg={}", (Object)newSessionId, (Object)msg);
            if (newSessionId != null) {
                this.sendErrorToUser(newSessionId, "\u65e0\u6548\u7684\u91cd\u8fde\u8bf7\u6c42");
            }
            return;
        }
        try {
            GameRoom room = this.roomService.getRoomByKey("avalon:room:" + msg.getRoomNumber());
            if (room == null) {
                log.error("[\u91cd\u8fde\u8bf7\u6c42] \u274c \u623f\u95f4\u4e0d\u5b58\u5728\u6216\u5df2\u8fc7\u671f: roomNumber={}, playerId={}", (Object)msg.getRoomNumber(), (Object)msg.getPlayerId());
                this.sendErrorToUser(newSessionId, "\u623f\u95f4\u4e0d\u5b58\u5728\u6216\u5df2\u8fc7\u671f\uff08\u623f\u95f4\u6570\u636e\u5df2\u8d85\u8fc724\u5c0f\u65f6\u672a\u4f7f\u7528\uff0c\u5df2\u88ab\u6e05\u7406\uff09");
                return;
            }
            log.info("[\u91cd\u8fde\u8bf7\u6c42] \u623f\u95f4\u72b6\u6001: roomNumber={}, gameStarted={}, playersSize={}, initialPlayersSize={}", new Object[]{msg.getRoomNumber(), room.isGameStarted(), room.getPlayers() != null ? room.getPlayers().size() : 0, room.getInitialPlayers() != null ? room.getInitialPlayers().size() : 0});
            this.roomService.rejoinWithId(msg.getRoomNumber(), msg.getPlayerId(), newSessionId, msg.getPreferredRole());
            log.info("[\u91cd\u8fde\u8bf7\u6c42] \u2705 \u73a9\u5bb6 {} \u91cd\u8fde\u623f\u95f4 {} \u6210\u529f, \u65b0sessionId={}", new Object[]{msg.getPlayerId(), msg.getRoomNumber(), newSessionId});
        }
        catch (Exception e) {
            log.error("[\u91cd\u8fde\u8bf7\u6c42] \u274c \u91cd\u8fde\u5931\u8d25: playerId={}, roomNumber={}, sessionId={}, error={}", new Object[]{msg.getPlayerId(), msg.getRoomNumber(), newSessionId, e.getMessage(), e});
            this.sendErrorToUser(newSessionId, "\u91cd\u8fde\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/update-preferred-role"})
    public void updatePreferredRole(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("[\u66f4\u65b0\u9996\u9009\u8eab\u4efd] \u65e0\u6548\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u7684\u9996\u9009\u8eab\u4efd\u8bf7\u6c42");
            }
            return;
        }
        try {
            this.roomService.updatePreferredRole(msg.getRoomNumber(), sessionId, msg.getPlayerId(), msg.getPreferredRole());
        }
        catch (Exception e) {
            log.error("[\u66f4\u65b0\u9996\u9009\u8eab\u4efd] \u5931\u8d25: sessionId={}, roomNumber={}, error={}", new Object[]{sessionId, msg.getRoomNumber(), e.getMessage()});
        }
    }

    @MessageMapping(value={"/select-seat"})
    public void selectSeat(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        try {
            this.roomService.selectSeat(msg.getRoomNumber(), sessionId, msg.getSeatNumber());
        }
        catch (Exception e) {
            log.error("\u9009\u62e9\u5ea7\u4f4d\u5931\u8d25: sessionId={}, seatNumber={}, error={}", new Object[]{sessionId, msg.getSeatNumber(), e.getMessage()});
            this.sendErrorToUser(sessionId, e.getMessage());
        }
    }

    @MessageMapping(value={"/ready"})
    public void setReady(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u51c6\u5907\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42");
            }
            return;
        }
        try {
            this.roomService.setReady(msg.getRoomNumber(), sessionId, msg.isReady());
        }
        catch (Exception e) {
            log.error("\u8bbe\u7f6e\u51c6\u5907\u72b6\u6001\u5931\u8d25: sessionId={}, error={}", (Object)sessionId, (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u8bbe\u7f6e\u51c6\u5907\u72b6\u6001\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/return-to-room"})
    public void returnToRoom(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u8fd4\u56de\u623f\u95f4\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42");
            }
            return;
        }
        try {
            this.roomService.handleReturnToRoom(msg.getRoomNumber(), sessionId);
        }
        catch (Exception e) {
            log.error("\u91cd\u7f6e\u623f\u95f4\u5931\u8d25: roomNumber={}, error={}", (Object)msg.getRoomNumber(), (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u91cd\u7f6e\u623f\u95f4\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/kick-player"})
    public void kickPlayer(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || msg.getTargetPlayerId() == null || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u8e22\u4eba\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u7684\u8e22\u4eba\u8bf7\u6c42");
            }
            return;
        }
        try {
            this.roomService.kickPlayer(msg.getRoomNumber(), sessionId, msg.getTargetPlayerId());
        }
        catch (Exception e) {
            log.error("\u8e22\u4eba\u5931\u8d25: error={}", (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u8e22\u4eba\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/transfer-host"})
    public void transferHost(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || msg.getTargetPlayerId() == null || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u8f6c\u8ba9\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u7684\u8f6c\u8ba9\u8bf7\u6c42");
            }
            return;
        }
        try {
            this.roomService.transferHost(msg.getRoomNumber(), sessionId, msg.getTargetPlayerId());
        }
        catch (Exception e) {
            log.error("\u8f6c\u8ba9\u623f\u4e3b\u5931\u8d25: error={}", (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u8f6c\u8ba9\u623f\u4e3b\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/start-mission"})
    public void startMission(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("[Controller] \u6536\u5230 /app/start-mission \u8bf7\u6c42: sessionId={}, msg={}", (Object)sessionId, (Object)msg);
        if (msg == null) {
            log.error("\u6d88\u606f\u4f53\u4e3a\u7a7a: sessionId={}", (Object)sessionId);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42\uff1a\u6d88\u606f\u4f53\u4e3a\u7a7a");
            }
            return;
        }
        if (msg.getRoomNumber() == null) {
            log.error("\u623f\u95f4\u53f7\u4e3a\u7a7a: sessionId={}, msg={}", (Object)sessionId, (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42\uff1a\u623f\u95f4\u53f7\u4e3a\u7a7a");
            }
            return;
        }
        if (sessionId == null) {
            log.error("Session ID \u4e3a\u7a7a: msg={}", (Object)msg);
            return;
        }
        try {
            boolean needStartNewMission;
            log.info("\u5f00\u59cb\u5904\u7406\u4efb\u52a1\u8bf7\u6c42: \u623f\u95f4\u53f7={}, sessionId={}", (Object)msg.getRoomNumber(), (Object)sessionId);
            GameRoom room = this.roomService.getRoomByKey("avalon:room:" + msg.getRoomNumber());
            if (room == null) {
                log.error("\u623f\u95f4\u4e0d\u5b58\u5728: \u623f\u95f4\u53f7={}", (Object)msg.getRoomNumber());
                throw new RuntimeException("\u623f\u95f4\u4e0d\u5b58\u5728");
            }
            log.info("\u627e\u5230\u623f\u95f4: \u623f\u95f4\u53f7={}, \u6e38\u620f\u5df2\u5f00\u59cb={}, \u5f53\u524d\u8f6e\u6b21={}", new Object[]{room.getRoomNumber(), room.isGameStarted(), room.getCurrentMissionRound()});
            if (!room.isGameStarted()) {
                log.warn("\u6e38\u620f\u5c1a\u672a\u5f00\u59cb\uff0c\u65e0\u6cd5\u5f00\u59cb\u4efb\u52a1: \u623f\u95f4\u53f7={}", (Object)room.getRoomNumber());
                throw new RuntimeException("\u6e38\u620f\u5c1a\u672a\u5f00\u59cb\uff0c\u65e0\u6cd5\u5f00\u59cb\u4efb\u52a1");
            }
            boolean bl = needStartNewMission = room.getCurrentMissionRound() == 0 || room.isMissionVotingCompleted() && room.getMissionResult() != null;
            if (needStartNewMission) {
                log.info("\u68c0\u6d4b\u5230\u9700\u8981\u5f00\u59cb\u65b0\u4efb\u52a1: \u5f53\u524d\u8f6e\u6b21={}, \u6295\u7968\u5b8c\u6210={}, \u4efb\u52a1\u7ed3\u679c={}", new Object[]{room.getCurrentMissionRound(), room.isMissionVotingCompleted(), room.getMissionResult()});
                this.missionService.startMission(room);
                log.info("\u4efb\u52a1\u5df2\u5f00\u59cb: \u623f\u95f4\u53f7={}, \u5f53\u524d\u8f6e\u6b21={}, \u4efb\u52a1\u6210\u5458\u6570={}", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), room.getSelectedMissionMembers() != null ? room.getSelectedMissionMembers().size() : 0});
            }
            this.missionService.joinMission(room, sessionId);
            log.info("\u73a9\u5bb6\u5df2\u52a0\u5165\u4efb\u52a1: \u623f\u95f4\u53f7={}, sessionId={}, \u5f53\u524d\u4efb\u52a1\u6210\u5458\u6570={}", new Object[]{room.getRoomNumber(), sessionId, room.getSelectedMissionMembers().size()});
            this.roomService.saveRoom(room);
            this.roomService.broadcastRoomState(msg.getRoomNumber());
            log.info("\u623f\u95f4\u72b6\u6001\u5df2\u5e7f\u64ad: \u623f\u95f4\u53f7={}", (Object)msg.getRoomNumber());
        }
        catch (Exception e) {
            log.error("\u5f00\u59cb\u4efb\u52a1\u5931\u8d25: sessionId={}, \u623f\u95f4\u53f7={}, error={}", new Object[]{sessionId, msg.getRoomNumber(), e.getMessage(), e});
            this.sendErrorToUser(sessionId, "\u5f00\u59cb\u4efb\u52a1\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/leave-mission"})
    public void leaveMission(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u9000\u51fa\u4efb\u52a1\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42");
            }
            return;
        }
        try {
            GameRoom room = this.roomService.getRoomByKey("avalon:room:" + msg.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("\u623f\u95f4\u4e0d\u5b58\u5728");
            }
            this.missionService.leaveMission(room, sessionId);
            this.roomService.saveRoom(room);
            this.roomService.broadcastRoomState(msg.getRoomNumber());
        }
        catch (Exception e) {
            log.error("\u9000\u51fa\u4efb\u52a1\u5931\u8d25: error={}", (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u9000\u51fa\u4efb\u52a1\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/toggle-mission-member"})
    public void toggleMissionMember(@Payload MissionRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (request == null || request.getRoomNumber() == null || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u4e0a\u8f66/\u4e0b\u8f66\u8bf7\u6c42: {}", (Object)request);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42");
            }
            return;
        }
        try {
            GameRoom room = this.roomService.getRoomByKey("avalon:room:" + request.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("\u623f\u95f4\u4e0d\u5b58\u5728");
            }
            this.missionService.toggleMissionMember(room, sessionId, request.isJoinMission());
            this.roomService.saveRoom(room);
            this.roomService.broadcastRoomState(request.getRoomNumber());
        }
        catch (Exception e) {
            log.error("\u4e0a\u8f66/\u4e0b\u8f66\u5931\u8d25: error={}", (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u64cd\u4f5c\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/vote"})
    public void vote(@Payload VoteRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (request == null || request.getRoomNumber() == null || request.getVote() == null || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u6295\u7968\u8bf7\u6c42: {}", (Object)request);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42");
            }
            return;
        }
        try {
            GameRoom room = this.roomService.getRoomByKey("avalon:room:" + request.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("\u623f\u95f4\u4e0d\u5b58\u5728");
            }
            this.missionService.vote(room, sessionId, request.getVote());
            this.roomService.saveRoom(room);
            this.roomService.broadcastRoomState(request.getRoomNumber());
        }
        catch (Exception e) {
            log.error("\u6295\u7968\u5931\u8d25: error={}", (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u6295\u7968\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/lake-lady-inspect"})
    public void lakeLadyInspect(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("[Controller] \u6536\u5230 /app/lake-lady-inspect \u8bf7\u6c42: sessionId={}, msg={}", (Object)sessionId, (Object)msg);
        if (msg == null || msg.getRoomNumber() == null || msg.getSeatNumber() <= 0 || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u6e56\u4e2d\u4ed9\u5973\u67e5\u9a8c\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42");
            }
            return;
        }
        try {
            GameRoom room = this.roomService.getRoomByKey("avalon:room:" + msg.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("\u623f\u95f4\u4e0d\u5b58\u5728");
            }
            this.missionService.inspectPlayerWithLakeLady(room, sessionId, msg.getSeatNumber());
            this.roomService.saveRoom(room);
            this.roomService.broadcastRoomState(msg.getRoomNumber());
        }
        catch (Exception e) {
            log.error("\u6e56\u4e2d\u4ed9\u5973\u67e5\u9a8c\u5931\u8d25: error={}", (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u67e5\u9a8c\u5931\u8d25: " + e.getMessage());
        }
    }

    @MessageMapping(value={"/lake-lady-publish"})
    public void lakeLadyPublish(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("[Controller] \u6536\u5230 /app/lake-lady-publish \u8bf7\u6c42: sessionId={}, msg={}", (Object)sessionId, (Object)msg);
        if (msg == null || msg.getRoomNumber() == null || msg.getSeatNumber() <= 0 || sessionId == null) {
            log.error("\u65e0\u6548\u7684\u6e56\u4e2d\u4ed9\u5973\u516c\u5f00\u8bf7\u6c42: {}", (Object)msg);
            if (sessionId != null) {
                this.sendErrorToUser(sessionId, "\u65e0\u6548\u8bf7\u6c42");
            }
            return;
        }
        try {
            GameRoom room = this.roomService.getRoomByKey("avalon:room:" + msg.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("\u623f\u95f4\u4e0d\u5b58\u5728");
            }
            String publicResult = msg.getTargetPlayerId();
            if (publicResult == null || !"good".equals(publicResult) && !"evil".equals(publicResult) && !"unknown".equals(publicResult)) {
                throw new RuntimeException("\u65e0\u6548\u7684\u516c\u5f00\u7ed3\u679c");
            }
            this.missionService.publishLakeLadyResult(room, sessionId, msg.getSeatNumber(), publicResult);
            this.roomService.saveRoom(room);
            this.roomService.broadcastRoomState(msg.getRoomNumber());
        }
        catch (Exception e) {
            log.error("\u6e56\u4e2d\u4ed9\u5973\u516c\u5f00\u5931\u8d25: error={}", (Object)e.getMessage());
            this.sendErrorToUser(sessionId, "\u516c\u5f00\u5931\u8d25: " + e.getMessage());
        }
    }

    @EventListener
    public void onDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap((Message)event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            log.warn("[\u65ad\u5f00\u8fde\u63a5] sessionId \u4e3a null");
            return;
        }
        try {
            String roomNumber = (String)this.stringRedisTemplate.opsForValue().get((Object)("avalon:session:" + sessionId));
            if (roomNumber != null) {
                Player player;
                log.info("[\u65ad\u5f00\u8fde\u63a5] \u73a9\u5bb6\u65ad\u5f00\u8fde\u63a5: sessionId={}, roomNumber={}", (Object)sessionId, (Object)roomNumber);
                GameRoom room = this.roomService.getRoomByKey("avalon:room:" + roomNumber);
                if (room != null && (player = (Player)room.getPlayers().get(sessionId)) != null) {
                    log.info("[\u65ad\u5f00\u8fde\u63a5] \u73a9\u5bb6\u4fe1\u606f: playerId={}, seatNumber={}, identity={}, gameStarted={}, initialPlayersSize={}", new Object[]{player.getPlayerId(), player.getSeatNumber(), player.getIdentity(), room.isGameStarted(), room.getInitialPlayers() != null ? room.getInitialPlayers().size() : 0});
                }
                this.roomService.removePlayer(roomNumber, sessionId);
                log.info("[\u65ad\u5f00\u8fde\u63a5] \u73a9\u5bb6\u5df2\u4ece\u623f\u95f4\u79fb\u9664: sessionId={}, roomNumber={}", (Object)sessionId, (Object)roomNumber);
            } else {
                log.warn("[\u65ad\u5f00\u8fde\u63a5] \u672a\u627e\u5230 session \u5bf9\u5e94\u7684\u623f\u95f4: sessionId={}", (Object)sessionId);
            }
        }
        catch (Exception e) {
            log.error("[\u65ad\u5f00\u8fde\u63a5] \u5904\u7406\u65ad\u5f00\u8fde\u63a5\u4e8b\u4ef6\u5931\u8d25: sessionId={}, error={}", new Object[]{sessionId, e.getMessage(), e});
        }
    }

    private void sendErrorToUser(String sessionId, String errorMessage) {
        if (sessionId == null || errorMessage == null) {
            return;
        }
        try {
            this.messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", (Object)Map.of((Object)"error", (Object)errorMessage));
        }
        catch (Exception e) {
            log.error("\u53d1\u9001\u9519\u8bef\u6d88\u606f\u5931\u8d25: sessionId={}, error={}", (Object)sessionId, (Object)e.getMessage());
        }
    }

    private void sendPlayerInfo(String sessionId, String playerId) {
        if (sessionId == null || playerId == null) {
            return;
        }
        try {
            this.messagingTemplate.convertAndSendToUser(sessionId, "/queue/player-info", (Object)Map.of((Object)"playerId", (Object)playerId));
        }
        catch (Exception e) {
            log.error("\u53d1\u9001\u73a9\u5bb6\u4fe1\u606f\u5931\u8d25: sessionId={}, playerId={}, error={}", new Object[]{sessionId, playerId, e.getMessage()});
        }
    }

    public AwlGameSocketController(RoomService roomService, MissionService missionService, SimpMessagingTemplate messagingTemplate, StringRedisTemplate stringRedisTemplate) {
        this.roomService = roomService;
        this.missionService = missionService;
        this.messagingTemplate = messagingTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }
}

