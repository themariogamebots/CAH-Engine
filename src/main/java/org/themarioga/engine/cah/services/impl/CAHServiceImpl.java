package org.themarioga.engine.cah.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.themarioga.engine.cah.ai.AIPlayerStrategy;
import org.themarioga.engine.cah.config.GameConfig;
import org.themarioga.engine.cah.enums.CAHErrorEnum;
import org.themarioga.engine.cah.enums.PunctuationModeEnum;
import org.themarioga.engine.cah.enums.RoundStatusEnum;
import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.exceptions.player.AIPlayerDoesntExistsException;
import org.themarioga.engine.cah.exceptions.player.PlayerCannotDrawCardException;
import org.themarioga.engine.cah.exceptions.player.PlayerCannotVoteCardException;
import org.themarioga.engine.cah.exceptions.round.RoundPresidentCannotPlayCardException;
import org.themarioga.engine.cah.exceptions.round.RoundWrongStatusException;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.dictionaries.Dictionary;
import org.themarioga.engine.cah.models.game.*;
import org.themarioga.engine.cah.services.intf.CAHService;
import org.themarioga.engine.cah.services.intf.game.GameService;
import org.themarioga.engine.cah.services.intf.game.PlayerService;
import org.themarioga.engine.cah.services.intf.game.RoundResultService;
import org.themarioga.engine.cah.services.intf.game.RoundService;
import org.themarioga.commons.engine.enums.CommonErrorEnum;
import org.themarioga.commons.engine.enums.GameStatusEnum;
import org.themarioga.commons.engine.exceptions.ApplicationException;
import org.themarioga.commons.engine.exceptions.game.*;
import org.themarioga.commons.engine.exceptions.player.PlayerDoesntExistsException;
import org.themarioga.commons.engine.exceptions.room.RoomAlreadyExistsException;
import org.themarioga.commons.engine.exceptions.user.UserDoesntExistsException;
import org.themarioga.commons.engine.models.Room;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.engine.security.SecurityUtils;
import org.themarioga.commons.engine.services.intf.RoomService;
import org.themarioga.commons.engine.services.intf.UserService;
import org.themarioga.commons.engine.util.Assert;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Service
public class CAHServiceImpl implements CAHService {

    private final Logger logger = LoggerFactory.getLogger(CAHServiceImpl.class);

    private final RoomService roomService;
    private final GameService gameService;
    private final PlayerService playerService;
    private final RoundService roundService;
    private final RoundResultService roundResultService;
    private final UserService userService;
    private final AIPlayerStrategy aiPlayerStrategy;
    private final GameConfig gameConfig;

    private final Random random = new SecureRandom();

