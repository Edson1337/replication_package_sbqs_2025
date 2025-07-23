package com.edsonmoreira.dslist.services;

import com.edsonmoreira.dslist.dto.GameDTO;
import com.edsonmoreira.dslist.dto.GameMinDTO;
import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameListRepository gameListRepository;

    @InjectMocks
    private GameService service;

    private Game existingGame;

    @BeforeEach
    void setUp() {
        existingGame = new Game();
        existingGame.setId(1L);
        existingGame.setTitle("Original");
    }

    @Test
    void testFindById_success() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame));
        GameDTO dto = service.findById(1L);
        assertEquals(1L, dto.getId());
        assertEquals("Original", dto.getTitle());
    }

    @Test
    void testFindById_notFound_throws() {
        when(gameRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.findById(2L));
    }

    @Test
    void testFindAll_returnsList() {
        Game g1 = new Game(); g1.setId(10L);
        Game g2 = new Game(); g2.setId(20L);
        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));
        List<GameMinDTO> list = service.findAll();
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(10L)));
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(20L)));
    }

    @Test
    void testFindAll_emptyList() {
        when(gameRepository.findAll()).thenReturn(List.of());
        List<GameMinDTO> list = service.findAll();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void testFindByList_returnsList() {
        GameMinProjection p1 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(5L);
        GameMinProjection p2 = mock(GameMinProjection.class);
        when(p2.getId()).thenReturn(6L);
        when(gameRepository.searchByList(99L)).thenReturn(List.of(p1, p2));

        List<GameMinDTO> list = service.findByList(99L);
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(5L)));
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(6L)));
    }

    @Test
    void testFindByList_emptyList() {
        when(gameRepository.searchByList(100L)).thenReturn(List.of());
        List<GameMinDTO> list = service.findByList(100L);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void testCreateGameInAList_success() {
        Game toSave = new Game();
        toSave.setTitle("Novo");
        Game saved = new Game();
        saved.setId(10L);

        when(gameRepository.searchByList(3L)).thenReturn(List.of());
        when(gameRepository.save(toSave)).thenReturn(saved);

        Game result = service.createGameInAList(3L, toSave);
        assertEquals(10L, result.getId());
        verify(gameListRepository).insertBelonging(3L, 10L, 1);
    }

    @Test
    void testUpdateGame_success() {
        Game updates = new Game();
        updates.setTitle("Atualizado");

        when(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame));
        service.updateGame(1L, updates);

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        assertEquals("Atualizado", captor.getValue().getTitle());
    }

    @Test
    void testUpdateGame_notFound_throws() {
        when(gameRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateGame(5L, new Game()));
    }

    @Test
    void testDeleteGame_success() {
        long listId = 2L;
        long gameId = 7L;
        GameMinProjection p1 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(gameId);
        when(gameRepository.searchListIdByGameId(gameId)).thenReturn(listId);
        when(gameRepository.searchByList(listId)).thenReturn(List.of(p1));

        service.deleteGame(gameId);

        verify(gameRepository).removeGameFromList(gameId, listId);
        verify(gameRepository).updateGamePositions(listId, 0);
    }

    @Test
    void testDeleteGame_gameNotInAnyList_throws() {
        when(gameRepository.searchListIdByGameId(8L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.deleteGame(8L));
    }

    @Test
    void testDeleteGame_gameNotInList_throws() {
        long listId = 3L;
        when(gameRepository.searchListIdByGameId(9L)).thenReturn(listId);
        GameMinProjection p = mock(GameMinProjection.class);
        when(p.getId()).thenReturn(5L);
        when(gameRepository.searchByList(listId)).thenReturn(List.of(p));
        assertThrows(RuntimeException.class, () -> service.deleteGame(9L));
    }
}
