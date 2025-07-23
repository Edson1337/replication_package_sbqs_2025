package com.edsonmoreira.dslist.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;
import com.edsonmoreira.dslist.services.GameService;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameListRepository gameListRepository;

    @InjectMocks
    private GameService service;

    @Test
    void findById_ShouldReturnGameDTO_WhenGameExists() {
        Game game = new Game();
        game.setId(1L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        var dto = service.findById(1L);

        assertEquals(1L, dto.getId());
        verify(gameRepository).findById(1L);
    }

    @Test
    void findAll_ShouldReturnListOfGameMinDTO() {
        Game g1 = new Game(); g1.setId(1L);
        Game g2 = new Game(); g2.setId(2L);
        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));

        var list = service.findAll();

        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(1L)));
        verify(gameRepository).findAll();
    }

    @Test
    void findByList_ShouldReturnListOfGameMinDTO() {
        GameMinProjection p = () -> 5L;
        when(gameRepository.searchByList(10L)).thenReturn(List.of(p));

        var list = service.findByList(10L);

        assertEquals(1, list.size());
        assertEquals(5L, list.get(0).getId());
        verify(gameRepository).searchByList(10L);
    }

    @Test
    void createGameInAList_ShouldSaveGameAndInsertBelonging() {
        Game toSave = new Game();
        Game saved = new Game(); saved.setId(7L);
        when(gameRepository.searchByList(2L)).thenReturn(Collections.emptyList());
        when(gameRepository.save(toSave)).thenReturn(saved);

        var result = service.createGameInAList(2L, toSave);

        assertSame(saved, result);
        verify(gameRepository).save(toSave);
        verify(gameListRepository).insertBelonging(2L, 7L, 1);
    }

    @Test
    void updateGame_ShouldCopyPropertiesAndSave_WhenExists() {
        Game existing = new Game(); existing.setId(3L); existing.setTitle("Old");
        Game updates  = new Game(); updates.setTitle("New");
        when(gameRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.updateGame(3L, updates);

        assertEquals("New", existing.getTitle());
        verify(gameRepository).save(existing);
    }

    @Test
    void updateGame_ShouldThrow_WhenNotFound() {
        when(gameRepository.findById(4L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateGame(4L, new Game()));
    }

    @Test
    void deleteGame_ShouldRemoveAndReorder_WhenFound() {
        Long gameId = 9L, listId = 20L;
        GameMinProjection p1 = () -> 8L;
        GameMinProjection p2 = () -> 9L;
        when(gameRepository.searchListIdByGameId(gameId)).thenReturn(listId);
        when(gameRepository.searchByList(listId)).thenReturn(List.of(p1, p2));

        service.deleteGame(gameId);

        verify(gameRepository).removeGameFromList(gameId, listId);
        verify(gameRepository).updateGamePositions(listId, 1);
    }

    @Test
    void deleteGame_ShouldThrow_WhenNotInAnyList() {
        when(gameRepository.searchListIdByGameId(5L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.deleteGame(5L));
    }

    @Test
    void deleteGame_ShouldThrow_WhenNotFoundInList() {
        when(gameRepository.searchListIdByGameId(6L)).thenReturn(30L);
        when(gameRepository.searchByList(30L)).thenReturn(Collections.emptyList());
        assertThrows(RuntimeException.class, () -> service.deleteGame(6L));
    }
}
