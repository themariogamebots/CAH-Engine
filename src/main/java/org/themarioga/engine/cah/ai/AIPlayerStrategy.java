package org.themarioga.engine.cah.ai;

import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.engine.cah.models.game.Round;

/**
 * Cerebro de los jugadores IA: decide qué carta juegan y cuál votan. El motor se encarga de
 * llamarlo en el momento adecuado y de aplicar las reglas; la estrategia solo elige.
 */
public interface AIPlayerStrategy {

    /**
     * Carta de la mano de {@code ai} que juega en {@code round}.
     */
    Card chooseCardToPlay(Round round, Player ai);

    /**
     * Carta jugada en {@code round} que vota {@code ai}. Nunca la suya.
     */
    Card chooseCardToVote(Round round, Player ai);

}
