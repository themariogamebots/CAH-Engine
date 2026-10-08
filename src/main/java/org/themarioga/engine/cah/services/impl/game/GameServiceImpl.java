package org.themarioga.engine.cah.services.impl.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.themarioga.engine.cah.config.GameConfig;
import org.themarioga.engine.cah.dao.intf.game.GameDao;
import org.themarioga.engine.cah.enums.CAHErrorEnum;
import org.themarioga.engine.cah.enums.PunctuationModeEnum;
import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.exceptions.game.GameAlreadyFilledException;
import org.themarioga.engine.cah.exceptions.game.GameNotEnoughHumansException;
import org.themarioga.engine.cah.exceptions.game.GameNotFilledException;
import org.themarioga.engine.cah.exceptions.dictionary.DictionaryDoesntExistsException;
import org.themarioga.engine.cah.models.dictionaries.Dictionary;
import org.themarioga.engine.cah.models.game.Game;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.engine.cah.models.game.Round;
import org.themarioga.engine.cah.services.intf.dictionaries.DictionaryService;
import org.themarioga.engine.cah.services.intf.game.GameService;
import org.themarioga.commons.engine.enums.CommonErrorEnum;
import org.themarioga.commons.engine.enums.GameStatusEnum;
import org.themarioga.commons.engine.exceptions.ApplicationException;
import org.themarioga.commons.engine.exceptions.game.*;
import org.themarioga.commons.engine.exceptions.player.PlayerAlreadyVotedDeleteException;
import org.themarioga.commons.engine.models.Room;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.engine.util.Assert;

import java.util.Date;
import java.util.UUID;
import java.util.Objects;

@Service
public class GameServiceImpl implements GameService {

    private final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameDao gameDao;
    private final DictionaryService dictionaryService;
    private final GameConfig gameConfig;

