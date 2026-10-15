/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.controller.GameController
 *  cn.game.board.model.avalon.dto.IdentityRequest
 *  cn.game.board.model.avalon.dto.IdentityResponse
 *  cn.game.board.service.GameService
 *  lombok.Generated
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package cn.game.board.controller;

import cn.game.board.model.avalon.dto.IdentityRequest;
import cn.game.board.model.avalon.dto.IdentityResponse;
import cn.game.board.service.GameService;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api"})
public class GameController {
    private final GameService gameService;

    @PostMapping(value={"/get-identity"})
    public ResponseEntity<IdentityResponse> getIdentity(@RequestBody IdentityRequest request) {
        if (request.getPlayersNumber() < 5 || request.getPlayersNumber() > 10 || request.getGameNumber() <= 0L || request.getSeatNumber() < 1 || request.getSeatNumber() > request.getPlayersNumber()) {
            return ResponseEntity.badRequest().build();
        }
        IdentityResponse response = this.gameService.getIdentity(request);
        return ResponseEntity.ok((Object)response);
    }

    @Generated
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }
}

