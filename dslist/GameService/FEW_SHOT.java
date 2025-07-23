package com.edsonmoreira.dslist.services;

import com.edsonmoreira.dslist.dto.GameDTO;
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

import java.util.List;
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

    @Test
    void testFindById_returnsGameDTO() {
        Game game = new Game();
        game.setId(1L);
        game.setTitle("Test Game");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameDTO dto = service.findById(1L);

        assertEquals(1L, dto.getId());
        assertEquals("Test Game", dto.getTitle());
    }

    @Test
    void testFindAll_returnsListOfMinDTO() {
        Game g1 = new Game(); g1.setId(1L);
        Game g2 = new Game(); g2.setId(2L);
        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));

        List<GameMinDTO> list = service.findAll();

        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(1L)));
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(2L)));
    }

    @Test
    void testFindByList_returnsListOfMinDTO() {
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
    void testCreateGameInAList_savesAndInserts() {
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
}