    @Autowired
    public GameServiceImpl(GameDao gameDao, DictionaryService dictionaryService, GameConfig gameConfig) {
        this.gameDao = gameDao;
        this.dictionaryService = dictionaryService;
        this.gameConfig = gameConfig;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game create(Room room, User creator) {
        logger.debug("Creating game for room {} by user {}", room, creator);

        // Check if the room exists
        Assert.assertNotNull(room, CommonErrorEnum.ROOM_NOT_FOUND);

        // Check if a game already exists in this room
        if (gameDao.countByRoom(room) > 0)
            throw new GameAlreadyExistsException();

        // Check if this user already have a running game
        if (gameDao.countByCreator(creator) > 0)
            throw new GameCreatorAlreadyExistsException();

        // Check if the creator exists
        Assert.assertNotNull(creator, CommonErrorEnum.USER_NOT_FOUND);

        // Create game
        Game game = new Game();
        game.setRoom(room);
        game.setCreator(creator);
        game.setStatus(GameStatusEnum.CREATED);
        game.setCreationDate(new Date());
        game.setVotationMode(gameConfig.getDefaultVotationMode());
        game.setPunctuationMode(gameConfig.getDefaultPunctuationMode());
        game.setNumberOfPointsToWin(gameConfig.getDefaultNumberOfPointsToWin());
        game.setNumberOfRoundsToEnd(gameConfig.getDefaultNumberOfRoundsToEnd());
        game.setMaxNumberOfPlayers(gameConfig.getDefaultMaxNumberOfPlayers());
        game.setDictionary(getDefaultDictionary());
        game.setCreationDate(new Date());

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game update(Game game) {
        logger.debug("Updating game: {}", game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void delete(Game game) {
        logger.debug("Deleting game: {}", game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        gameDao.delete(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setStatus(Game game, GameStatusEnum gameStatusEnum) {
        logger.debug("Setting status {} to game: {}", gameStatusEnum, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Set the status
        game.setStatus(gameStatusEnum);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setVotationMode(Game game, VotationModeEnum type) {
        logger.debug("Setting votation mode {} to game {}", type, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check if the game has already started
        if (game.getStatus() == GameStatusEnum.STARTED)
            throw new GameAlreadyStartedException();

        // Set the type to game
        game.setVotationMode(type);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setMaxNumberOfPlayers(Game game, int maxNumberOfPlayers) {
        logger.debug("Setting max number of players {} to game {}", maxNumberOfPlayers, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check if the game has already started
        if (game.getStatus() == GameStatusEnum.STARTED)
            throw new GameAlreadyStartedException();

        // Check if the game has more players than the max we want to set
        if (maxNumberOfPlayers < game.getPlayers().size())
            throw new GameAlreadyFilledException();

        // Check if max number of players is less than min
        if (maxNumberOfPlayers < gameConfig.getDefaultMinNumberOfPlayers())
            throw new GameAlreadyFilledException();

        // Set the max number of players
        game.setMaxNumberOfPlayers(maxNumberOfPlayers);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setNumberOfPointsToWin(Game game, int numberOfCards) {
        logger.debug("Setting number of cards {} to game {}", numberOfCards, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check if the game has already started
        if (game.getStatus() == GameStatusEnum.STARTED)
            throw new GameAlreadyStartedException();

        // Set the game punctuation type
        game.setPunctuationMode(PunctuationModeEnum.POINTS);

        // Set the number of cards to win
        game.setNumberOfPointsToWin(numberOfCards);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setNumberOfRoundsToEnd(Game game, int numberOfRoundsToEnd) {
        logger.debug("Setting number of rounds {} to game {}", numberOfRoundsToEnd, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check if the game has already started
        if (game.getStatus() == GameStatusEnum.STARTED)
            throw new GameAlreadyStartedException();

        // Set the game punctuation type
        game.setPunctuationMode(PunctuationModeEnum.ROUNDS);

        // Set the number of cards to win
        game.setNumberOfRoundsToEnd(numberOfRoundsToEnd);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setDictionary(Game game, Dictionary dictionary) {
        logger.debug("Setting dictionary {} to game {}", dictionary, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check game exists
        Assert.assertNotNull(dictionary, CAHErrorEnum.DICTIONARY_NOT_FOUND);

        // Check if the game has already started
        if (game.getStatus() == GameStatusEnum.STARTED)
            throw new GameAlreadyStartedException();

        // Set the dictionary
        game.setDictionary(dictionary);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game addPlayer(Game game, Player player) {
        logger.debug("Adding player {} to game {}", player, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check player exists
        Assert.assertNotNull(player, CommonErrorEnum.PLAYER_NOT_FOUND);

        // Check if the game has not started
        if (game.getStatus() != GameStatusEnum.CREATED)
            throw new GameAlreadyStartedException();

        // Check if game is already filled
        if (game.getPlayers().size() + 1 > game.getMaxNumberOfPlayers())
            throw new GameAlreadyFilledException();

        // Add player to game
        game.getPlayers().add(player);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game removePlayer(Game game, Player player) {
        logger.debug("Removing player from player {} in game {}", player, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check player exists
        Assert.assertNotNull(player, CommonErrorEnum.PLAYER_NOT_FOUND);

        // Check the status of the game
        if (game.getStatus() != GameStatusEnum.CREATED)
            throw new GameAlreadyStartedException();

        // Game creator cannor leave
        if (Objects.equals(game.getCreator().getId(), player.getUser().getId()))
            throw new GameCreatorCannotLeaveException();

        // Remove a player
        game.getPlayers().removeIf(p -> Objects.equals(p.getId(), player.getId()));

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game startGame(Game game) {
        logger.debug("Starting game in room {}", game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check the status of the game
        if (game.getStatus() == GameStatusEnum.STARTED)
            throw new GameAlreadyStartedException();

        // Check the players are more than min
        if (game.getPlayers().size() < gameConfig.getDefaultMinNumberOfPlayers())
            throw new GameNotFilledException();

        // Check there are enough humans: the AI players fill the game, but don't replace people
        if (countHumanPlayers(game) < gameConfig.getMinHumanPlayers())
            throw new GameNotEnoughHumansException();

        // Check the players are less than max
        if (game.getPlayers().size() > game.getMaxNumberOfPlayers())
            throw new GameAlreadyFilledException();

        // Change game status
        game.setStatus(GameStatusEnum.STARTED);
        game = gameDao.createOrUpdate(game);

        // Transfer cards to deck
        gameDao.transferCardsFromDictionaryToDeck(game);

        return game;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void endGame(Game game) {
        logger.debug("Ending game {}", game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Only started games can end
        if (game.getStatus() != GameStatusEnum.ENDING)
            throw new GameNotEndingException();

        gameDao.delete(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setCurrentRound(Game game, Round round) {
        logger.debug("Setting current round {} to game {}", round, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check the number of players in the game
        if (game.getStatus() != GameStatusEnum.STARTED)
            throw new GameNotStartedException();

        // Set the current round
        game.setCurrentRound(round);

        return gameDao.createOrUpdate(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game voteForDeletion(Game game, Player player) {
        logger.debug("Player {} vote for the deletion of the game {}", player, game);

        // Check game exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check player exists
        Assert.assertNotNull(player, CommonErrorEnum.PLAYER_NOT_FOUND);

        // Check if game is started
        if (game.getStatus() == GameStatusEnum.CREATED)
            throw new GameNotStartedException();

        // Check if the player trying to delete is the creator
        if (game.getCreator().getId().equals(player.getUser().getId()))
            throw new GameCreatorCannotLeaveException();

        // Check user not voted already
        if (game.getDeletionVotes().stream().anyMatch(a -> a.getId().equals(player.getUser().getId())))
            throw new PlayerAlreadyVotedDeleteException();

        // Add deletion votes
        game.getDeletionVotes().add(player.getUser());

        // If more than half of the human players vote to delete the game... The AI players never
        // vote, so counting them would make the quorum unreachable
        if (game.getDeletionVotes().size() >= ((countHumanPlayers(game) / 2) + 1)) {
            game.setStatus(GameStatusEnum.DELETING);
        }

        return gameDao.createOrUpdate(game);
    }

    /**
     * Diccionario con el que arranca una partida a la que todavía no le han elegido uno.
     * <p>
     * Si no está configurado, o apunta a un diccionario que no existe, la partida no se puede
     * crear. Se dice explícitamente: sin esta comprobación el fallo llegaba como un "Identifier may
     * not be null" de Hibernate, que no ayuda a saber que falta configuración.
     */
    private Dictionary getDefaultDictionary() {
        UUID defaultDictionaryId = gameConfig.getDefaultDictionaryId();
        if (defaultDictionaryId == null) {
            logger.error("No hay diccionario por defecto configurado (cah.game.default-dictionary-id)");

            throw new DictionaryDoesntExistsException();
        }

        Dictionary dictionary = dictionaryService.getDictionaryById(defaultDictionaryId);
        if (dictionary == null) {
            logger.error("El diccionario por defecto {} no existe", defaultDictionaryId);

            throw new DictionaryDoesntExistsException();
        }

        return dictionary;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Game getByRoom(Room room) {
        logger.debug("Getting game with room: {}", room);

        return gameDao.getByRoom(room);
    }

    private int countHumanPlayers(Game game) {
        return (int) game.getPlayers().stream().filter(player -> !player.isAi()).count();
    }

}
