package com.edsonmoreira.dslist.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.edsonmoreira.dslist.dto.GameDTO;
import com.edsonmoreira.dslist.dto.GameMinDTO;
import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameListRepository gameListRepository;

    @InjectMocks
    private GameService service;

    private Game sampleGame;

    @BeforeEach
    void init() {
        sampleGame = new Game();
        sampleGame.setId(1L);
        sampleGame.setTitle("Test Game");
        sampleGame.setYear(2022);
        sampleGame.setGenre("Action");
        sampleGame.setPlatforms("PC");
        sampleGame.setScore(9.5);
        sampleGame.setImgUrl("img.png");
        sampleGame.setShortDescription("short");
        sampleGame.setLongDescription("long");
    }

    @Test
    @DisplayName("findById deve retornar GameDTO quando existir")
    void findById_Sucesso() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(sampleGame));

        GameDTO dto = service.findById(1L);

        assertEquals(1L, dto.getId());
        assertEquals("Test Game", dto.getTitle());
        verify(gameRepository).findById(1L);
    }

    @Test
    @DisplayName("findById deve lançar NoSuchElementException quando não existir")
    void findById_Falha() {
        when(gameRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.findById(2L));
    }

    @Test
    @DisplayName("findAll deve retornar lista de GameMinDTO")
    void findAll_Sucesso() {
        Game g2 = new Game();
        g2.setId(2L);
        g2.setTitle("G2");
        when(gameRepository.findAll()).thenReturn(Arrays.asList(sampleGame, g2));

        List<GameMinDTO> list = service.findAll();

        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(1L)));
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(2L)));
    }

    @Test
    @DisplayName("findByList deve converter projeção em GameMinDTO")
    void findByList_Sucesso() {
        GameMinProjection proj = mock(GameMinProjection.class);
        when(proj.getId()).thenReturn(5L);
        when(proj.getTitle()).thenReturn("Proj");
        when(proj.getGameYear()).thenReturn(2021);
        when(proj.getImgUrl()).thenReturn("url");
        when(gameRepository.searchByList(10L)).thenReturn(List.of(proj));

        List<GameMinDTO> list = service.findByList(10L);

        assertEquals(1, list.size());
        assertEquals(5L, list.get(0).getId());
        assertEquals("Proj", list.get(0).getTitle());
    }

    @Test
    @DisplayName("createGameInAList deve salvar jogo e inserir em lista")
    void createGameInAList_Sucesso() {
        when(gameRepository.searchByList(1L)).thenReturn(List.of());
        when(gameRepository.save(sampleGame)).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        Game saved = service.createGameInAList(1L, sampleGame);

        assertEquals(100L, saved.getId());
        verify(gameListRepository).insertBelonging(1L, 100L, 1);
    }

    @Test
    @DisplayName("updateGame deve copiar propriedades e salvar quando existir")
    void updateGame_Sucesso() {
        Game updates = new Game();
        updates.setTitle("New Title");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(sampleGame));

        service.updateGame(1L, updates);

        assertEquals("New Title", sampleGame.getTitle());
        verify(gameRepository).save(sampleGame);
    }

    @Test
    @DisplayName("updateGame deve lançar RuntimeException quando não existir")
    void updateGame_Falha() {
        when(gameRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateGame(2L, new Game()));
    }

    @Test
    @DisplayName("deleteGame deve remover jogo e atualizar posições")
    void deleteGame_Sucesso() {
        GameMinProjection p1 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(1L);
        GameMinProjection p2 = mock(GameMinProjection.class);
        when(p2.getId()).thenReturn(2L);

        when(gameRepository.searchListIdByGameId(2L)).thenReturn(9L);
        when(gameRepository.searchByList(9L)).thenReturn(Arrays.asList(p1, p2));

        service.deleteGame(2L);

        verify(gameRepository).removeGameFromList(2L, 9L);
        verify(gameRepository).updateGamePositions(9L, 1);
    }

    @Test
    @DisplayName("deleteGame deve lançar RuntimeException quando não estiver em lista")
    void deleteGame_SemLista() {
        when(gameRepository.searchListIdByGameId(3L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.deleteGame(3L));
    }

    @Test
    @DisplayName("deleteGame deve lançar RuntimeException quando não encontrado na lista")
    void deleteGame_NaoEncontradoNaLista() {
        GameMinProjection p = mock(GameMinProjection.class);
        when(p.getId()).thenReturn(4L);
        when(gameRepository.searchListIdByGameId(5L)).thenReturn(7L);
        when(gameRepository.searchByList(7L)).thenReturn(List.of(p));
        assertThrows(RuntimeException.class, () -> service.deleteGame(5L));
    }
}
