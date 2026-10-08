package org.themarioga.engine.cah.service.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.themarioga.engine.cah.dao.intf.game.RoundResultDao;
import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.dictionaries.Dictionary;
import org.themarioga.engine.cah.models.game.*;
import org.themarioga.engine.cah.services.impl.game.RoundResultServiceImpl;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundResultServiceTest {

    @InjectMocks
    private RoundResultServiceImpl roundResultService;

    @Mock
    private RoundResultDao roundResultDao;

    private Round round;
    private Card blackCard;
    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary();
        dictionary.setId(UUID.randomUUID());

        Game game = new Game();
        game.setDictionary(dictionary);
        game.setVotationMode(VotationModeEnum.DEMOCRACY);

        blackCard = card();

        round = new Round();
        round.setGame(game);
        round.setRoundBlackCard(blackCard);
    }

    /**
     * Tres cartas: la de la IA se lleva dos votos (uno de un humano y otro de otra IA) y gana;
     * la de un humano, uno; la otra, ninguno. Cada fila cuenta sus votos y cuántos fueron de IA.
     */
    @Test
    void recordsOneRowPerPlayedCard() {
        Player human1 = player(false);
        Player human2 = player(false);
        Player ai = player(true);
        Player otherAi = player(true);

        Card aiCard = played(ai);
        Card human1Card = played(human1);
        Card human2Card = played(human2);

        voted(human1, aiCard);
        voted(otherAi, aiCard);
        voted(ai, human1Card);

        when(roundResultDao.create(any(RoundResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<RoundResult> results = roundResultService.recordRound(round, aiCard);

        Assertions.assertEquals(3, results.size());
        verify(roundResultDao, times(3)).create(any(RoundResult.class));

        RoundResult winner = resultOf(results, aiCard);
        Assertions.assertTrue(winner.getWon());
        Assertions.assertEquals(2, winner.getVotes());
        Assertions.assertEquals(1, winner.getAiVotes());
        Assertions.assertTrue(winner.getAiPlayer());

        RoundResult second = resultOf(results, human1Card);
        Assertions.assertFalse(second.getWon());
        Assertions.assertEquals(1, second.getVotes());
        Assertions.assertEquals(1, second.getAiVotes());
        Assertions.assertFalse(second.getAiPlayer());

        RoundResult third = resultOf(results, human2Card);
        Assertions.assertEquals(0, third.getVotes());
        Assertions.assertEquals(0, third.getAiVotes());

        for (RoundResult result : results) {
            Assertions.assertEquals(dictionary.getId(), result.getDictionaryId());
            Assertions.assertEquals(blackCard.getId(), result.getBlackCardId());
            Assertions.assertEquals(3, result.getCandidates());
            Assertions.assertEquals(VotationModeEnum.DEMOCRACY, result.getVotationMode());
            Assertions.assertNotNull(result.getCreationDate());
        }
    }

    private RoundResult resultOf(List<RoundResult> results, Card card) {
        return results.stream().filter(result -> result.getWhiteCardId().equals(card.getId())).findFirst().orElseThrow();
    }

    private Card played(Player player) {
        Card card = card();

        PlayedCard playedCard = new PlayedCard();
        playedCard.setRound(round);
        playedCard.setPlayer(player);
        playedCard.setCard(card);
        round.getPlayedCards().add(playedCard);

        return card;
    }

    private void voted(Player player, Card card) {
        VotedCard votedCard = new VotedCard();
        votedCard.setRound(round);
        votedCard.setPlayer(player);
        votedCard.setCard(card);
        round.getVotedCards().add(votedCard);
    }

    private static Player player(boolean ai) {
        Player player = new Player();
        player.setId(UUID.randomUUID());
        player.setAi(ai);
        return player;
    }

    private static Card card() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        return card;
    }

}
