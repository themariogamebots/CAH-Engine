package org.themarioga.engine.cah.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.themarioga.engine.cah.BaseTest;
import org.themarioga.engine.cah.exceptions.game.GameAlreadyFilledException;
import org.themarioga.engine.cah.exceptions.game.GameNotEnoughHumansException;
import org.themarioga.engine.cah.exceptions.player.AIPlayerDoesntExistsException;
import org.themarioga.engine.cah.models.game.PlayedCard;
import org.themarioga.engine.cah.models.game.VotedCard;
import org.themarioga.commons.engine.exceptions.ApplicationException;
import org.themarioga.commons.engine.exceptions.user.UserDoesntExistsException;
import org.themarioga.engine.cah.enums.RoundStatusEnum;
import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.exceptions.round.RoundPresidentCannotPlayCardException;
import org.themarioga.engine.cah.exceptions.round.RoundWrongStatusException;
import org.themarioga.engine.cah.models.dictionaries.Dictionary;
import org.themarioga.engine.cah.models.game.Game;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.engine.cah.services.intf.CAHService;
import org.themarioga.engine.cah.services.intf.dictionaries.CardService;
import org.themarioga.engine.cah.services.intf.dictionaries.DictionaryService;
import org.themarioga.engine.cah.services.intf.game.GameService;
import org.themarioga.commons.engine.enums.GameStatusEnum;
import org.themarioga.commons.engine.exceptions.game.GameCreatorCannotLeaveException;
import org.themarioga.commons.engine.exceptions.game.GameDoesntExistsException;
import org.themarioga.commons.engine.exceptions.game.GameNotEndingException;
import org.themarioga.commons.engine.exceptions.game.GameNotStartedException;
import org.themarioga.commons.engine.exceptions.game.GameOnlyCreatorCanPerformActionException;
import org.themarioga.commons.engine.exceptions.player.PlayerDoesntExistsException;
import org.themarioga.commons.engine.models.Room;
import org.themarioga.commons.engine.security.SecurityUtils;
import org.themarioga.commons.engine.security.UserRole;
import org.themarioga.commons.engine.services.intf.RoomService;
import org.themarioga.commons.engine.services.intf.UserService;

import java.util.UUID;

// Una sola anotación con todos los ficheros, no una por fichero: cada @DatabaseSetup hace un
// CLEAN_INSERT de todas las tablas que declara su DTD —no solo de las que trae el fichero—, así que
// repetir la anotación hace que cada fichero borre lo que insertó el anterior.
@DatabaseSetup({"classpath:dbunit/service/setup/lang.xml", "classpath:dbunit/service/setup/user.xml", "classpath:dbunit/service/setup/room.xml", "classpath:dbunit/service/setup/dictionaries/dictionary.xml", "classpath:dbunit/service/setup/dictionaries/dictionarycollaborators.xml", "classpath:dbunit/service/setup/dictionaries/card.xml", "classpath:dbunit/service/setup/cah.xml"})
class CAHServiceTest extends BaseTest {

    @Autowired
    CAHService cahService;
    @Autowired
    RoomService roomService;
    @Autowired
    UserService userService;
    @Autowired
    DictionaryService dictionaryService;
    @Autowired
    GameService gameService;
    @Autowired
    CardService cardService;

