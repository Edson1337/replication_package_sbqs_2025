package com.edsonmoreira.dslist.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.edsonmoreira.dslist.dto.GameDTO;
import com.edsonmoreira.dslist.dto.GameMinDTO;
import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameListRepository gameListRepository;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    void setUp() {
        // Mocks are initialized by MockitoExtension
    }

    @Test
    void shouldReturnGameDTOWhenIdExists() {
        Game game = new Game();
        game.setId(1L);
        game.setTitle("Game 1");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameDTO dto = gameService.findById(1L);

        assertEquals(1L, dto.getId());
        assertEquals("Game 1", dto.getTitle());
    }

    @Test
    void shouldThrowExceptionWhenIdNotFound() {
        when(gameRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> gameService.findById(1L));
    }

    @Test
    void shouldReturnListOfGameMinDTO() {
        Game g1 = new Game(); g1.setId(1L); g1.setTitle("Game A");
        Game g2 = new Game(); g2.setId(2L); g2.setTitle("Game B");
        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));

        List<GameMinDTO> result = gameService.findAll();

        assertEquals(2, result.size());
        assertEquals("Game A", result.get(0).getTitle());
        assertEquals("Game B", result.get(1).getTitle());
    }

    @Test
    void shouldReturnGameMinDTObyList() {
        GameMinProjection proj = mock(GameMinProjection.class);
        when(proj.getId()).thenReturn(5L);
        when(proj.getTitle()).thenReturn("List Game");
        when(gameRepository.searchByList(2L)).thenReturn(List.of(proj));

        List<GameMinDTO> list = gameService.findByList(2L);

        assertEquals(1, list.size());
        assertEquals(5L, list.get(0).getId());
        assertEquals("List Game", list.get(0).getTitle());
    }

    @Test
    void shouldCreateGameInList() {
        Game newGame = new Game();
        newGame.setTitle("New");
        Game saved = new Game();
        saved.setId(10L);
        when(gameRepository.searchByList(3L)).thenReturn(Collections.emptyList());
        when(gameRepository.save(newGame)).thenReturn(saved);

        Game result = gameService.createGameInAList(3L, newGame);

        assertEquals(10L, result.getId());
        verify(gameListRepository).insertBelonging(3L, 10L, 1);
    }

    @Test
    void shouldUpdateExistingGame() {
        Game existing = new Game();
        existing.setId(7L);
        existing.setTitle("Old");
        Game updates = new Game();
        updates.setTitle("New");
        when(gameRepository.findById(7L)).thenReturn(Optional.of(existing));

        gameService.updateGame(7L, updates);

        assertEquals("New", existing.getTitle());
        verify(gameRepository).save(existing);
    }

    @Test
    void shouldThrowWhenUpdatingNonexistentGame() {
        when(gameRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> gameService.updateGame(8L, new Game()));
    }

    @Test
    void shouldDeleteGameExistingInList() {
        Long id = 4L;
        Long listId = 1L;
        GameMinProjection p1 = mock(GameMinProjection.class);
        GameMinProjection p2 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(2L);
        when(p2.getId()).thenReturn(id);

        when(gameRepository.searchListIdByGameId(id)).thenReturn(listId);
        when(gameRepository.searchByList(listId)).thenReturn(List.of(p1, p2));

        gameService.deleteGame(id);

        verify(gameRepository).removeGameFromList(id, listId);
        verify(gameRepository).updateGamePositions(listId, 1);
    }

    @Test
    void shouldThrowWhenDeletingNotFound() {
        when(gameRepository.searchListIdByGameId(9L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> gameService.deleteGame(9L));
    }
}