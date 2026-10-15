package cn.game.board.controller;

import cn.game.board.model.dual.DualClientMessage;
import cn.game.board.service.DualDealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 双身份狼人杀发牌入口。
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DualGameSocketController {

    private final DualDealService dualDealService;

    @MessageMapping("/dual/enter")
    public void enter(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.enter(msg));
    }

    @MessageMapping("/dual/claim-judge")
    public void claimJudge(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.claimJudge(msg));
    }

    @MessageMapping("/dual/config")
    public void config(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.updateConfig(msg));
    }

    @MessageMapping("/dual/select-seat")
    public void selectSeat(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.selectSeat(msg));
    }

    @MessageMapping("/dual/ready")
    public void ready(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.setReady(msg));
    }

    @MessageMapping("/dual/deal")
    public void deal(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.deal(msg));
    }

    @MessageMapping("/dual/choose")
    public void choose(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.choose(msg));
    }

    @MessageMapping("/dual/new-round")
    public void newRound(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.newRound(msg));
    }

    @MessageMapping("/dual/leave")
    public void leave(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.leave(msg));
    }

    @MessageMapping("/dual/close")
    public void close(@Payload DualClientMessage msg) {
        guard(msg, () -> dualDealService.close(msg));
    }

    private void guard(DualClientMessage msg, Runnable action) {
        String playerId = msg == null ? null : msg.getPlayerId();
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            dualDealService.sendError(playerId, e.getMessage());
        } catch (Exception e) {
            log.error("双身份发牌操作失败: playerId={}", playerId, e);
            dualDealService.sendError(playerId, "操作失败");
        }
    }
}
