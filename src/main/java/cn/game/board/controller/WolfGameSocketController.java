package cn.game.board.controller;

import cn.game.board.model.werewolf.WolfClientMessage;
import cn.game.board.service.WolfDealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 狼人杀发牌的 WebSocket 入口。只处理房间和发牌，不处理夜间技能。
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WolfGameSocketController {

    private final WolfDealService wolfDealService;

    @MessageMapping("/wolf/enter")
    public void enter(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.enter(msg));
    }

    @MessageMapping("/wolf/claim-judge")
    public void claimJudge(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.claimJudge(msg));
    }

    @MessageMapping("/wolf/config")
    public void config(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.updateConfig(msg));
    }

    @MessageMapping("/wolf/sync")
    public void sync(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.sync(msg));
    }

    @MessageMapping("/wolf/select-seat")
    public void selectSeat(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.selectSeat(msg));
    }

    @MessageMapping("/wolf/ready")
    public void ready(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.setReady(msg));
    }

    @MessageMapping("/wolf/deal")
    public void deal(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.deal(msg));
    }

    @MessageMapping("/wolf/new-round")
    public void newRound(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.newRound(msg));
    }

    @MessageMapping("/wolf/leave")
    public void leave(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.leave(msg));
    }

    @MessageMapping("/wolf/close")
    public void close(@Payload WolfClientMessage msg) {
        guard(msg, () -> wolfDealService.close(msg));
    }

    private void guard(WolfClientMessage msg, Runnable action) {
        String playerId = msg == null ? null : msg.getPlayerId();
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            wolfDealService.sendError(playerId, e.getMessage());
        } catch (Exception e) {
            log.error("狼人发牌操作失败: playerId={}", playerId, e);
            wolfDealService.sendError(playerId, "操作失败");
        }
    }
}
