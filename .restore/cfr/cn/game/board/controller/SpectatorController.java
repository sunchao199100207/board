/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.controller.SpectatorController
 *  cn.game.board.model.avalon.dto.SpectatorRoomView
 *  cn.game.board.service.RoomService
 *  lombok.Generated
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package cn.game.board.controller;

import cn.game.board.model.avalon.dto.SpectatorRoomView;
import cn.game.board.service.RoomService;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api"})
public class SpectatorController {
    private final RoomService roomService;

    @GetMapping(value={"/rooms/{roomNumber}/spectator"})
    public ResponseEntity<SpectatorRoomView> getSpectatorRoom(@PathVariable String roomNumber) {
        try {
            SpectatorRoomView view = this.roomService.buildSpectatorView(roomNumber);
            return ResponseEntity.ok((Object)view);
        }
        catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Generated
    public SpectatorController(RoomService roomService) {
        this.roomService = roomService;
    }
}

