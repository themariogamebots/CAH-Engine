package org.themarioga.engine.cah.services.intf.game;

import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.Round;
import org.themarioga.engine.cah.models.game.RoundResult;

import java.util.List;
import java.util.UUID;

public interface RoundResultService {

    List<RoundResult> recordRound(Round round, Card winningCard);

    List<RoundResult> getByBlackCardId(UUID blackCardId);

}
