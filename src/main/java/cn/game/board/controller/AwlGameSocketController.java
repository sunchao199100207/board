package cn.game.board.controller;

import cn.game.board.model.avalon.dto.ClientMessage;
import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.MissionRequest;
import cn.game.board.model.avalon.dto.Player;
import cn.game.board.model.avalon.dto.VoteRequest;
import cn.game.board.service.MissionService;
import cn.game.board.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

/**
 * 处理阿瓦隆房间游戏的 WebSocket 消息控制器
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AwlGameSocketController {

    private final RoomService roomService;
    private final MissionService missionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @MessageMapping("/create-room")
    public void createRoom(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        try {
            GameRoom room = roomService.createRoom(msg.getRoomNumber(), msg.getTotalPlayers(), sessionId, msg.getPreferredRole());
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-created", room);
            Player self = room.getPlayers().get(sessionId);
            if (self != null) {
                sendPlayerInfo(sessionId, self.getPlayerId());
            }
        } catch (Exception e) {
            log.error("创建房间失败: sessionId={}, roomNumber={}, error={}", sessionId, msg.getRoomNumber(), e.getMessage());
            sendErrorToUser(sessionId, e.getMessage());
        }
    }

    @MessageMapping("/join-room")
    public void joinRoom(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("加入房间请求: sessionId={}, roomNumber={}", sessionId, msg.getRoomNumber());
        try {
            GameRoom room = roomService.joinRoom(msg.getRoomNumber(), sessionId, msg.getPreferredRole());
            if (room != null) {
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-created", room);
                Player self = room.getPlayers().get(sessionId);
                if (self != null) {
                    sendPlayerInfo(sessionId, self.getPlayerId());
                }
            }
        } catch (Exception e) {
            log.error("加入房间失败: sessionId={}, roomNumber={}, error={}", sessionId, msg.getRoomNumber(), e.getMessage());
            sendErrorToUser(sessionId, e.getMessage());
        }
    }

    @MessageMapping("/rejoin-with-id")
    public void rejoinWithId(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String newSessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || msg.getPlayerId() == null || newSessionId == null) {
            log.error("无效的重连请求: {}", msg);
            if (newSessionId != null) sendErrorToUser(newSessionId, "无效的重连请求");
            return;
        }
        try {
            roomService.rejoinWithId(msg.getRoomNumber(), msg.getPlayerId(), newSessionId, msg.getPreferredRole());
        } catch (Exception e) {
            log.error("重连失败: playerId={}, roomNumber={}, error={}", msg.getPlayerId(), msg.getRoomNumber(), e.getMessage());
            sendErrorToUser(newSessionId, "重连失败: " + e.getMessage());
        }
    }

    @MessageMapping("/update-preferred-role")
    public void updatePreferredRole(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("[更新首选身份] 无效请求: {}", msg);
            if (sessionId != null) {
                sendErrorToUser(sessionId, "无效的首选身份请求");
            }
            return;
        }
        try {
            roomService.updatePreferredRole(msg.getRoomNumber(), sessionId, msg.getPlayerId(), msg.getPreferredRole());
        } catch (Exception e) {
//            log.error("[更新首选身份] 失败: sessionId={}, roomNumber={}, error={}", sessionId, msg.getRoomNumber(), e.getMessage());
//            sendErrorToUser(sessionId, "更新首选身份失败: " + e.getMessage());
        }
    }

    @MessageMapping("/select-seat")
    public void selectSeat(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        try {
            roomService.selectSeat(msg.getRoomNumber(), sessionId, msg.getSeatNumber());
        } catch (Exception e) {
            log.error("选择座位失败: sessionId={}, seatNumber={}, error={}", sessionId, msg.getSeatNumber(), e.getMessage());
            sendErrorToUser(sessionId, e.getMessage());
        }
    }

    @MessageMapping("/ready")
    public void setReady(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("无效的准备请求: {}", msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求");
            return;
        }
        try {
            roomService.setReady(msg.getRoomNumber(), sessionId, msg.isReady());
        } catch (Exception e) {
            log.error("设置准备状态失败: sessionId={}, error={}", sessionId, e.getMessage());
            sendErrorToUser(sessionId, "设置准备状态失败: " + e.getMessage());
        }
    }

    @MessageMapping("/return-to-room")
    public void returnToRoom(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("无效的返回房间请求: {}", msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求");
            return;
        }
        try {
            roomService.handleReturnToRoom(msg.getRoomNumber(), sessionId);
        } catch (Exception e) {
            log.error("重置房间失败: roomNumber={}, error={}", msg.getRoomNumber(), e.getMessage());
            sendErrorToUser(sessionId, "重置房间失败: " + e.getMessage());
        }
    }

    @MessageMapping("/kick-player")
    public void kickPlayer(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || msg.getTargetPlayerId() == null || sessionId == null) {
            log.error("无效的踢人请求: {}", msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效的踢人请求");
            return;
        }
        try {
            roomService.kickPlayer(msg.getRoomNumber(), sessionId, msg.getTargetPlayerId());
        } catch (Exception e) {
            log.error("踢人失败: error={}", e.getMessage());
            sendErrorToUser(sessionId, "踢人失败: " + e.getMessage());
        }
    }

    @MessageMapping("/transfer-host")
    public void transferHost(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || msg.getTargetPlayerId() == null || sessionId == null) {
            log.error("无效的转让请求: {}", msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效的转让请求");
            return;
        }
        try {
            roomService.transferHost(msg.getRoomNumber(), sessionId, msg.getTargetPlayerId());
        } catch (Exception e) {
            log.error("转让房主失败: error={}", e.getMessage());
            sendErrorToUser(sessionId, "转让房主失败: " + e.getMessage());
        }
    }

    @MessageMapping("/start-mission")
    public void startMission(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("[Controller] 收到 /app/start-mission 请求: sessionId={}, msg={}", sessionId, msg);
        
        if (msg == null) {
            log.error("消息体为空: sessionId={}", sessionId);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求：消息体为空");
            return;
        }
        
        if (msg.getRoomNumber() == null) {
            log.error("房间号为空: sessionId={}, msg={}", sessionId, msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求：房间号为空");
            return;
        }
        
        if (sessionId == null) {
            log.error("Session ID 为空: msg={}", msg);
            return;
        }
        
        try {
            log.info("开始处理任务请求: 房间号={}, sessionId={}", msg.getRoomNumber(), sessionId);
            GameRoom room = roomService.getRoomByKey(RoomService.ROOM_KEY_PREFIX + msg.getRoomNumber());
            if (room == null) {
                log.error("房间不存在: 房间号={}", msg.getRoomNumber());
                throw new RuntimeException("房间不存在");
            }
            log.info("找到房间: 房间号={}, 游戏已开始={}, 当前轮次={}", room.getRoomNumber(), room.isGameStarted(), room.getCurrentMissionRound());
            
            if (!room.isGameStarted()) {
                log.warn("游戏尚未开始，无法开始任务: 房间号={}", room.getRoomNumber());
                throw new RuntimeException("游戏尚未开始，无法开始任务");
            }
            
            // 如果任务还没开始，或者上一轮任务已完成，开始新任务
            boolean needStartNewMission = room.getCurrentMissionRound() == 0 || 
                (room.isMissionVotingCompleted() && room.getMissionResult() != null);
            
            if (needStartNewMission) {
                log.info("检测到需要开始新任务: 当前轮次={}, 投票完成={}, 任务结果={}", 
                        room.getCurrentMissionRound(), room.isMissionVotingCompleted(), room.getMissionResult());
                missionService.startMission(room);
                log.info("任务已开始: 房间号={}, 当前轮次={}, 任务成员数={}", 
                        room.getRoomNumber(), room.getCurrentMissionRound(), 
                        room.getSelectedMissionMembers() != null ? room.getSelectedMissionMembers().size() : 0);
            }
            
            // 将当前玩家加入任务
            missionService.joinMission(room, sessionId);
            log.info("玩家已加入任务: 房间号={}, sessionId={}, 当前任务成员数={}", 
                    room.getRoomNumber(), sessionId, room.getSelectedMissionMembers().size());
            
            roomService.saveRoom(room);
            roomService.broadcastRoomState(msg.getRoomNumber());
            log.info("房间状态已广播: 房间号={}", msg.getRoomNumber());
        } catch (Exception e) {
            log.error("开始任务失败: sessionId={}, 房间号={}, error={}", sessionId, msg.getRoomNumber(), e.getMessage(), e);
            sendErrorToUser(sessionId, "开始任务失败: " + e.getMessage());
        }
    }

    @MessageMapping("/leave-mission")
    public void leaveMission(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (msg == null || msg.getRoomNumber() == null || sessionId == null) {
            log.error("无效的退出任务请求: {}", msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求");
            return;
        }
        try {
            GameRoom room = roomService.getRoomByKey(RoomService.ROOM_KEY_PREFIX + msg.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("房间不存在");
            }
            missionService.leaveMission(room, sessionId);
            roomService.saveRoom(room);
            roomService.broadcastRoomState(msg.getRoomNumber());
        } catch (Exception e) {
            log.error("退出任务失败: error={}", e.getMessage());
            sendErrorToUser(sessionId, "退出任务失败: " + e.getMessage());
        }
    }

    @MessageMapping("/toggle-mission-member")
    public void toggleMissionMember(@Payload MissionRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (request == null || request.getRoomNumber() == null || sessionId == null) {
            log.error("无效的上车/下车请求: {}", request);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求");
            return;
        }
        try {
            GameRoom room = roomService.getRoomByKey(RoomService.ROOM_KEY_PREFIX + request.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("房间不存在");
            }
            missionService.toggleMissionMember(room, sessionId, request.isJoinMission());
            roomService.saveRoom(room);
            roomService.broadcastRoomState(request.getRoomNumber());
        } catch (Exception e) {
            log.error("上车/下车失败: error={}", e.getMessage());
            sendErrorToUser(sessionId, "操作失败: " + e.getMessage());
        }
    }

    @MessageMapping("/vote")
    public void vote(@Payload VoteRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (request == null || request.getRoomNumber() == null || request.getVote() == null || sessionId == null) {
            log.error("无效的投票请求: {}", request);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求");
            return;
        }
        try {
            GameRoom room = roomService.getRoomByKey(RoomService.ROOM_KEY_PREFIX + request.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("房间不存在");
            }
            missionService.vote(room, sessionId, request.getVote());
            roomService.saveRoom(room);
            roomService.broadcastRoomState(request.getRoomNumber());
        } catch (Exception e) {
            log.error("投票失败: error={}", e.getMessage());
            sendErrorToUser(sessionId, "投票失败: " + e.getMessage());
        }
    }

    @MessageMapping("/lake-lady-inspect")
    public void lakeLadyInspect(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("[Controller] 收到 /app/lake-lady-inspect 请求: sessionId={}, msg={}", sessionId, msg);
        if (msg == null || msg.getRoomNumber() == null || msg.getSeatNumber() <= 0 || sessionId == null) {
            log.error("无效的湖中仙女查验请求: {}", msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求");
            return;
        }
        try {
            GameRoom room = roomService.getRoomByKey(RoomService.ROOM_KEY_PREFIX + msg.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("房间不存在");
            }
            missionService.inspectPlayerWithLakeLady(room, sessionId, msg.getSeatNumber());
            roomService.saveRoom(room);
            roomService.broadcastRoomState(msg.getRoomNumber());
        } catch (Exception e) {
            log.error("湖中仙女查验失败: error={}", e.getMessage());
            sendErrorToUser(sessionId, "查验失败: " + e.getMessage());
        }
    }

    @MessageMapping("/lake-lady-publish")
    public void lakeLadyPublish(@Payload ClientMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("[Controller] 收到 /app/lake-lady-publish 请求: sessionId={}, msg={}", sessionId, msg);
        if (msg == null || msg.getRoomNumber() == null || msg.getSeatNumber() <= 0 || sessionId == null) {
            log.error("无效的湖中仙女公开请求: {}", msg);
            if (sessionId != null) sendErrorToUser(sessionId, "无效请求");
            return;
        }
        try {
            GameRoom room = roomService.getRoomByKey(RoomService.ROOM_KEY_PREFIX + msg.getRoomNumber());
            if (room == null) {
                throw new RuntimeException("房间不存在");
            }
            // 从 targetPlayerId 字段获取公开结果（"good"、"evil" 或 "unknown"）
            String publicResult = msg.getTargetPlayerId();
            if (publicResult == null || (!"good".equals(publicResult) && !"evil".equals(publicResult) && !"unknown".equals(publicResult))) {
                throw new RuntimeException("无效的公开结果");
            }
            missionService.publishLakeLadyResult(room, sessionId, msg.getSeatNumber(), publicResult);
            roomService.saveRoom(room);
            roomService.broadcastRoomState(msg.getRoomNumber());
        } catch (Exception e) {
            log.error("湖中仙女公开失败: error={}", e.getMessage());
            sendErrorToUser(sessionId, "公开失败: " + e.getMessage());
        }
    }

    @EventListener
    public void onDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            log.warn("断开连接的 sessionId 为 null");
            return;
        }

        try {
            String roomNumber = stringRedisTemplate.opsForValue().get("avalon:session:" + sessionId);
            if (roomNumber != null) {
                roomService.removePlayer(roomNumber, sessionId);
            }
        } catch (Exception e) {
            log.error("处理断开连接事件失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    private void sendErrorToUser(String sessionId, String errorMessage) {
        if (sessionId == null || errorMessage == null) return;
        try {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", Map.of("error", errorMessage));
        } catch (Exception e) {
            log.error("发送错误消息失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    private void sendPlayerInfo(String sessionId, String playerId) {
        if (sessionId == null || playerId == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/player-info", Map.of("playerId", playerId));
        } catch (Exception e) {
            log.error("发送玩家信息失败: sessionId={}, playerId={}, error={}", sessionId, playerId, e.getMessage());
        }
    }
}
