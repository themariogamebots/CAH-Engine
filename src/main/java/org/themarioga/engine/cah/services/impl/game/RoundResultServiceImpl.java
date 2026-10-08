package org.themarioga.engine.cah.services.impl.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.themarioga.commons.engine.exceptions.ApplicationException;
import org.themarioga.commons.engine.util.Assert;
import org.themarioga.engine.cah.dao.intf.game.RoundResultDao;
import org.themarioga.engine.cah.enums.CAHErrorEnum;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.PlayedCard;
import org.themarioga.engine.cah.models.game.Round;
import org.themarioga.engine.cah.models.game.RoundResult;
import org.themarioga.engine.cah.models.game.VotedCard;
import org.themarioga.engine.cah.services.intf.game.RoundResultService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RoundResultServiceImpl implements RoundResultService {

    private final Logger logger = LoggerFactory.getLogger(RoundResultServiceImpl.class);

    private final RoundResultDao roundResultDao;

    @Autowired
    public RoundResultServiceImpl(RoundResultDao roundResultDao) {
        this.roundResultDao = roundResultDao;
    }

    /**
     * Guarda una fila por carta jugada en la ronda. Hay que llamarlo al cerrar la votación: la ronda
     * se borra al pasar a la siguiente y con ella sus cartas y sus votos.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public List<RoundResult> recordRound(Round round, Card winningCard) {
        logger.debug("Recording the results of round {}", round);

        Assert.assertNotNull(round, CAHErrorEnum.ROUND_NOT_FOUND);
        Assert.assertNotNull(winningCard, CAHErrorEnum.CARD_NOT_FOUND);

        Date now = new Date();
        List<RoundResult> results = new ArrayList<>();
        for (PlayedCard playedCard : round.getPlayedCards()) {
            UUID cardId = playedCard.getCard().getId();
            List<VotedCard> votes = round.getVotedCards().stream().filter(votedCard -> Objects.equals(votedCard.getCard().getId(), cardId)).toList();

            RoundResult result = new RoundResult();
            result.setDictionaryId(round.getGame().getDictionary().getId());
            result.setBlackCardId(round.getRoundBlackCard().getId());
            result.setWhiteCardId(cardId);
            result.setVotes(votes.size());
            result.setAiVotes((int) votes.stream().filter(votedCard -> votedCard.getPlayer().isAi()).count());
            result.setWon(Objects.equals(cardId, winningCard.getId()));
            result.setCandidates(round.getPlayedCards().size());
            result.setVotationMode(round.getGame().getVotationMode());
            result.setAiPlayer(playedCard.getPlayer().isAi());
            result.setCreationDate(now);

            results.add(roundResultDao.create(result));
        }

        return results;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<RoundResult> getByBlackCardId(UUID blackCardId) {
        logger.debug("Getting the results of black card {}", blackCardId);

        return roundResultDao.findByBlackCardId(blackCardId);
    }

}