    @BeforeEach
    void setUpUser() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), UserRole.USER);
    }

    @Test
    void testCreateGame_NewRoom() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("88888888-8888-8888-8888-888888888888")), UserRole.USER);

        Game game = cahService.createGame("New Room");

        Assertions.assertNotNull(game);
        Assertions.assertNotNull(game.getId());
        Assertions.assertEquals("New Room", game.getRoom().getRoomname());
        Assertions.assertEquals("New Room", game.getRoom().getName());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getCreator().getId());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getPlayers().get(0).getUser().getId());
    }

    @Test
    void testCreateGame_ReactivateRoom() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("88888888-8888-8888-8888-888888888888")), UserRole.USER);

        Game game = cahService.createGame("tg:-100003");

        Assertions.assertNotNull(game);
        Assertions.assertNotNull(game.getId());
        Assertions.assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), game.getRoom().getId());
        Assertions.assertEquals("tg:-100003", game.getRoom().getRoomname());
        Assertions.assertEquals(true, game.getRoom().getActive());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getCreator().getId());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getPlayers().get(0).getUser().getId());
    }

    @Test
    void testCreateGame_ExistingRoom() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("88888888-8888-8888-8888-888888888888")), UserRole.USER);

        Game game = cahService.createGame("tg:-100002");

        Assertions.assertNotNull(game);
        Assertions.assertNotNull(game.getId());
        Assertions.assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), game.getRoom().getId());
        Assertions.assertEquals("tg:-100002", game.getRoom().getRoomname());
        Assertions.assertEquals("Second", game.getRoom().getName());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getCreator().getId());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getPlayers().get(0).getUser().getId());
    }

    /**
     * Sobrecarga que usan las implementaciones de plataforma (CAH-Telegram): la sala se resuelve
     * fuera, junto con su tabla de equivalencias, y se le pasa ya construida al motor.
     */
    @Test
    void testCreateGame_WithRoom() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("88888888-8888-8888-8888-888888888888")), UserRole.USER);

        Room room = roomService.getByRoomname("tg:-100002");
        Game game = cahService.createGame(room);

        Assertions.assertNotNull(game);
        Assertions.assertNotNull(game.getId());
        Assertions.assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), game.getRoom().getId());
        Assertions.assertEquals("Second", game.getRoom().getName());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getCreator().getId());
        Assertions.assertEquals(UUID.fromString("88888888-8888-8888-8888-888888888888"), game.getPlayers().get(0).getUser().getId());
    }

    @Test
    void testSetVotationMode() {
        Game game = cahService.setVotationMode(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), VotationModeEnum.DICTATORSHIP);

        Assertions.assertEquals(VotationModeEnum.DICTATORSHIP, game.getVotationMode());
    }

    @Test
    void testSetVotationMode_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.setVotationMode(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), VotationModeEnum.DICTATORSHIP));
    }

    @Test
    void testSetVotationMode_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.setVotationMode(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), VotationModeEnum.DICTATORSHIP));
    }

    @Test
    void testSetMaxNumberOfPlayers() {
        Game game = cahService.setMaxNumberOfPlayers(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), 6);

        Assertions.assertEquals(6, game.getMaxNumberOfPlayers());
    }

    @Test
    void testSetMaxNumberOfPlayers_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.setMaxNumberOfPlayers(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), 6));
    }

    @Test
    void testSetMaxNumberOfPlayers_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.setMaxNumberOfPlayers(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), 6));
    }

    @Test
    void testSetNumberOfPointsToWin() {
        Game game = cahService.setNumberOfPointsToWin(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), 6);

        Assertions.assertEquals(6, game.getNumberOfPointsToWin());
    }

    @Test
    void testSetNumberOfPointsToWin_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.setNumberOfPointsToWin(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), 6));
    }

    @Test
    void testSetNumberOfPointsToWin_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.setNumberOfPointsToWin(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), 6));
    }

    @Test
    void testSetNumberOfRoundsToEnd() {
        Game game = cahService.setNumberOfRoundsToEnd(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), 6);

        Assertions.assertEquals(6, game.getNumberOfRoundsToEnd());
    }

    @Test
    void testSetNumberOfRoundsToEnd_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.setNumberOfRoundsToEnd(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), 6));
    }

    @Test
    void testSetNumberOfRoundsToEnd_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.setNumberOfRoundsToEnd(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), 6));
    }

    @Test
    void testSetDictionary() {
        Dictionary dictionary = dictionaryService.getDictionaryById(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        Game game = cahService.setDictionary(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), dictionary);

        Assertions.assertEquals(dictionary.getId(), game.getDictionary().getId());
    }

    @Test
    void testSetDictionary_GameDoesntExistsException() {
        Dictionary dictionary = dictionaryService.getDictionaryById(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.setDictionary(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), dictionary));
    }

    @Test
    void testSetDictionary_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Dictionary dictionary = dictionaryService.getDictionaryById(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.setDictionary(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), dictionary));
    }

    @Test
    void testDeleteGameByCreator() {
        Game game = cahService.deleteGameByCreator(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertNotNull(game);
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), game.getCreator().getId());
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), game.getRoom().getId());
    }

    @Test
    void testDeleteGameByCreator_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.deleteGameByCreator(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111"))));
    }

    @Test
    void testDeleteGameByCreator_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.deleteGameByCreator(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    /**
     * Quien administra el bot tiene comandos para borrar la partida de otro y para borrarlas todas.
     * Si la comprobación del creador no le dejara pasar, esos comandos no podrían existir: rechazaban
     * partida por partida, y el de borrarlas todas además en silencio, porque su bucle registra el
     * error y sigue con la siguiente.
     */
    @Test
    void testDeleteGameByCreator_AnAdminCanDeleteSomeoneElsesGame() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.ADMIN);

        Game game = cahService.deleteGameByCreator(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertNotNull(game);
        Assertions.assertNull(gameService.getByRoom(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testAddPlayer() {
        cahService.setMaxNumberOfPlayers(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), 4);

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        Game game = cahService.addPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertNotNull(game);
        Assertions.assertEquals(4, game.getPlayers().size());
    }

    @Test
    void testAddPlayer_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.addPlayer(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111"))));
    }

    @Test
    void testKickPlayer() {
        Game game = cahService.kickPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")));

        Assertions.assertNotNull(game);
        Assertions.assertEquals(2, game.getPlayers().size());
    }

    @Test
    void testKickPlayer_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.kickPlayer(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), userService.getById(UUID.fromString("33333333-3333-3333-3333-333333333333"))));
    }

    @Test
    void testKickPlayer_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.kickPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), userService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testKickPlayer_GameCreatorCannotLeaveException() {
        Assertions.assertThrows(GameCreatorCannotLeaveException.class, () -> cahService.kickPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), userService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testKickPlayer_PlayerDoesntExistsException() {
        Assertions.assertThrows(PlayerDoesntExistsException.class, () -> cahService.kickPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444"))));
    }

    @Test
    void testLeavePlayer() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Game game = cahService.leavePlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertNotNull(game);
        Assertions.assertEquals(2, game.getPlayers().size());
    }

    @Test
    void testLeavePlayer_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.leavePlayer(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111"))));
    }

    @Test
    void testLeavePlayer_GameCreatorCannotLeaveException() {
        Assertions.assertThrows(GameCreatorCannotLeaveException.class, () -> cahService.leavePlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testLeavePlayer_PlayerDoesntExistsException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        Assertions.assertThrows(PlayerDoesntExistsException.class, () -> cahService.leavePlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testVoteForDeletion() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        gameService.setStatus(gameService.getByRoom(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))), GameStatusEnum.STARTED);

        Game game = cahService.voteForDeletion(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertNotNull(game);
        Assertions.assertEquals(3, game.getPlayers().size());
        Assertions.assertEquals(1, game.getDeletionVotes().size());
        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());
    }

    @Test
    void testVoteForDeletion_Ending() {
        gameService.setStatus(gameService.getByRoom(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))), GameStatusEnum.STARTED);

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        cahService.voteForDeletion(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("33333333-3333-3333-3333-333333333333")), UserRole.USER);

        Game game = cahService.voteForDeletion(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertNotNull(game);
        Assertions.assertEquals(3, game.getPlayers().size());
        Assertions.assertEquals(2, game.getDeletionVotes().size());
        Assertions.assertEquals(GameStatusEnum.DELETING, game.getStatus());
    }

    @Test
    void testVoteForDeletion_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.voteForDeletion(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111"))));
    }

    @Test
    void testVoteForDeletion_PlayerDoesntExistsException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        Assertions.assertThrows(PlayerDoesntExistsException.class, () -> cahService.voteForDeletion(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testStartGame() {
        Game game = cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertNotNull(game);
        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());
        Assertions.assertEquals(2, game.getBlackCardsDeck().size());
        Assertions.assertEquals(6, game.getWhiteCardsDeck().size());
        Assertions.assertEquals(3, game.getPlayers().get(0).getHand().size());
    }

    @Test
    void testStartGame_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.startGame(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111"))));
    }

    @Test
    void testStartGame_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testPlayCard() {
        Game game = cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        game = cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        Assertions.assertNotNull(game);
        Assertions.assertEquals(1, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(1).getHand().size());
    }

    @Test
    void testPlayCard_Voting() {
        Game game = cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        game = cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("33333333-3333-3333-3333-333333333333")), UserRole.USER);

        game = cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        Assertions.assertNotNull(game);
        Assertions.assertEquals(2, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());
    }

    @Test
    void testPlayCard_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.playCard(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), cardService.getCardById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testPlayCard_GameNotStartedException() {
        Assertions.assertThrows(GameNotStartedException.class, () -> cahService.playCard(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), cardService.getCardById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testPlayCard_PlayerDoesntExistsException() {
        cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("99999999-9999-9999-9999-999999999999")), UserRole.USER);

        Assertions.assertThrows(PlayerDoesntExistsException.class, () -> cahService.playCard(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), cardService.getCardById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testPlayCard_RoundPresidentCannotPlayCardException() {
        cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertThrows(RoundPresidentCannotPlayCardException.class, () -> cahService.playCard(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), cardService.getCardById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testVoteCard() {
        Game game = cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("33333333-3333-3333-3333-333333333333")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(1, game.getCurrentRound().getVotedCards().size());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
    }

    @Test
    void testVoteCard_GameDoesntExistsException() {
        Assertions.assertThrows(GameDoesntExistsException.class, () -> cahService.voteCard(roomService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), cardService.getCardById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testVoteCard_GameNotStartedException() {
        Assertions.assertThrows(GameNotStartedException.class, () -> cahService.voteCard(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), cardService.getCardById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testVoteCard_PlayerDoesntExistsException() {
        cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("99999999-9999-9999-9999-999999999999")), UserRole.USER);

        Assertions.assertThrows(PlayerDoesntExistsException.class, () -> cahService.voteCard(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), cardService.getCardById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testNextRound() {
        Game game = cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("33333333-3333-3333-3333-333333333333")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(0, game.getCurrentRound().getRoundNumber());

        cahService.nextRound(game);

        Assertions.assertEquals(1, game.getCurrentRound().getRoundNumber());
    }

    @Test
    void testNextRound_GameNotStarted() {
        Assertions.assertThrows(GameNotStartedException.class, () -> cahService.nextRound(gameService.getByRoom(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")))));
    }

    @Test
    void testNextRound_RoundWrongStatusException() {
        Game game = cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertThrows(RoundWrongStatusException.class, () -> cahService.nextRound(game));
    }

    @Test
    void completeClassicGameTest() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        Game game = cahService.createGame("Classic Game");

        cahService.setNumberOfRoundsToEnd(game.getRoom(), 2);

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.addPlayer(game.getRoom());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.addPlayer(game.getRoom());

        Assertions.assertEquals(3, game.getPlayers().size());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.startGame(game.getRoom());

        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());
        Assertions.assertEquals(0, game.getCurrentRound().getRoundNumber());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        Assertions.assertEquals(2, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(1, game.getCurrentRound().getVotedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());

        cahService.nextRound(game);

        Assertions.assertEquals(1, game.getCurrentRound().getRoundNumber());
        Assertions.assertEquals(3, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(3, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.PLAYING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(0).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        Assertions.assertEquals(2, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(1, game.getCurrentRound().getVotedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(0).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
        Assertions.assertEquals(GameStatusEnum.ENDING, game.getStatus());

        Player winner = cahService.getWinner(game);

        Assertions.assertNotNull(winner);
        Assertions.assertEquals(UUID.fromString("66666666-6666-6666-6666-666666666666"), winner.getUser().getId());

        // Todo el test va en una sola transacción, con cartas repartidas y jugadas en la misma: si
        // alguna carta jugada se quedara en player_hand_card, borrar la partida violaría la FK. Y la
        // sesión no es la del creador, que no hace falta para cerrar una partida terminada
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);
        Room room = game.getRoom();
        cahService.endGame(game);
        getCurrentSession().flush();

        Assertions.assertNull(gameService.getByRoom(room));
    }

    @Test
    void testEndGame_GameNotEndingException() {
        Game game = cahService.startGame(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")));

        Assertions.assertThrows(GameNotEndingException.class, () -> cahService.endGame(game));
    }

    @Test
    void completeDictatorshipGameTest() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        Game game = cahService.createGame("Classic Game");

        cahService.setVotationMode(game.getRoom(), VotationModeEnum.DICTATORSHIP);
        cahService.setNumberOfRoundsToEnd(game.getRoom(), 2);

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.addPlayer(game.getRoom());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.addPlayer(game.getRoom());

        Assertions.assertEquals(3, game.getPlayers().size());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.startGame(game.getRoom());

        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());
        Assertions.assertEquals(0, game.getCurrentRound().getRoundNumber());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        Assertions.assertEquals(2, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(1, game.getCurrentRound().getVotedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());

        cahService.nextRound(game);

        Assertions.assertEquals(1, game.getCurrentRound().getRoundNumber());
        Assertions.assertEquals(3, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(3, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.PLAYING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        Assertions.assertEquals(2, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(1, game.getCurrentRound().getVotedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
        Assertions.assertEquals(GameStatusEnum.ENDING, game.getStatus());

        Player winner = cahService.getWinner(game);

        Assertions.assertNotNull(winner);
        Assertions.assertEquals(UUID.fromString("66666666-6666-6666-6666-666666666666"), winner.getUser().getId());
    }

    @Test
    void completeDemocracyGameTest() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        Game game = cahService.createGame("Classic Game");

        cahService.setVotationMode(game.getRoom(), VotationModeEnum.DEMOCRACY);
        cahService.setNumberOfPointsToWin(game.getRoom(), 2);

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.addPlayer(game.getRoom());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.addPlayer(game.getRoom());

        Assertions.assertEquals(3, game.getPlayers().size());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.startGame(game.getRoom());

        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());
        Assertions.assertEquals(0, game.getCurrentRound().getRoundNumber());

        cahService.playCard(game.getRoom(), game.getPlayers().get(0).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        Assertions.assertEquals(3, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(3, game.getCurrentRound().getVotedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(0).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
        Assertions.assertEquals(GameStatusEnum.STARTED, game.getStatus());

        cahService.nextRound(game);

        Assertions.assertEquals(1, game.getCurrentRound().getRoundNumber());
        Assertions.assertEquals(3, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(3, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.PLAYING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(0).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(1).getHand().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.playCard(game.getRoom(), game.getPlayers().get(2).getHand().get(0).getCard());

        Assertions.assertEquals(3, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("44444444-4444-4444-4444-444444444444")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("55555555-5555-5555-5555-555555555555")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(0).getCard());

        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("66666666-6666-6666-6666-666666666666")), UserRole.USER);

        cahService.voteCard(game.getRoom(), game.getCurrentRound().getPlayedCards().get(1).getCard());

        Assertions.assertEquals(3, game.getCurrentRound().getVotedCards().size());
        Assertions.assertEquals(2, game.getPlayers().get(0).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(1).getHand().size());
        Assertions.assertEquals(2, game.getPlayers().get(2).getHand().size());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
        Assertions.assertEquals(GameStatusEnum.ENDING, game.getStatus());

        Player winner = cahService.getWinner(game);

        Assertions.assertNotNull(winner);
        Assertions.assertEquals(UUID.fromString("55555555-5555-5555-5555-555555555555"), winner.getUser().getId());
    }

    // ///////////// Jugadores IA //////////////////

    private static final UUID CREATOR = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID HUMAN = UUID.fromString("55555555-5555-5555-5555-555555555555");

    /**
     * Partida nueva con el creador, otro humano y {@code aiPlayers} IAs, con el modo y las rondas
     * indicados. La sesión se queda con el creador.
     */
    private Game createGameWithAIPlayers(VotationModeEnum mode, int rounds, int aiPlayers) {
        loginAs(CREATOR);
        Game game = cahService.createGame("AI Game");
        cahService.setVotationMode(game.getRoom(), mode);
        cahService.setNumberOfRoundsToEnd(game.getRoom(), rounds);

        loginAs(HUMAN);
        cahService.addPlayer(game.getRoom());

        loginAs(CREATOR);
        for (int i = 1; i <= aiPlayers; i++) {
            cahService.addAIPlayer(game.getRoom(), "IA " + i);
        }

        return game;
    }

    private void loginAs(UUID userId) {
        SecurityUtils.setUserDetails(userService.getById(userId), UserRole.USER);
    }

    private Player playerOf(Game game, UUID userId) {
        return game.getPlayers().stream().filter(p -> p.getUser().getId().equals(userId)).findFirst().orElseThrow();
    }

    private Player aiPlayerOf(Game game) {
        return game.getPlayers().stream().filter(Player::isAi).findFirst().orElseThrow();
    }

    /**
     * Juega la primera carta de la mano del humano indicado.
     */
    private void humanPlays(Game game, UUID userId) {
        loginAs(userId);
        cahService.playCard(game.getRoom(), playerOf(game, userId).getHand().get(0).getCard());
    }

    /**
     * Vota la primera carta jugada que no sea la suya.
     */
    private void humanVotes(Game game, UUID userId) {
        loginAs(userId);
        Player player = playerOf(game, userId);
        PlayedCard target = game.getCurrentRound().getPlayedCards().stream().filter(pc -> !pc.getPlayer().getId().equals(player.getId())).findFirst().orElseThrow();
        cahService.voteCard(game.getRoom(), target.getCard());
    }

    private boolean aiPlayedThisRound(Game game) {
        Player ai = aiPlayerOf(game);
        return game.getCurrentRound().getPlayedCards().stream().anyMatch(pc -> pc.getPlayer().getId().equals(ai.getId()));
    }

    @Test
    void testAddAIPlayer() {
        Game game = createGameWithAIPlayers(VotationModeEnum.CLASSIC, 1, 1);

        Assertions.assertEquals(3, game.getPlayers().size());

        Player ai = aiPlayerOf(game);
        Assertions.assertTrue(ai.isAi());
        Assertions.assertEquals(2, ai.getJoinOrder());
        Assertions.assertTrue(ai.getUser().getUsername().startsWith("ai:"));
        Assertions.assertEquals("IA 1", ai.getUser().getName());
        Assertions.assertEquals(game.getCreator().getLang().getId(), ai.getUser().getLang().getId());
        Assertions.assertFalse(playerOf(game, HUMAN).isAi());
    }

    @Test
    void testAddAIPlayer_GameOnlyCreatorCanPerformActionException() {
        SecurityUtils.setUserDetails(userService.getById(UUID.fromString("11111111-1111-1111-1111-111111111111")), UserRole.USER);

        Assertions.assertThrows(GameOnlyCreatorCanPerformActionException.class, () -> cahService.addAIPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), "IA 1"));
    }

    /**
     * La partida de los datos de prueba ya tiene sus tres plazas ocupadas: una IA también ocupa plaza.
     */
    @Test
    void testAddAIPlayer_GameAlreadyFilledException() {
        Assertions.assertThrows(GameAlreadyFilledException.class, () -> cahService.addAIPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), "IA 1"));
    }

    @Test
    void testAddAIPlayer_EmptyName() {
        Assertions.assertThrows(ApplicationException.class, () -> cahService.addAIPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000")), " "));
    }

    @Test
    void testRemoveAIPlayer_RemovesTheLastOneAndItsUser() {
        Game game = createGameWithAIPlayers(VotationModeEnum.CLASSIC, 1, 2);
        Player lastAi = game.getPlayers().stream().filter(Player::isAi).filter(p -> p.getJoinOrder() == 3).findFirst().orElseThrow();
        String lastAiUsername = lastAi.getUser().getUsername();

        game = cahService.removeAIPlayer(game.getRoom());
        getCurrentSession().flush();

        Assertions.assertEquals(3, game.getPlayers().size());
        Assertions.assertEquals(1, game.getPlayers().stream().filter(Player::isAi).count());
        Assertions.assertEquals("IA 1", aiPlayerOf(game).getUser().getName());
        Assertions.assertThrows(UserDoesntExistsException.class, () -> userService.getByUsername(lastAiUsername));
    }

    @Test
    void testRemoveAIPlayer_AIPlayerDoesntExistsException() {
        Assertions.assertThrows(AIPlayerDoesntExistsException.class, () -> cahService.removeAIPlayer(roomService.getById(UUID.fromString("00000000-0000-0000-0000-000000000000"))));
    }

    @Test
    void testKickPlayer_AIPlayerDeletesItsUser() {
        Game game = createGameWithAIPlayers(VotationModeEnum.CLASSIC, 1, 1);
        org.themarioga.commons.engine.models.User aiUser = aiPlayerOf(game).getUser();
        String aiUsername = aiUser.getUsername();

        cahService.kickPlayer(game.getRoom(), aiUser);
        getCurrentSession().flush();

        Assertions.assertThrows(UserDoesntExistsException.class, () -> userService.getByUsername(aiUsername));
    }

    @Test
    void testDeleteGameByCreator_DeletesTheAIUsers() {
        Game game = createGameWithAIPlayers(VotationModeEnum.CLASSIC, 1, 1);
        String aiUsername = aiPlayerOf(game).getUser().getUsername();

        cahService.deleteGameByCreator(game.getRoom());
        getCurrentSession().flush();

        Assertions.assertThrows(UserDoesntExistsException.class, () -> userService.getByUsername(aiUsername));
        Assertions.assertNotNull(userService.getByUsername("fifth"), "los humanos no se tocan");
    }

    @Test
    void testStartGame_GameNotEnoughHumansException() {
        loginAs(CREATOR);
        Game game = cahService.createGame("AI Game");
        cahService.addAIPlayer(game.getRoom(), "IA 1");
        cahService.addAIPlayer(game.getRoom(), "IA 2");

        Assertions.assertThrows(GameNotEnoughHumansException.class, () -> cahService.startGame(game.getRoom()));
    }

    /**
     * En CLASSIC la presidencia rota por orden de llegada, así que en tres rondas pasa por el
     * creador, el otro humano y la IA. Cuando le toca a la IA, elige ganadora en cuanto juega el
     * último humano y la ronda se cierra sola.
     */
    @Test
    void completeClassicGameWithAIPlayerTest() {
        Game game = createGameWithAIPlayers(VotationModeEnum.CLASSIC, 3, 1);

        cahService.startGame(game.getRoom());

        // Ronda 0: preside el creador. La IA ya ha jugado al empezar
        Assertions.assertEquals(CREATOR, game.getCurrentRound().getRoundPresident().getUser().getId());
        Assertions.assertTrue(aiPlayedThisRound(game));
        Assertions.assertEquals(1, game.getCurrentRound().getPlayedCards().size());
        Assertions.assertEquals(2, aiPlayerOf(game).getHand().size());

        humanPlays(game, HUMAN);
        Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());
        Assertions.assertTrue(game.getCurrentRound().getVotedCards().isEmpty(), "la IA no preside: no vota");

        humanVotes(game, CREATOR);
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());

        cahService.nextRound(game);

        // Ronda 1: preside el otro humano
        Assertions.assertEquals(HUMAN, game.getCurrentRound().getRoundPresident().getUser().getId());
        Assertions.assertTrue(aiPlayedThisRound(game));
        Assertions.assertEquals(2, aiPlayerOf(game).getHand().size(), "la mano de la IA se rellena como la de todos");

        humanPlays(game, CREATOR);
        humanVotes(game, HUMAN);
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());

        cahService.nextRound(game);

        // Ronda 2: preside la IA, que no juega
        Assertions.assertTrue(game.getCurrentRound().getRoundPresident().isAi());
        Assertions.assertTrue(game.getCurrentRound().getPlayedCards().isEmpty());

        humanPlays(game, CREATOR);
        Assertions.assertEquals(RoundStatusEnum.PLAYING, game.getCurrentRound().getStatus());

        humanPlays(game, HUMAN);

        // La IA ha votado nada más abrirse la votación, y con eso la ronda y la partida han acabado
        Assertions.assertEquals(1, game.getCurrentRound().getVotedCards().size());
        Assertions.assertTrue(game.getCurrentRound().getVotedCards().get(0).getPlayer().isAi());
        Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());
        Assertions.assertEquals(GameStatusEnum.ENDING, game.getStatus());

        Assertions.assertEquals(3, game.getPlayers().stream().mapToInt(Player::getPoints).sum());
        Assertions.assertNotNull(cahService.getWinner(game));

        // La última ronda la ha cerrado la IA con la jugada del otro humano: la partida se borra
        // sin que la sesión sea la del creador
        String aiUsername = aiPlayerOf(game).getUser().getUsername();
        Room room = game.getRoom();
        loginAs(HUMAN);
        cahService.endGame(game);
        getCurrentSession().flush();

        Assertions.assertNull(gameService.getByRoom(room));
        Assertions.assertThrows(UserDoesntExistsException.class, () -> userService.getByUsername(aiUsername));
    }

    /**
     * En DICTATORSHIP preside siempre el creador: la IA juega, pero no vota nunca.
     */
    @Test
    void completeDictatorshipGameWithAIPlayerTest() {
        Game game = createGameWithAIPlayers(VotationModeEnum.DICTATORSHIP, 2, 1);

        cahService.startGame(game.getRoom());

        for (int round = 0; round < 2; round++) {
            Assertions.assertTrue(aiPlayedThisRound(game));

            humanPlays(game, HUMAN);
            Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());
            Assertions.assertTrue(game.getCurrentRound().getVotedCards().isEmpty());

            humanVotes(game, CREATOR);
            Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());

            if (round == 0) cahService.nextRound(game);
        }

        Assertions.assertEquals(GameStatusEnum.ENDING, game.getStatus());
    }

    /**
     * En DEMOCRACY la IA juega al empezar la ronda y vota al abrirse la votación; los humanos
     * siempre son los últimos y son ellos los que cierran cada fase.
     */
    @Test
    void completeDemocracyGameWithAIPlayerTest() {
        Game game = createGameWithAIPlayers(VotationModeEnum.DEMOCRACY, 2, 1);

        cahService.startGame(game.getRoom());

        for (int round = 0; round < 2; round++) {
            Assertions.assertTrue(aiPlayedThisRound(game));

            humanPlays(game, CREATOR);
            humanPlays(game, HUMAN);
            Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

            // La IA ya ha votado, y no a su propia carta
            Player ai = aiPlayerOf(game);
            VotedCard aiVote = game.getCurrentRound().getVotedCards().stream().filter(vc -> vc.getPlayer().getId().equals(ai.getId())).findFirst().orElseThrow();
            PlayedCard aiCard = game.getCurrentRound().getPlayedCards().stream().filter(pc -> pc.getPlayer().getId().equals(ai.getId())).findFirst().orElseThrow();
            Assertions.assertNotEquals(aiCard.getCard().getId(), aiVote.getCard().getId());
            Assertions.assertEquals(1, game.getCurrentRound().getVotedCards().size());

            humanVotes(game, CREATOR);
            Assertions.assertEquals(RoundStatusEnum.VOTING, game.getCurrentRound().getStatus());

            humanVotes(game, HUMAN);
            Assertions.assertEquals(3, game.getCurrentRound().getVotedCards().size());
            Assertions.assertEquals(RoundStatusEnum.ENDING, game.getCurrentRound().getStatus());

            if (round == 0) cahService.nextRound(game);
        }

        Assertions.assertEquals(GameStatusEnum.ENDING, game.getStatus());
    }

}
