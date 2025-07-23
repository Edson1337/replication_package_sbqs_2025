package com.edsonmoreira.dslist.services;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edsonmoreira.dslist.dto.GameDTO;
import com.edsonmoreira.dslist.dto.GameMinDTO;
import com.edsonmoreira.dslist.entities.Game;
import com.edsonmoreira.dslist.projections.GameMinProjection;
import com.edsonmoreira.dslist.repositories.GameListRepository;
import com.edsonmoreira.dslist.repositories.GameRepository;
import com.edsonmoreira.dslist.utils.EntityUtils;

@Service
public class GameService {
        
        @Autowired
        private GameRepository gameRepository;
        
        @Autowired
        private GameListRepository gameListRepository;
        
        @Transactional(readOnly = true)
        public GameDTO findById(Long id) {
                Game result = gameRepository.findById(id).get();
                return new GameDTO(result);
        }
        
        @Transactional(readOnly = true)
        public List<GameMinDTO> findAll() {
                List<Game> result = gameRepository.findAll();
                return result.stream().map(x -> new GameMinDTO(x)).toList();
        }
        
        @Transactional(readOnly = true)
        public List<GameMinDTO> findByList(Long listId) {
                List<GameMinProjection> result = gameRepository.searchByList(listId);
                return result.stream().map(x -> new GameMinDTO(x)).toList();
        }
        
        @Transactional
        public Game createGameInAList(Long listId , Game game) {
                List<GameMinProjection> gameList = gameRepository.searchByList(listId);
                Integer gameListSize = gameList.size() + 1;
                
                Game gameSaved = gameRepository.save(game);
                
                gameListRepository.insertBelonging(listId, gameSaved.getId(), gameListSize);
                
                return gameSaved;
                
        }
        
        @Transactional
        public void updateGame(Long id, Game gameUpdates) {
            Game existingGame = gameRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Game not found with id: " + id));

            BeanUtils.copyProperties(gameUpdates, existingGame, EntityUtils.getNullPropertyNames(gameUpdates));

            gameRepository.save(existingGame);
        }
        
        @Transactional
        public void deleteGame(Long id) {
                Long listId = gameRepository.searchListIdByGameId(id);
            if (listId == null) {
                throw new RuntimeException("Game not found in any list.");
            }

            List<GameMinProjection> gamesInList = gameRepository.searchByList(listId);

            int removedPosition = -1;
            for (int i = 0; i < gamesInList.size(); i++) {
                if (gamesInList.get(i).getId().equals(id)) {
                    removedPosition = i;
                    break;
                }
            }

            if (removedPosition == -1) {
                throw new RuntimeException("Game not found in the list.");
            }

            gameRepository.removeGameFromList(id, listId);

            gameRepository.updateGamePositions(listId, removedPosition);
        }
}
