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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    void findById_deveRetornarGameDTO() {
        Game game = new Game();
        game.setId(1L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameDTO dto = gameService.findById(1L);

        assertEquals(1L, dto.getId());
        verify(gameRepository).findById(1L);
    }

    @Test
    void findAll_deveRetornarListaDeGameMinDTO() {
        Game g1 = new Game(); g1.setId(1L);
        Game g2 = new Game(); g2.setId(2L);
        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));

        List<GameMinDTO> list = gameService.findAll();

        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getId());
        assertEquals(2L, list.get(1).getId());
        verify(gameRepository).findAll();
    }

    @Test
    void findByList_deveRetornarListaDeGameMinDTO() {
        GameMinProjection p1 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(10L);
        GameMinProjection p2 = mock(GameMinProjection.class);
        when(p2.getId()).thenReturn(20L);
        when(gameRepository.searchByList(5L)).thenReturn(List.of(p1, p2));

        List<GameMinDTO> list = gameService.findByList(5L);

        assertEquals(2, list.size());
        assertEquals(10L, list.get(0).getId());
        assertEquals(20L, list.get(1).getId());
        verify(gameRepository).searchByList(5L);
    }

    @Test
    void createGameInAList_deveSalvarEInserirPosicao() {
        long listId = 7L;
        when(gameRepository.searchByList(listId)).thenReturn(List.of());
        Game saved = new Game(); saved.setId(100L);
        when(gameRepository.save(any(Game.class))).thenReturn(saved);

        Game result = gameService.createGameInAList(listId, new Game());

        assertSame(saved, result);
        verify(gameRepository).save(any(Game.class));
        verify(gameListRepository).insertBelonging(listId, 100L, 1);
    }

    @Test
    void updateGame_deveCopiarPropriedadesESalvar() {
        Game existing = new Game(); existing.setId(2L); existing.setTitle("Old");
        when(gameRepository.findById(2L)).thenReturn(Optional.of(existing));
        Game updates = new Game(); updates.setTitle("New");

        gameService.updateGame(2L, updates);

        assertEquals("New", existing.getTitle());
        verify(gameRepository).save(existing);
    }

    @Test
    void deleteGame_deveRemoverEAtualizarPosicoes() {
        long gameId = 3L, listId = 9L;
        when(gameRepository.searchListIdByGameId(gameId)).thenReturn(listId);
        GameMinProjection p1 = mock(GameMinProjection.class);
        when(p1.getId()).thenReturn(1L);
        GameMinProjection p2 = mock(GameMinProjection.class);
        when(p2.getId()).thenReturn(gameId);
        when(gameRepository.searchByList(listId)).thenReturn(List.of(p1, p2));

        gameService.deleteGame(gameId);

        verify(gameRepository).removeGameFromList(gameId, listId);
        verify(gameRepository).updateGamePositions(listId, 1);
    }

    @Test
    void deleteGame_quandoNaoEstiverEmLista_deveLancarExcecao() {
        when(gameRepository.searchListIdByGameId(5L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameService.deleteGame(5L));
        assertEquals("Game not found in any list.", ex.getMessage());
    }

    @Test
    void deleteGame_quandoIdNaoEncontradoNaLista_deveLancarExcecao() {
        long gameId = 8L, listId = 4L;
        when(gameRepository.searchListIdByGameId(gameId)).thenReturn(listId);
        GameMinProjection only = mock(GameMinProjection.class);
        when(only.getId()).thenReturn(1L);
        when(gameRepository.searchByList(listId)).thenReturn(List.of(only));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameService.deleteGame(gameId));
        assertEquals("Game not found in the list.", ex.getMessage());
    }
}
