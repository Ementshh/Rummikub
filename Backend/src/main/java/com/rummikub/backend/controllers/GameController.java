package com.rummikub.backend.controllers;

import com.rummikub.backend.models.Game;
import com.rummikub.backend.models.GameParticipant;
import com.rummikub.backend.services.GameService;
import com.rummikub.backend.services.RummikubLogicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    private UUID getCurrentUserId() {
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(principal);
    }

    @PostMapping
    public ResponseEntity<?> createGame() {
        try {
            Game game = gameService.createGame();
            return ResponseEntity.status(201).body(Map.of("success", true, "data", game));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinGame(@PathVariable String id) {
        try {
            GameParticipant participant = gameService.joinGame(id, getCurrentUserId());
            return ResponseEntity.ok(Map.of("success", true, "data", participant));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startGame(@PathVariable String id) {
        try {
            Map<String, Object> result = gameService.startGame(id);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    public static class EndTurnRequest {
        public List<RummikubLogicService.TableSetRequest> table_sets;
        public List<Integer> rack_tiles;
    }

    @PostMapping("/{id}/end-turn")
    public ResponseEntity<?> endTurn(@PathVariable String id, @RequestBody EndTurnRequest request) {
        try {
            if (request.table_sets == null || request.rack_tiles == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Format body salah. table_sets dan rack_tiles harus berupa array."));
            }
            Map<String, Object> result = gameService.executeEndTurn(id, getCurrentUserId(), request.table_sets, request.rack_tiles);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/draw")
    public ResponseEntity<?> drawTile(@PathVariable String id) {
        try {
            Map<String, Object> result = gameService.drawTilePenalty(id, getCurrentUserId());
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/state")
    public ResponseEntity<?> getGameState(@PathVariable String id) {
        try {
            Map<String, Object> result = gameService.getGameState(id, getCurrentUserId());
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
