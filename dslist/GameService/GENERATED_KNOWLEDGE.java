package com.edsonmoreira.dslist.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import com.edsonmoreira.dslist.dto.GameDTO;
import com.edsonmoreira.dslist.dto.GameMinDTO;
import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameListRepository gameListRepository;
    @InjectMocks
    private GameService gameService;

    @Test
    void deveRetornarGameDTOQuandoIdExistir() {
        Game game = new Game();
        game.setId(1L);
        game.setTitle("Game 1");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameDTO dto = gameService.findById(1L);

        assertEquals(1L, dto.getId());
        assertEquals("Game 1", dto.getTitle());
    }

    @Test
    void deveLancarExcecaoQuandoIdNaoExistir() {
        when(gameRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> gameService.findById(1L));
    }

    @Test
    void deveRetornarListaGameMinDTO() {
        Game g1 = new Game(); g1.setId(1L); g1.setTitle("Jogo A");
        Game g2 = new Game(); g2.setId(2L); g2.setTitle("Jogo B");
        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));

        List<GameMinDTO> result = gameService.findAll();

        assertEquals(2, result.size());
        assertEquals("Jogo A", result.get(0).getTitle());
        assertEquals("Jogo B", result.get(1).getTitle());
    }

    @Test
    void deveRetornarGameMinDTOPorLista() {
        GameMinProjection proj = mock(GameMinProjection.class);
        when(proj.getId()).thenReturn(5L);
        when(proj.getTitle()).thenReturn("Lista Game");
        when(gameRepository.searchByList(2L)).thenReturn(List.of(proj));

        List<GameMinDTO> list = gameService.findByList(2L);

        assertEquals(1, list.size());
        assertEquals(5L, list.get(0).getId());
        assertEquals("Lista Game", list.get(0).getTitle());
    }

    @Test
    void deveCriarGameEmLista() {
        Game novo = new Game(); novo.setTitle("Novo");
        Game salvo = new Game(); salvo.setId(10L);
        when(gameRepository.searchByList(3L)).thenReturn(List.of());
        when(gameRepository.save(novo)).thenReturn(salvo);

        Game result = gameService.createGameInAList(3L, novo);

        assertEquals(10L, result.getId());
        verify(gameListRepository).insertBelonging(3L, 10L, 1);
    }

    @Test
    void deveAtualizarGameExistente() {
        Game existente = new Game();
        existente.setId(7L);
        existente.setTitle("Old");
        Game updates = new Game();
        updates.setTitle("New");
        when(gameRepository.findById(7L)).thenReturn(Optional.of(existente));

        gameService.updateGame(7L, updates);

        assertEquals("New", existente.getTitle());
        verify(gameRepository).save(existente);
    }

    @Test
    void deveLancarQuandoAtualizarInexistente() {
        when(gameRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> gameService.updateGame(8L, new Game()));
    }

    @Test
    void deveDeletarGameExistenteNaLista() {
        Long id = 4L, listId = 1L;
        GameMinProjection p1 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(2L);
        GameMinProjection p2 = mock(GameMinProjection.class);
        when(p2.getId()).thenReturn(id);

        when(gameRepository.searchListIdByGameId(id)).thenReturn(listId);
        when(gameRepository.searchByList(listId)).thenReturn(List.of(p1, p2));

        gameService.deleteGame(id);

        verify(gameRepository).removeGameFromList(id, listId);
        verify(gameRepository).updateGamePositions(listId, 1);
    }

    @Test
    void deveLancarQuandoDeletarNaoEncontrado() {
        when(gameRepository.searchListIdByGameId(9L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> gameService.deleteGame(9L));
    }
}
