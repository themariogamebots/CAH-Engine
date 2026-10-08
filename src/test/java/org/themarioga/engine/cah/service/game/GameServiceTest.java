package org.themarioga.engine.cah.service.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.themarioga.engine.cah.config.GameConfig;
import org.themarioga.engine.cah.exceptions.game.GameNotEnoughHumansException;
import org.themarioga.engine.cah.dao.intf.game.GameDao;
import org.themarioga.engine.cah.enums.PunctuationModeEnum;
import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.exceptions.game.GameAlreadyFilledException;
import org.themarioga.engine.cah.exceptions.game.GameNotFilledException;
import org.themarioga.engine.cah.models.dictionaries.Dictionary;
import org.themarioga.engine.cah.models.game.Game;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.engine.cah.models.game.Round;
import org.themarioga.engine.cah.services.impl.game.GameServiceImpl;
import org.themarioga.engine.cah.services.intf.dictionaries.DictionaryService;
import org.themarioga.commons.engine.enums.GameStatusEnum;
import org.themarioga.commons.engine.exceptions.ApplicationException;
import org.themarioga.commons.engine.exceptions.game.*;
import org.themarioga.commons.engine.exceptions.player.PlayerAlreadyVotedDeleteException;
import org.themarioga.commons.engine.models.Room;
import org.themarioga.commons.engine.models.User;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @InjectMocks
    private GameServiceImpl gameService;

    @Mock
    private GameDao gameDao;

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private GameConfig gameConfig;

    private Game game;
    private Room room;
    private User creator;
    private User playerUser;
    private Player player;
    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        creator = new User();
        creator.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        dictionary = new Dictionary();
        dictionary.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        game = new Game();
        game.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        game.setRoom(room);
        game.setCreator(creator);
        game.setStatus(GameStatusEnum.CREATED);
        game.setVotationMode(VotationModeEnum.DEMOCRACY);
        game.setPunctuationMode(PunctuationModeEnum.POINTS);
        game.setNumberOfPointsToWin(1);
        game.setNumberOfRoundsToEnd(1);
        game.setMaxNumberOfPlayers(5);
        game.setDictionary(dictionary);

        playerUser = new User();
        playerUser.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        player = new Player();
        player.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        player.setUser(playerUser);
        player.setGame(game);

        Player creatorPlayer = new Player();
        creatorPlayer.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        creatorPlayer.setUser(creator);
        creatorPlayer.setGame(game);
        java.util.Date now = new java.util.Date();
        room.setCreationDate(now);
        creator.setCreationDate(now);
        dictionary.setCreationDate(now);
        game.setCreationDate(now);
        playerUser.setCreationDate(now);
        player.setCreationDate(now);
        creatorPlayer.setCreationDate(now);

        game.getPlayers().add(creatorPlayer);
    }

    @Test
    void testCreateGame() {
        when(gameDao.countByRoom(room)).thenReturn(0L);
        when(gameDao.countByCreator(creator)).thenReturn(0L);
        when(gameConfig.getDefaultVotationMode()).thenReturn(VotationModeEnum.DEMOCRACY);
        when(gameConfig.getDefaultPunctuationMode()).thenReturn(PunctuationModeEnum.POINTS);
        when(gameConfig.getDefaultNumberOfPointsToWin()).thenReturn(10);
        when(gameConfig.getDefaultNumberOfRoundsToEnd()).thenReturn(10);
        when(gameConfig.getDefaultMaxNumberOfPlayers()).thenReturn(10);
        when(gameConfig.getDefaultDictionaryId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        when(dictionaryService.getDictionaryById(UUID.fromString("00000000-0000-0000-0000-000000000000"))).thenReturn(dictionary);
        when(gameDao.createOrUpdate(any(Game.class))).thenAnswer(i -> {
            Game g = i.getArgument(0);
            g.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
            return g;
        });

        Game createdGame = gameService.create(room, creator);

        Assertions.assertNotNull(createdGame);
        Assertions.assertEquals(UUID.fromString("44444444-4444-4444-4444-444444444444"), createdGame.getId());
        Assertions.assertEquals(room, createdGame.getRoom());
        Assertions.assertEquals(creator, createdGame.getCreator());
        Assertions.assertEquals(GameStatusEnum.CREATED, createdGame.getStatus());
        verify(gameDao).createOrUpdate(any(Game.class));
    }

    @Test
    void testCreate_GameAlreadyExists() {
        when(gameDao.countByRoom(room)).thenReturn(1L);

        Assertions.assertThrows(GameAlreadyExistsException.class, () -> gameService.create(room, creator));
    }

    @Test
    void testCreate_CreatorAlreadyHaveGame() {
        when(gameDao.countByRoom(room)).thenReturn(0L);
        when(gameDao.countByCreator(creator)).thenReturn(1L);

        Assertions.assertThrows(GameCreatorAlreadyExistsException.class, () -> gameService.create(room, creator));
    }

    @Test
    void testUpdate() {
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        game.setVotationMode(VotationModeEnum.DICTATORSHIP);
        Game updatedGame = gameService.update(game);

        Assertions.assertNotNull(updatedGame);
        Assertions.assertEquals(VotationModeEnum.DICTATORSHIP, updatedGame.getVotationMode());
    }

    @Test
    void testDeleteGame() {
        doNothing().when(gameDao).delete(game);

        gameService.delete(game);

        verify(gameDao).delete(game);
    }

    @Test
    void testDelete_GameNotExists() {
        Assertions.assertThrows(ApplicationException.class, () -> gameService.delete(null));
    }

    @Test
    void testUpdateStatus() {
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.setStatus(game, GameStatusEnum.DELETING);

        Assertions.assertEquals(GameStatusEnum.DELETING, updatedGame.getStatus());
    }

    @Test
    void testSetMaxVotationMode() {
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.setVotationMode(game, VotationModeEnum.DICTATORSHIP);

        Assertions.assertEquals(VotationModeEnum.DICTATORSHIP, updatedGame.getVotationMode());
    }

    @Test
    void testSetMaxVotationMode_GameAlreadyStarted() {
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.setVotationMode(game, VotationModeEnum.DICTATORSHIP));
    }

    @Test
    void testSetMaxNumberOfPlayers() {
        when(gameConfig.getDefaultMinNumberOfPlayers()).thenReturn(2);
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.setMaxNumberOfPlayers(game, 5);

        Assertions.assertEquals(5, updatedGame.getMaxNumberOfPlayers());
    }

    @Test
    void testSetMaxNumberOfPlayers_GameAlreadyStarted() {
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.setMaxNumberOfPlayers(game, 5));
    }

    @Test
    void testSetMaxNumberOfPlayers_GameAlreadyFilled() {
        Assertions.assertThrows(GameAlreadyFilledException.class, () -> gameService.setMaxNumberOfPlayers(game, 0));
    }

    @Test
    void testSetMaxNumberOfPlayers_GameAlreadyFilled2() {
        when(gameConfig.getDefaultMinNumberOfPlayers()).thenReturn(3);
        Assertions.assertThrows(GameAlreadyFilledException.class, () -> gameService.setMaxNumberOfPlayers(game, 1));
    }

    @Test
    void testSetNumberPointsToWin() {
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.setNumberOfPointsToWin(game, 5);

        Assertions.assertEquals(PunctuationModeEnum.POINTS, updatedGame.getPunctuationMode());
        Assertions.assertEquals(5, updatedGame.getNumberOfPointsToWin());
    }

    @Test
    void testSetNumberPointsToWin_GameAlreadyStarted() {
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.setNumberOfPointsToWin(game, 5));
    }

    @Test
    void testSetNumberRounds() {
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.setNumberOfRoundsToEnd(game, 5);

        Assertions.assertEquals(PunctuationModeEnum.ROUNDS, updatedGame.getPunctuationMode());
        Assertions.assertEquals(5, updatedGame.getNumberOfRoundsToEnd());
    }

    @Test
    void testSetNumberRounds_GameAlreadyStarted() {
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.setNumberOfRoundsToEnd(game, 5));
    }

    @Test
    void testSetDictionary() {
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.setDictionary(game, dictionary);

        Assertions.assertEquals(dictionary, updatedGame.getDictionary());
    }

    @Test
    void testSetDictionary_GameAlreadyStarted() {
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.setDictionary(game, dictionary));
    }

    @Test
    void testAddPlayer() {
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.addPlayer(game, player);

        Assertions.assertEquals(2L, updatedGame.getPlayers().size());
    }

    @Test
    void testAddPlayer_GameStarted() {
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.addPlayer(game, player));
    }

    @Test
    void testAddPlayer_GameAlreadyFilled() {
        game.setMaxNumberOfPlayers(1);

        Assertions.assertThrows(GameAlreadyFilledException.class, () -> gameService.addPlayer(game, player));
    }

    @Test
    void testRemovePlayer() {
        game.getPlayers().add(player);
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.removePlayer(game, player);

        Assertions.assertEquals(1L, updatedGame.getPlayers().size());
    }

    @Test
    void testRemovePlayer_GameAlreadyStarted() {
        game.getPlayers().add(player);
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.removePlayer(game, player));
    }

    @Test
    void testRemovePlayer_CreatorLeave() {
        Player creatorPlayer = game.getPlayers().get(0);

        Assertions.assertThrows(GameCreatorCannotLeaveException.class, () -> gameService.removePlayer(game, creatorPlayer));
    }

    @Test
    void testStartGame() {
        game.getPlayers().add(player);
        game.getPlayers().add(new Player());
        when(gameConfig.getDefaultMinNumberOfPlayers()).thenReturn(3);
        when(gameConfig.getMinHumanPlayers()).thenReturn(2);
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game startedGame = gameService.startGame(game);

        Assertions.assertEquals(GameStatusEnum.STARTED, startedGame.getStatus());
        verify(gameDao).transferCardsFromDictionaryToDeck(game);
    }

    @Test
    void testStartGame_GameAlreadyStarted() {
        game.setStatus(GameStatusEnum.STARTED);

        Assertions.assertThrows(GameAlreadyStartedException.class, () -> gameService.startGame(game));
    }

    @Test
    void testStartGame_GameNotFilled() {
        when(gameConfig.getDefaultMinNumberOfPlayers()).thenReturn(3);

        Assertions.assertThrows(GameNotFilledException.class, () -> gameService.startGame(game));
    }

    @Test
    void testStartGame_GameOverflowed() {
        game.setMaxNumberOfPlayers(1);
        game.getPlayers().add(player);
        when(gameConfig.getDefaultMinNumberOfPlayers()).thenReturn(1);
        when(gameConfig.getMinHumanPlayers()).thenReturn(2);

        Assertions.assertThrows(GameAlreadyFilledException.class, () -> gameService.startGame(game));
    }

    /**
     * Las IAs completan la partida, pero no cuentan como humanos: con un solo humano y dos IAs hay
     * jugadores de sobra y aun así no se puede empezar.
     */
    @Test
    void testStartGame_NotEnoughHumans() {
        game.getPlayers().add(aiPlayer());
        game.getPlayers().add(aiPlayer());
        when(gameConfig.getDefaultMinNumberOfPlayers()).thenReturn(3);
        when(gameConfig.getMinHumanPlayers()).thenReturn(2);

        Assertions.assertThrows(GameNotEnoughHumansException.class, () -> gameService.startGame(game));
    }

    @Test
    void testStartGame_WithAIPlayers() {
        game.getPlayers().add(player);
        game.getPlayers().add(aiPlayer());
        when(gameConfig.getDefaultMinNumberOfPlayers()).thenReturn(3);
        when(gameConfig.getMinHumanPlayers()).thenReturn(2);
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game startedGame = gameService.startGame(game);

        Assertions.assertEquals(GameStatusEnum.STARTED, startedGame.getStatus());
    }

    @Test
    void testVoteDeletion_vote() {
        game.setStatus(GameStatusEnum.STARTED);
        game.getPlayers().add(player);
        game.getPlayers().add(new Player());

        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.voteForDeletion(game, player);

        Assertions.assertNotNull(updatedGame);
        Assertions.assertEquals(1, updatedGame.getDeletionVotes().size());
        Assertions.assertEquals(playerUser, updatedGame.getDeletionVotes().get(0));
        Assertions.assertEquals(GameStatusEnum.STARTED, updatedGame.getStatus());
    }

    @Test
    void testVoteDeletion_delete() {
        game.setStatus(GameStatusEnum.STARTED);
        game.getPlayers().add(player);
        User anotherUser = new User();
        anotherUser.setId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        game.getDeletionVotes().add(anotherUser);

        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.voteForDeletion(game, player);

        Assertions.assertNotNull(updatedGame);
        Assertions.assertEquals(2, updatedGame.getDeletionVotes().size());
        Assertions.assertEquals(playerUser, updatedGame.getDeletionVotes().get(1));
        Assertions.assertEquals(GameStatusEnum.DELETING, updatedGame.getStatus());
    }

    /**
     * Las IAs no votan nunca: si contaran para el quórum, con tres IAs y tres humanos harían falta
     * cuatro votos de los dos humanos que pueden votar (el creador no puede).
     */
    @Test
    void testVoteDeletion_AIPlayersDontCount() {
        game.setStatus(GameStatusEnum.STARTED);
        game.getPlayers().add(player);
        Player anotherPlayer = new Player();
        User anotherUser = new User();
        anotherUser.setId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        anotherPlayer.setUser(anotherUser);
        game.getPlayers().add(anotherPlayer);
        game.getPlayers().add(aiPlayer());
        game.getPlayers().add(aiPlayer());
        game.getPlayers().add(aiPlayer());
        game.getDeletionVotes().add(anotherUser);

        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.voteForDeletion(game, player);

        Assertions.assertEquals(2, updatedGame.getDeletionVotes().size());
        Assertions.assertEquals(GameStatusEnum.DELETING, updatedGame.getStatus());
    }

    private Player aiPlayer() {
        Player aiPlayer = new Player();
        aiPlayer.setId(UUID.randomUUID());
        aiPlayer.setAi(true);
        aiPlayer.setGame(game);
        return aiPlayer;
    }

    @Test
    void testVoteForDeletion_GameNotStarted() {
        Assertions.assertThrows(GameNotStartedException.class, () -> gameService.voteForDeletion(game, player));
    }

    @Test
    void testVoteForDeletion_GameCreatorCannotLeaveException() {
        game.setStatus(GameStatusEnum.STARTED);
        Player creatorPlayer = game.getPlayers().get(0);

        Assertions.assertThrows(GameCreatorCannotLeaveException.class, () -> gameService.voteForDeletion(game, creatorPlayer));
    }

    @Test
    void testVoteDeletion_PlayerAlreadyVoted() {
        game.setStatus(GameStatusEnum.STARTED);
        game.getDeletionVotes().add(playerUser);

        Assertions.assertThrows(PlayerAlreadyVotedDeleteException.class, () -> gameService.voteForDeletion(game, player));
    }

    @Test
    void testEndGame() {
        game.setStatus(GameStatusEnum.ENDING);

        gameService.endGame(game);

        verify(gameDao).delete(game);
    }

    @Test
    void testEndGame_GameNotEnding() {
        Assertions.assertThrows(GameNotEndingException.class, () -> gameService.endGame(game));
    }

    @Test
    void testSetCurrentRound() {
        game.setStatus(GameStatusEnum.STARTED);
        Round round = new Round();
        round.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        round.setCreationDate(new java.util.Date());
        when(gameDao.createOrUpdate(game)).thenReturn(game);

        Game updatedGame = gameService.setCurrentRound(game, round);

        Assertions.assertEquals(round, updatedGame.getCurrentRound());
    }

    @Test
    void testSetCurrentRound_GameNotStarted() {
        game.setStatus(GameStatusEnum.DELETING);
        Round round = new Round();
        round.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        round.setCreationDate(new java.util.Date());

        Assertions.assertThrows(GameNotStartedException.class, () -> gameService.setCurrentRound(game, round));
    }

}
