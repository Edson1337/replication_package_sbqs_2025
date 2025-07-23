package com.edsonmoreira.dslist.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.edsonmoreira.dslist.dto.GameDTO;
import com.edsonmoreira.dslist.dto.GameMinDTO;
import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameListRepository gameListRepository;

    @InjectMocks
    private GameService service;

    private Game game;
    private GameMinProjection proj;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        game = new Game();
        game.setId(1L);
        game.setTitle("Test Game");
        proj = mock(GameMinProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getTitle()).thenReturn("Test Game");
    }

    @Test
    void shouldReturnGameDTOWhenFindById() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        GameDTO dto = service.findById(1L);
        assertEquals(1L, dto.getId());
        assertEquals("Test Game", dto.getTitle());
    }

    @Test
    void shouldThrowWhenFindByIdNotFound() {
        when(gameRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(2L));
    }

    @Test
    void shouldReturnAllGameMinDTO() {
        when(gameRepository.findAll()).thenReturn(List.of(game));
        List<GameMinDTO> list = service.findAll();
        assertEquals(1, list.size());
        assertEquals("Test Game", list.get(0).getTitle());
    }

    @Test
    void shouldReturnEmptyListWhenFindByListNoGames() {
        when(gameRepository.searchByList(10L)).thenReturn(List.of());
        List<GameMinDTO> list = service.findByList(10L);
        assertTrue(list.isEmpty());
    }

    @Test
    void shouldReturnGameMinDTOListWhenFindByList() {
        when(gameRepository.searchByList(1L)).thenReturn(List.of(proj));
        var list = service.findByList(1L);
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getId());
    }

    @Test
    void shouldCreateGameInAList() {
        when(gameRepository.searchByList(1L)).thenReturn(List.of(proj));
        when(gameRepository.save(game)).thenReturn(game);
        service.createGameInAList(1L, game);
        verify(gameRepository).save(game);
        verify(gameListRepository).insertBelonging(1L, 1L, 2);
    }

    @Test
    void shouldUpdateGameProperties() {
        Game updated = new Game();
        updated.setTitle("New Title");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        service.updateGame(1L, updated);
        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        assertEquals("New Title", captor.getValue().getTitle());
    }

    @Test
    void shouldThrowOnUpdateWhenGameNotFound() {
        when(gameRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateGame(5L, new Game()));
    }

    @Test
    void shouldDeleteGameAndUpdatePositions() {
        when(gameRepository.searchListIdByGameId(1L)).thenReturn(1L);
        when(gameRepository.searchByList(1L)).thenReturn(List.of(proj, proj));
        service.deleteGame(1L);
        verify(gameRepository).removeGameFromList(1L, 1L);
        verify(gameRepository).updateGamePositions(1L, 0);
    }

    @Test
    void shouldThrowOnDeleteWhenGameNotInAnyList() {
        when(gameRepository.searchListIdByGameId(2L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.deleteGame(2L));
    }
}
