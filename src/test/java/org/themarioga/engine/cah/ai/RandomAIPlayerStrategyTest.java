package org.themarioga.engine.cah.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.PlayedCard;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.engine.cah.models.game.PlayerHandCard;
import org.themarioga.engine.cah.models.game.Round;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

class RandomAIPlayerStrategyTest {

    private RandomAIPlayerStrategy strategy;
    private Round round;
    private Player ai;
    private Player human;

    @BeforeEach
    void setUp() {
        strategy = new RandomAIPlayerStrategy(new Random(42));

        round = new Round();
        ai = player();
        human = player();
    }

    @Test
    void playsACardFromItsHand() {
        Set<UUID> hand = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Card card = card();
            hand.add(card.getId());

            PlayerHandCard handCard = new PlayerHandCard();
            handCard.setPlayer(ai);
            handCard.setCard(card);
            ai.getHand().add(handCard);
        }

        Set<UUID> chosen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            chosen.add(strategy.chooseCardToPlay(round, ai).getId());
        }

        Assertions.assertTrue(hand.containsAll(chosen));
        Assertions.assertEquals(hand, chosen, "con 200 intentos tiene que haber salido cada carta alguna vez");
    }

    @Test
    void playsNothingWithAnEmptyHand() {
        Assertions.assertNull(strategy.chooseCardToPlay(round, ai));
    }

    @Test
    void neverVotesItsOwnCard() {
        Card own = played(ai);
        Card other1 = played(human);
        Card other2 = played(player());

        Set<UUID> chosen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            chosen.add(strategy.chooseCardToVote(round, ai).getId());
        }

        Assertions.assertFalse(chosen.contains(own.getId()));
        Assertions.assertEquals(Set.of(other1.getId(), other2.getId()), chosen);
    }

    @Test
    void votesNothingWhenOnlyItsOwnCardWasPlayed() {
        played(ai);

        Assertions.assertNull(strategy.chooseCardToVote(round, ai));
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

    private static Player player() {
        Player player = new Player();
        player.setId(UUID.randomUUID());
        return player;
    }

    private static Card card() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        return card;
    }

}
