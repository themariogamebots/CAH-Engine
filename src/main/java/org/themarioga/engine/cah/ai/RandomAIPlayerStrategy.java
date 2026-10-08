package org.themarioga.engine.cah.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.PlayedCard;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.engine.cah.models.game.PlayerHandCard;
import org.themarioga.engine.cah.models.game.Round;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Juega y vota al azar. Es la primera estrategia: no sabe nada de qué cartas suelen ganar.
 */
@Component
public class RandomAIPlayerStrategy implements AIPlayerStrategy {

    private final Random random;

    @Autowired
    public RandomAIPlayerStrategy() {
        this(new SecureRandom());
    }

    /**
     * Para los tests, que necesitan una semilla fija.
     */
    RandomAIPlayerStrategy(Random random) {
        this.random = random;
    }

    @Override
    public Card chooseCardToPlay(Round round, Player ai) {
        List<PlayerHandCard> hand = ai.getHand();
        if (hand.isEmpty()) return null;

        return hand.get(random.nextInt(hand.size())).getCard();
    }

    @Override
    public Card chooseCardToVote(Round round, Player ai) {
        List<PlayedCard> candidates = round.getPlayedCards().stream().filter(playedCard -> !Objects.equals(playedCard.getPlayer().getId(), ai.getId())).toList();
        if (candidates.isEmpty()) return null;

        return candidates.get(random.nextInt(candidates.size())).getCard();
    }

}