    @Autowired
    public CAHServiceImpl(RoomService roomService, GameService gameService, PlayerService playerService, RoundService roundService, RoundResultService roundResultService, UserService userService, AIPlayerStrategy aiPlayerStrategy, GameConfig gameConfig) {
        this.roomService = roomService;
        this.gameService = gameService;
        this.playerService = playerService;
        this.roundService = roundService;
        this.roundResultService = roundResultService;
        this.userService = userService;
        this.aiPlayerStrategy = aiPlayerStrategy;
        this.gameConfig = gameConfig;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game createGame(String roomName) {
        logger.debug("Creating game for room {}", roomName);

        // Check roomName exists
        Assert.assertNotNull(roomName, CommonErrorEnum.ROOM_NOT_FOUND);

        // Create or load room
        Room room;
        try {
            room = roomService.createOrReactivate(roomName, roomName);
        } catch (RoomAlreadyExistsException e) {
            room = roomService.getByRoomname(roomName);
        }

        return createGame(room);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game createGame(Room room) {
        logger.debug("Creating game for room {}", room);

        // Check room exists
        Assert.assertNotNull(room, CommonErrorEnum.ROOM_NOT_FOUND);

        // Obtenemos el usuario de la sesión y comprobamos que existe
        User creator = getSessionUser();

        // Create the game
        Game game = gameService.create(room, creator);

        // Add the game creator player
        Player player = playerService.create(game, creator);
        game.getPlayers().add(player);

        return gameService.update(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setVotationMode(Room room, VotationModeEnum type) {
        logger.debug("Setting VotationMode {} to room {}", type, room);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        return gameService.setVotationMode(game, type);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setMaxNumberOfPlayers(Room room, int maxNumberOfPlayers) {
        logger.debug("Setting MaxNumberOfPlayers {} to room {}", maxNumberOfPlayers, room);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        return gameService.setMaxNumberOfPlayers(game, maxNumberOfPlayers);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setNumberOfPointsToWin(Room room, int numberOfCards) {
        logger.debug("Setting NumberOfPointsToWin {} to room {}", numberOfCards, room);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        return gameService.setNumberOfPointsToWin(game, numberOfCards);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setNumberOfRoundsToEnd(Room room, int numberOfRoundsToEnd) {
        logger.debug("Setting NumberOfRoundsToEnd {} to room {}", numberOfRoundsToEnd, room);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        return gameService.setNumberOfRoundsToEnd(game, numberOfRoundsToEnd);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game setDictionary(Room room, Dictionary dictionary) {
        logger.debug("Setting Dictionary {} to room {}", dictionary, room);

        // Check dictionary exists
        Assert.assertNotNull(dictionary, CAHErrorEnum.DICTIONARY_NOT_FOUND);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        return gameService.setDictionary(game, dictionary);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game deleteGameByCreator(Room room) {
        logger.debug("Deleting game from room {}", room);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        // The AI users only exist for this game: collect them before the players are gone
        List<User> aiUsers = getAIUsers(game);

        // Delete the game
        gameService.delete(game);

        // Delete the AI users, now that no player references them
        aiUsers.forEach(userService::delete);

        return game;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game addPlayer(Room room) {
        logger.debug("Adding player to game from room {}", room);

        // Get the game
        Game game = getGameByRoom(room);

        // Obtenemos el usuario de la sesión y comprobamos que existe
        User user = getSessionUser();

        // Create the player
        Player player = playerService.create(game, user);

        // Add player to the game
        return gameService.addPlayer(game, player);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game addAIPlayer(Room room, String name) {
        logger.debug("Adding AI player {} to game from room {}", name, room);

        // Check the name is not empty: the platform translates it, the engine knows nothing about i18n
        Assert.assertNotEmpty(name, CommonErrorEnum.USER_NAME_EMPTY);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        // Each AI player needs its own user, because a user can only play one game at a time. The
        // ':' can't collide with a Telegram alias, and "ai:" can't collide with "tg:<id>"
        User user = userService.createOrReactivate(AI_USERNAME_PREFIX + UUID.randomUUID(), name, game.getCreator().getLang());

        // Create the player
        Player player = playerService.createAI(game, user);

        // Add player to the game
        return gameService.addPlayer(game, player);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game removeAIPlayer(Room room) {
        logger.debug("Removing AI player from game in room {}", room);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        // Get the last AI player that joined
        Player player = game.getPlayers().stream().filter(Player::isAi).max(Comparator.comparing(Player::getJoinOrder)).orElseThrow(AIPlayerDoesntExistsException::new);
        User user = player.getUser();

        // Remove the player from the game
        game = gameService.removePlayer(game, player);

        // Delete the player and its user, that only existed for this game
        playerService.delete(player);
        userService.delete(user);

        return game;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game kickPlayer(Room room, User userKicked) {
        logger.debug("Kicking player {} from game in room {}", userKicked, room);

        // Check userKicked exists
        Assert.assertNotNull(userKicked, CommonErrorEnum.USER_NOT_FOUND);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        // Try to get the player for this game and user
        Player player = playerService.findPlayerByGameAndUser(game, userKicked);

        // Check if player has been created successfully
        if (player == null)
            throw new PlayerDoesntExistsException();

        // Remove the player from the game
        game = gameService.removePlayer(game, player);

        // Delete the player
        playerService.delete(player);

        // An AI player's user only existed for this game
        if (player.isAi()) userService.delete(userKicked);

        return game;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game leavePlayer(Room room) {
        logger.debug("Leaving game in room {}", room);

        // Get the game
        Game game = getGameByRoom(room);

        // Get the player
        Player player = getPlayerBySessionUserAndGame(game);

        // Remove the player from the game
        game = gameService.removePlayer(game, player);

        // Delete the player
        playerService.delete(player);

        return game;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game voteForDeletion(Room room) {
        logger.debug("User voting to delete game on room {}", room);

        // Get the game
        Game game = getGameByRoom(room);

        // Get the player
        Player player = getPlayerBySessionUserAndGame(game);

        // Vote for deletion
        game = gameService.voteForDeletion(game, player);

        return game;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game startGame(Room room) {
        logger.debug("Starting game from room {}", room);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the user performing the action is the creator
        checkSessionUserIsCreator(game);

        // Start the game
        game = gameService.startGame(game);

        // Start the first round
        startRound(game, getNextRoundNumber(null));

        return gameService.update(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game playCard(Room room, Card card) {
        logger.debug("User playing card {} on room {}", card, room);

        // Check user exists
        Assert.assertNotNull(card, CAHErrorEnum.CARD_NOT_FOUND);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the game have already started
        if (game.getStatus() != GameStatusEnum.STARTED)
            throw new GameNotStartedException();

        // Get the player
        Player player = getPlayerBySessionUserAndGame(game);

        doPlayCard(game, player, card);

        return gameService.update(game);
    }

    /**
     * Juega la carta en nombre de {@code player}, sea quien tiene la sesión o una IA. Si con ella han
     * jugado todos, abre la votación y deja votar a las IAs a las que les toque.
     */
    private void doPlayCard(Game game, Player player, Card card) {
        // If the game is Classic or dictatorship the creator cant
        if ((game.getVotationMode() == VotationModeEnum.CLASSIC || game.getVotationMode() == VotationModeEnum.DICTATORSHIP) && game.getCurrentRound().getRoundPresident().getId().equals(player.getId()))
            throw new RoundPresidentCannotPlayCardException();

        // Add card to round
        roundService.addCardToPlayedCards(game.getCurrentRound(), player, card);

        // Remove card from player hand
        playerService.removeCardFromHand(player, card);

        // Set status to voting if everyone have played
        if (roundService.checkIfEveryoneHavePlayedACard(game.getCurrentRound())) {
            roundService.setStatus(game.getCurrentRound(), RoundStatusEnum.VOTING);

            // The AI players vote as soon as the voting opens
            voteAIPlayers(game);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game voteCard(Room room, Card card) {
        logger.debug("User voting card {} on room {}", card, room);

        // Check user exists
        Assert.assertNotNull(card, CAHErrorEnum.CARD_NOT_FOUND);

        // Get the game
        Game game = getGameByRoom(room);

        // Check the game have already started
        if (game.getStatus() != GameStatusEnum.STARTED)
            throw new GameNotStartedException();

        // Get the player
        Player player = getPlayerBySessionUserAndGame(game);

        doVoteCard(game, player, card);

        return gameService.update(game);
    }

    /**
     * Vota la carta en nombre de {@code player}, sea quien tiene la sesión o una IA. Si con su voto
     * se alcanza el quórum, puntúa y cierra la ronda.
     */
    private void doVoteCard(Game game, Player player, Card card) {
        // Check you didn't vote for your own card
        if (roundService.getPlayedCardByCard(game.getCurrentRound(), card).getPlayer().getId().equals(player.getId()))
            throw new PlayerCannotVoteCardException();

        // Vote card
        roundService.voteCard(game.getCurrentRound(), player, card);

        // Check if everyone have voted a card
        if (roundService.checkIfEveryoneHaveVotedACard(game.getCurrentRound())) {
            // Get the most voted card
            PlayedCard mostVotedCard = roundService.getPlayedCardByCard(game.getCurrentRound(), roundService.getMostVotedCard(game.getCurrentRound()));
            if (mostVotedCard == null)
                throw new RoundWrongStatusException();

            // Keep how every card did, before the round (and its cards and votes) is deleted
            roundResultService.recordRound(game.getCurrentRound(), mostVotedCard.getCard());

            // Give 1 point to the player who played the most voted card
            playerService.incrementPoints(mostVotedCard.getPlayer());

            // Set the round status to ending
            roundService.setStatus(game.getCurrentRound(), RoundStatusEnum.ENDING);

            // Check if game is ended
            if (checkIfGameEnded(game)) {
                // Set the ended status
                game.setStatus(GameStatusEnum.ENDING);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Game nextRound(Game game) {
        logger.debug("Starting next round on game {}", game);

        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Check the game have already started
        if (game.getStatus() != GameStatusEnum.STARTED)
            throw new GameNotStartedException();

        // Check if there is already a round and is in the correct state
        if (game.getCurrentRound() == null || !game.getCurrentRound().getStatus().equals(RoundStatusEnum.ENDING))
            throw new RoundWrongStatusException();

        // Get the current round
        Round round = game.getCurrentRound();

        // Unset the current round
        gameService.setCurrentRound(game, null);

        // Delete the current round
        roundService.deleteRound(round);

        // Start the next round
        startRound(game, getNextRoundNumber(round));

        return gameService.update(game);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Player getWinner(Game game) {
        logger.debug("Getting the winner of the game {}", game);

        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        List<Player> players = new ArrayList<>(game.getPlayers());

        // On a tie, the player who joined first wins, so the result no longer
        // depends on the arbitrary iteration order of game.getPlayers()
        players.sort(Comparator.comparing(Player::getPoints).reversed().thenComparing(Player::getJoinOrder));

        return players.get(0);
    }

    /**
     * Borra una partida que ha terminado por las reglas.
     * <p>
     * A diferencia de {@link #deleteGameByCreator} no mira quién tiene la sesión: la última ronda la
     * puede cerrar cualquiera (en democracia el último en votar, en CLASSIC el presidente, que puede
     * ser una IA), y exigir al creador dejaba la partida colgada en ENDING.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void endGame(Game game) {
        logger.debug("Ending the game {}", game);

        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // The AI users only exist for this game: collect them before the players are gone
        List<User> aiUsers = getAIUsers(game);

        // Delete the game (it checks the game is ending)
        gameService.endGame(game);

        // Delete the AI users, now that no player references them
        aiUsers.forEach(userService::delete);
    }

    private List<User> getAIUsers(Game game) {
        return game.getPlayers().stream().filter(Player::isAi).map(Player::getUser).toList();
    }

    private void startRound(Game game, int roundNumber) {
        logger.debug("Starting round on game {}", game);

        // Create next round
        Round round = roundService.createRound(game, roundNumber);

        // Set the next round
        gameService.setCurrentRound(game, round);

        // Fill player hands
        for (Player player : game.getPlayers()) {
            // A hand can be bigger than the limit if the limit was lowered mid-game: that player
            // draws nothing and the hand shrinks back as cards are played
            int numberCardsNeedToFillHand = Math.max(0, gameConfig.getNumberOfCardsInHand() - player.getHand().size());

            // Deal a random sample of cards instead of always the first ones in the deck,
            // since the deck's persisted order is not itself randomized
            List<Card> shuffledDeck = new ArrayList<>(game.getWhiteCardsDeck());
            Collections.shuffle(shuffledDeck, random);
            List<Card> cardsToTransfer = new ArrayList<>(shuffledDeck.subList(0, numberCardsNeedToFillHand));
            playerService.insertWhiteCardsIntoPlayerHand(player, cardsToTransfer);

            game.getWhiteCardsDeck().removeAll(cardsToTransfer);
        }

        // The AI players play as soon as the round starts
        playAIPlayers(game);
    }

    /**
     * Cada IA que no sea presidente de la ronda juega la carta que le diga la estrategia.
     */
    private void playAIPlayers(Game game) {
        Round round = game.getCurrentRound();

        for (Player player : aiPlayers(game)) {
            if (round.getStatus() != RoundStatusEnum.PLAYING) return;
            if (isRoundPresident(game, player)) continue;

            Card card = aiPlayerStrategy.chooseCardToPlay(round, player);
            if (card == null) throw new PlayerCannotDrawCardException();

            doPlayCard(game, player, card);
        }
    }

    /**
     * Vota cada IA a la que le toque: en democracia todas, en el resto solo si es la presidenta de
     * la ronda. Se para en cuanto la ronda se cierra.
     */
    private void voteAIPlayers(Game game) {
        Round round = game.getCurrentRound();

        for (Player player : aiPlayers(game)) {
            if (round.getStatus() != RoundStatusEnum.VOTING) return;
            if (game.getVotationMode() != VotationModeEnum.DEMOCRACY && !isRoundPresident(game, player)) continue;

            Card card = aiPlayerStrategy.chooseCardToVote(round, player);
            if (card == null) throw new PlayerCannotVoteCardException();

            doVoteCard(game, player, card);
        }
    }

    private List<Player> aiPlayers(Game game) {
        return game.getPlayers().stream().filter(Player::isAi).sorted(Comparator.comparing(Player::getJoinOrder)).toList();
    }

    private boolean isRoundPresident(Game game, Player player) {
        Player president = game.getCurrentRound().getRoundPresident();

        return president != null && Objects.equals(president.getId(), player.getId());
    }

    private boolean checkIfGameEnded(Game game) {
        if (game.getPunctuationMode().equals(PunctuationModeEnum.ROUNDS)) {
            return Objects.equals(game.getCurrentRound().getRoundNumber() + 1, game.getNumberOfRoundsToEnd());
        } else if (game.getPunctuationMode().equals(PunctuationModeEnum.POINTS)) {
            for (Player player : game.getPlayers()) {
                if (Objects.equals(player.getPoints(), game.getNumberOfPointsToWin()))
                    return true;
            }

            return false;
        }

        throw new GameNotEndingException();
    }

    private int getNextRoundNumber(Round round) {
        logger.debug("Getting next round number from round {}", round);

        // Determine and return the next round number
        if (round == null) {
            return 0;
        } else {
            return round.getRoundNumber() + 1;
        }
    }

    // ///////////// Helpers //////////////////

    private User getSessionUser() {
        User user = SecurityUtils.getUser();
        if (user == null)
            throw new UserDoesntExistsException();
        return user;
    }

    private Game getGameByRoom(Room room) {
        // Check room exists
        Assert.assertNotNull(room, CommonErrorEnum.ROOM_NOT_FOUND);

        // Get the game from this room
        Game game = gameService.getByRoom(room);

        // Check if the game exists
        if (game == null) throw new GameDoesntExistsException();

        return game;
    }

    /**
     * Solo el creador manda sobre su partida... salvo quien administra el bot, que tiene comandos
     * para borrar la partida de otro y para borrarlas todas. Sin esta salida esos comandos no
     * podrían existir: la comprobación del creador los rechazaría uno por uno, y en el caso de
     * borrarlas todas en silencio, porque el bucle registra el error y sigue con la siguiente.
     */
    private void checkSessionUserIsCreator(Game game) {
        if (SecurityUtils.isAdmin()) return;

        // Obtenemos el usuario de la sesión y comprobamos que existe
        User creator = getSessionUser();

        // Check if the user performing the action is the creator of the game
        if (!game.getCreator().getId().equals(creator.getId()))
            throw new GameOnlyCreatorCanPerformActionException();
    }

    private Player getPlayerBySessionUserAndGame(Game game) {
        // Obtenemos el usuario de la sesión y comprobamos que existe
        User user = getSessionUser();

        // Try to get the player for this game and user
        Player player = playerService.findPlayerByGameAndUser(game, user);

        // Check if player has been created successfully
        if (player == null) throw new PlayerDoesntExistsException();

        return player;
    }

}
