package com.edsonmoreira.dslist.services;

import com.edsonmoreira.dslist.dto.GameMinDTO;
import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameListRepository gameListRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void findByIdShouldReturnGameDTO() {
        Game game = new Game();
        game.setId(1L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        var dto = gameService.findById(1L);

        assertEquals(1L, dto.getId());
        verify(gameRepository).findById(1L);
    }

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(gameRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> gameService.findById(2L));
    }

    @Test
    void findAllShouldReturnEmptyList() {
        when(gameRepository.findAll()).thenReturn(Collections.emptyList());
        var result = gameService.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void findAllShouldReturnList() {
        Game g1 = new Game(); g1.setId(1L);
        Game g2 = new Game(); g2.setId(2L);
        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));

        var result = gameService.findAll();
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void findByListShouldReturnEmpty() {
        when(gameRepository.searchByList(5L)).thenReturn(Collections.emptyList());
        var result = gameService.findByList(5L);
        assertTrue(result.isEmpty());
    }

//    @Test
//    void findByListShouldReturnData() {
//        GameMinProjection p = new GameMinProjection() {
//            public Long getId() { return 7L; }
//            public String getTitle() { return "X"; }
//            public Integer getYear() { return 2020; }
//            public String getImgUrl() { return ""; }
//            public String getShortDescription() { return ""; }
//        };
//        when(gameRepository.searchByList(3L)).thenReturn(List.of(p));
//
//        var result = gameService.findByList(3L);
//        assertEquals(1, result.size());
//        assertEquals(7L, result.get(0).getId());
//    }

    @Test
    void createGameInAListShouldInsert() {
        long listId = 9L;
        Game game = new Game();
        game.setId(null);
        when(gameRepository.searchByList(listId)).thenReturn(List.of());
        Game saved = new Game();
        saved.setId(100L);
        when(gameRepository.save(game)).thenReturn(saved);

        var result = gameService.createGameInAList(listId, game);
        assertEquals(100L, result.getId());
        verify(gameListRepository).insertBelonging(listId, 100L, 1);
    }

    @Test
    void updateGameShouldSave() {
        long id = 11L;
        Game existing = new Game();
        existing.setId(id);
        when(gameRepository.findById(id)).thenReturn(Optional.of(existing));
        Game updates = new Game();
        updates.setTitle("new");
        gameService.updateGame(id, updates);
        verify(gameRepository).save(existing);
    }

    @Test
    void updateGameShouldThrowWhenNotFound() {
        when(gameRepository.findById(12L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> gameService.updateGame(12L, new Game()));
    }

    @Test
    void deleteGameShouldThrowWhenNoList() {
        when(gameRepository.searchListIdByGameId(20L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> gameService.deleteGame(20L));
    }

    @Test
    void deleteGameShouldThrowWhenNotInList() {
        when(gameRepository.searchListIdByGameId(21L)).thenReturn(2L);
        GameMinProjection p = mock(GameMinProjection.class);
        when(p.getId()).thenReturn(99L);
        when(gameRepository.searchByList(2L)).thenReturn(List.of(p));
        assertThrows(RuntimeException.class, () -> gameService.deleteGame(21L));
    }

//    @Test
//    void deleteGameShouldRemoveAndUpdatePositions() {
//        long gameId = 30L;
//        long listId = 4L;
//        when(gameRepository.searchListIdByGameId(gameId)).thenReturn(listId);
//        GameMinProjection p1 = mock(GameMinProjection.class);
//        GameMinProjection p2 = mock(GameMinProjection.class);
//        when(p1.getId()).thenReturn(gameId);
//        when(p2.getId()).thenReturn(31L);
//        when(gameRepository.searchByList(listId)).thenReturn(List.of(p1, p2));
//
//        gameService.deleteGame(gameId);
//        verify(gameRepository).removeGameFromList(gameId, listId);
//        verify(gameRepository).updateGamePositions(listId, 0);
//    }
}
