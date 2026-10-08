package org.themarioga.engine.cah.service.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.themarioga.engine.cah.dao.intf.game.RoundDao;
import org.themarioga.engine.cah.enums.CardTypeEnum;
import org.themarioga.engine.cah.enums.RoundStatusEnum;
import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.exceptions.card.CardAlreadyPlayedException;
import org.themarioga.engine.cah.exceptions.card.CardDoesntExistsException;
import org.themarioga.engine.cah.exceptions.card.CardNotPlayedException;
import org.themarioga.engine.cah.exceptions.player.PlayerAlreadyPlayedCardException;
import org.themarioga.engine.cah.exceptions.player.PlayerAlreadyVotedCardException;
import org.themarioga.engine.cah.exceptions.player.PlayerCannotVoteCardException;
import org.themarioga.engine.cah.exceptions.round.RoundWrongStatusException;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.*;
import org.themarioga.engine.cah.services.impl.game.RoundServiceImpl;
import org.themarioga.engine.cah.services.intf.game.PlayerService;
import org.themarioga.commons.engine.models.User;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

    @InjectMocks
    private RoundServiceImpl roundService;

    @Mock
    private RoundDao roundDao;

    @Mock
    private PlayerService playerService;

    private Round round;
    private Game game;
    private Player player;
    private Player player2;
    private User creator;
    private User otherUser;
    private Card blackCard;
    private Card whiteCard;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        otherUser = new User();
        otherUser.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        game = new Game();
        game.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        game.setCreator(creator);
        game.setVotationMode(VotationModeEnum.CLASSIC);

        player = new Player();
        player.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        player.setUser(creator);
        player.setGame(game);
        player.setJoinOrder(0);

        player2 = new Player();
        player2.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        player2.setUser(otherUser);
        player2.setGame(game);
        player2.setJoinOrder(1);

        game.getPlayers().add(player);
        game.getPlayers().add(player2);

        blackCard = new Card();
        blackCard.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        blackCard.setType(CardTypeEnum.BLACK);
        game.getBlackCardsDeck().add(blackCard);

        whiteCard = new Card();
        whiteCard.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        whiteCard.setType(CardTypeEnum.WHITE);

        round = new Round();
        round.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        round.setGame(game);
        round.setRoundNumber(0);
        round.setStatus(RoundStatusEnum.PLAYING);
        round.setRoundPresident(player);
        game.setCurrentRound(round);

        java.util.Date now = new java.util.Date();
        creator.setCreationDate(now);
        otherUser.setCreationDate(now);
        game.setCreationDate(now);
        player.setCreationDate(now);
        player2.setCreationDate(now);
        blackCard.setCreationDate(now);
        whiteCard.setCreationDate(now);
        round.setCreationDate(now);
    }

    @Test
    void testCreateRound_Classic() {
        when(roundDao.createOrUpdate(any(Round.class))).thenAnswer(i -> i.getArgument(0));

        Round newRound = roundService.createRound(game, 0);

        Assertions.assertNotNull(newRound);
        Assertions.assertEquals(0, newRound.getRoundNumber());
        Assertions.assertNotNull(newRound.getRoundPresident());
        Assertions.assertEquals(player.getId(), newRound.getRoundPresident().getId());
    }

    @Test
    void testCreateRound_Democracy() {
        game.setVotationMode(VotationModeEnum.DEMOCRACY);
        when(roundDao.createOrUpdate(any(Round.class))).thenAnswer(i -> i.getArgument(0));

        Round newRound = roundService.createRound(game, 1);

        Assertions.assertNotNull(newRound);
        Assertions.assertEquals(1, newRound.getRoundNumber());
        Assertions.assertNull(newRound.getRoundPresident());
    }

    @Test
    void testCreateRound_Dictatorship() {
        game.setVotationMode(VotationModeEnum.DICTATORSHIP);
        when(playerService.findPlayerByGameAndUser(game, creator)).thenReturn(player);
        when(roundDao.createOrUpdate(any(Round.class))).thenAnswer(i -> i.getArgument(0));

        Round newRound = roundService.createRound(game, 0);

        Assertions.assertNotNull(newRound);
        Assertions.assertEquals(0, newRound.getRoundNumber());
        Assertions.assertNotNull(newRound.getRoundPresident());
    }

    @Test
    void testDeleteRound() {
        round.setStatus(RoundStatusEnum.ENDING);
        doNothing().when(roundDao).delete(round);
        when(roundDao.getCurrentSession()).thenReturn(mock(org.hibernate.Session.class));

        roundService.deleteRound(round);

        verify(roundDao).delete(round);
    }

    @Test
    void testDeleteRound_NotEnding() {
        round.setStatus(RoundStatusEnum.PLAYING);

        Assertions.assertThrows(RoundWrongStatusException.class, () -> roundService.deleteRound(round));
    }

    @Test
    void testSetRound() {
        when(roundDao.createOrUpdate(round)).thenReturn(round);

        Round updatedRound = roundService.setStatus(round, RoundStatusEnum.VOTING);

        Assertions.assertEquals(RoundStatusEnum.VOTING, updatedRound.getStatus());
    }

    @Test
    void testAddCardToPlayedCards() {
        when(roundDao.createOrUpdate(round)).thenReturn(round);

        Round updatedRound = roundService.addCardToPlayedCards(round, player, whiteCard);

        Assertions.assertEquals(1, updatedRound.getPlayedCards().size());
    }

    @Test
    void testAddCardToPlayedCards_WrongStatus() {
        round.setStatus(RoundStatusEnum.VOTING);

        Assertions.assertThrows(RoundWrongStatusException.class, () -> roundService.addCardToPlayedCards(round, player, whiteCard));
    }

    @Test
    void testAddCardToPlayedCards_PlayerAlreadyPlayed() {
        PlayedCard playedCard = new PlayedCard();
        playedCard.setPlayer(player);
        playedCard.setCard(new Card());
        round.getPlayedCards().add(playedCard);

        Assertions.assertThrows(PlayerAlreadyPlayedCardException.class, () -> roundService.addCardToPlayedCards(round, player, whiteCard));
    }

    @Test
    void testAddCardToPlayedCards_CardAlreadyPlayed() {
        PlayedCard playedCard = new PlayedCard();
        playedCard.setPlayer(player2);
        playedCard.setCard(whiteCard);
        round.getPlayedCards().add(playedCard);

        Assertions.assertThrows(CardAlreadyPlayedException.class, () -> roundService.addCardToPlayedCards(round, player, whiteCard));
    }

    @Test
    void testVoteCard() {
        round.setStatus(RoundStatusEnum.VOTING);
        PlayedCard playedCard = new PlayedCard();
        playedCard.setPlayer(player2);
        playedCard.setCard(whiteCard);
        round.getPlayedCards().add(playedCard);
        when(roundDao.createOrUpdate(round)).thenReturn(round);

        Round updatedRound = roundService.voteCard(round, player, whiteCard);

        Assertions.assertEquals(1, updatedRound.getVotedCards().size());
    }

    @Test
    void testVoteCard_WrongStatus() {
        round.setStatus(RoundStatusEnum.PLAYING);

        Assertions.assertThrows(RoundWrongStatusException.class, () -> roundService.voteCard(round, player, whiteCard));
    }

    @Test
    void testVoteCard_CannotVoteCard() {
        round.setStatus(RoundStatusEnum.VOTING);

        Assertions.assertThrows(PlayerCannotVoteCardException.class, () -> roundService.voteCard(round, player2, whiteCard));
    }

    @Test
    void testVoteCard_AlreadyVotedCard() {
        round.setStatus(RoundStatusEnum.VOTING);
        PlayedCard playedCard = new PlayedCard();
        playedCard.setPlayer(player2);
        playedCard.setCard(whiteCard);
        round.getPlayedCards().add(playedCard);

        VotedCard votedCard = new VotedCard();
        votedCard.setPlayer(player);
        votedCard.setCard(whiteCard);
        round.getVotedCards().add(votedCard);

        Assertions.assertThrows(PlayerAlreadyVotedCardException.class, () -> roundService.voteCard(round, player, whiteCard));
    }

    @Test
    void testVoteCard_CardNotPlayed() {
        round.setStatus(RoundStatusEnum.VOTING);

        Assertions.assertThrows(CardNotPlayedException.class, () -> roundService.voteCard(round, player, whiteCard));
    }

    @Test
    void testSetNextBlackCard() {
        when(roundDao.createOrUpdate(round)).thenReturn(round);

        Round updatedRound = roundService.setNextBlackCard(round, blackCard);

        Assertions.assertEquals(blackCard.getId(), updatedRound.getRoundBlackCard().getId());
    }

    @Test
    void testSetNextBlackCard_CardDoesntExists() {
        Assertions.assertThrows(CardDoesntExistsException.class, () -> roundService.setNextBlackCard(round, whiteCard));
    }

    @Test
    void testGetMostVotedCard() {
        when(roundDao.getMostVotedCards(round)).thenReturn(java.util.List.of(whiteCard));

        Card mostVotedCard = roundService.getMostVotedCard(round);

        Assertions.assertEquals(whiteCard, mostVotedCard);
    }

    @Test
    void testGetMostVotedCard_Tie() {
        Card whiteCard2 = new Card();
        whiteCard2.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        whiteCard2.setType(CardTypeEnum.WHITE);

        when(roundDao.getMostVotedCards(round)).thenReturn(java.util.List.of(whiteCard, whiteCard2));

        Card mostVotedCard = roundService.getMostVotedCard(round);

        Assertions.assertTrue(mostVotedCard == whiteCard || mostVotedCard == whiteCard2);
    }

    /**
     * El motor elige ganadora para dar el punto y la plataforma vuelve a preguntar para anunciarla: en
     * un empate, las dos respuestas tienen que coincidir, venga en el orden que venga de la base de datos.
     */
    @Test
    void testGetMostVotedCard_TieIsTheSameForTheSameRound() {
        Card whiteCard2 = new Card();
        whiteCard2.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        whiteCard2.setType(CardTypeEnum.WHITE);
        Card whiteCard3 = new Card();
        whiteCard3.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        whiteCard3.setType(CardTypeEnum.WHITE);

        when(roundDao.getMostVotedCards(round)).thenReturn(java.util.List.of(whiteCard, whiteCard2, whiteCard3), java.util.List.of(whiteCard3, whiteCard, whiteCard2));

        Card first = roundService.getMostVotedCard(round);
        Card second = roundService.getMostVotedCard(round);

        Assertions.assertEquals(first.getId(), second.getId());
    }

    @Test
    void testGetMostVotedCard_NoVotes() {
        when(roundDao.getMostVotedCards(round)).thenReturn(java.util.List.of());

        Card mostVotedCard = roundService.getMostVotedCard(round);

        Assertions.assertNull(mostVotedCard);
    }

    @Test
    void testGetPlayedCardByCard() {
        PlayedCard playedCard = new PlayedCard();
        when(roundDao.getPlayedCardByCard(round, whiteCard)).thenReturn(playedCard);

        PlayedCard returnedPlayedCard = roundService.getPlayedCardByCard(round, whiteCard);

        Assertions.assertNotNull(returnedPlayedCard);
    }

    @Test
    void testGetCheckIfEveryoneHavePlayedACard() {
        when(roundDao.countPlayedCards(round)).thenReturn(1L);

        Assertions.assertTrue(roundService.checkIfEveryoneHavePlayedACard(round));
    }

    @Test
    void testGetCheckIfEveryoneHaveVotedACard() {
        when(roundDao.countVotedCards(round)).thenReturn(1L);

        Assertions.assertTrue(roundService.checkIfEveryoneHaveVotedACard(round));
    }

}
