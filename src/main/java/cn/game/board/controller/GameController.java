package cn.game.board.controller;

import cn.game.board.model.avalon.dto.IdentityRequest;
import cn.game.board.model.avalon.dto.IdentityResponse;
import cn.game.board.model.avalon.dto.SpectatorRoomView;
import cn.game.board.service.GameService;
import cn.game.board.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 游戏 REST API 控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final RoomService roomService;

    @PostMapping("/get-identity")
    public ResponseEntity<IdentityResponse> getIdentity(@RequestBody IdentityRequest request) {
        if (request.getPlayersNumber() < 5 || request.getPlayersNumber() > 10 ||
                request.getGameNumber() <= 0 ||
                request.getSeatNumber() < 1 || request.getSeatNumber() > request.getPlayersNumber()) {
            return ResponseEntity.badRequest().build();
        }

        IdentityResponse response = gameService.getIdentity(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取房间的观战视图
     * @param roomNumber 房间号
     * @return 观战视图数据
     */
    @GetMapping("/rooms/{roomNumber}/spectator")
    public ResponseEntity<SpectatorRoomView> getSpectatorView(@PathVariable String roomNumber) {
        try {
            SpectatorRoomView spectatorView = roomService.buildSpectatorView(roomNumber);
            if (spectatorView == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(spectatorView);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
