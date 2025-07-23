package com.edsonmoreira.dslist.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import org.mockito.*;

class GameServiceTests {

    @InjectMocks
    private GameService service;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameListRepository gameListRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnGameDTOWhenFound() {
        Game game = new Game(1L, "Title", 2000, "Genre", "Platform", 9.5, "img.jpg", "short", "long");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameDTO dto = service.findById(1L);

        assertEquals(game.getTitle(), dto.getTitle());
        verify(gameRepository).findById(1L);
    }

    @Test
    void shouldReturnListOfGameMinDTO() {
        List<Game> games = Arrays.asList(
                new Game(1L, "A", 2000, "G", "P", 9.0, null, null, null),
                new Game(2L, "B", 2001, "G2", "P2", 8.0, null, null, null)
        );
        when(gameRepository.findAll()).thenReturn(games);

        List<GameMinDTO> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getTitle());
        verify(gameRepository).findAll();
    }

    @Test
    void shouldReturnGameMinDTOListWhenSearchingByList() {
        GameMinProjection p1 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(1L);
        when(p1.getTitle()).thenReturn("X");
        List<GameMinProjection> projList = List.of(p1);
        when(gameRepository.searchByList(10L)).thenReturn(projList);

        List<GameMinDTO> result = service.findByList(10L);

        assertEquals(1, result.size());
        assertEquals("X", result.get(0).getTitle());
        verify(gameRepository).searchByList(10L);
    }

    @Test
    void shouldCreateGameInList() {
        Game toSave = new Game(null, "New", 2022, "G", "P", 7.0, null, null, null);
        Game saved = new Game(5L, "New", 2022, "G", "P", 7.0, null, null, null);
        when(gameRepository.searchByList(2L)).thenReturn(List.of());
        when(gameRepository.save(toSave)).thenReturn(saved);

        Game result = service.createGameInAList(2L, toSave);

        assertEquals(5L, result.getId());
        verify(gameRepository).save(toSave);
        verify(gameListRepository).insertBelonging(2L, 5L, 1);
    }

    @Test
    void shouldUpdateGameWhenExists() {
        Game existing = new Game(3L, "Old", 2000, "G", "P", 5.0, null, null, null);
        Game updates = new Game(null, "Updated", null, null, null, null, null, null, null);
        when(gameRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.updateGame(3L, updates);

        assertEquals("Updated", existing.getTitle());
        verify(gameRepository).save(existing);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingGame() {
        when(gameRepository.findById(4L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateGame(4L, new Game()));
        assertTrue(ex.getMessage().contains("Game not found"));
    }

    @Test
    void shouldDeleteGameAndUpdatePositions() {
        GameMinProjection p1 = mock(GameMinProjection.class);
        GameMinProjection p2 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(10L);
        when(p2.getId()).thenReturn(20L);
        List<GameMinProjection> list = Arrays.asList(p1, p2);

        when(gameRepository.searchListIdByGameId(20L)).thenReturn(7L);
        when(gameRepository.searchByList(7L)).thenReturn(list);

        service.deleteGame(20L);

        verify(gameRepository).removeGameFromList(20L, 7L);
        // a posição removida é 1, logo updateGamePositions(7,1)
        verify(gameRepository).updateGamePositions(7L, 1);
    }

    @Test
    void shouldThrowWhenDeleteWithNoList() {
        when(gameRepository.searchListIdByGameId(99L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.deleteGame(99L));
        assertTrue(ex.getMessage().contains("Game not found in any list"));
    }
}
