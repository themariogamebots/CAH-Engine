package org.themarioga.engine.cah.services.impl.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.themarioga.engine.cah.dao.intf.game.RoundDao;
import org.themarioga.engine.cah.enums.CAHErrorEnum;
import org.themarioga.engine.cah.enums.CardTypeEnum;
import org.themarioga.engine.cah.enums.RoundStatusEnum;
import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.exceptions.card.CardAlreadyPlayedException;
import org.themarioga.engine.cah.exceptions.card.CardDoesntExistsException;
import org.themarioga.engine.cah.exceptions.card.CardNotPlayedException;
import org.themarioga.engine.cah.exceptions.round.RoundWrongStatusException;
import org.themarioga.engine.cah.exceptions.player.PlayerAlreadyPlayedCardException;
import org.themarioga.engine.cah.exceptions.player.PlayerAlreadyVotedCardException;
import org.themarioga.engine.cah.exceptions.player.PlayerCannotVoteCardException;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.*;
import org.themarioga.engine.cah.services.intf.game.PlayerService;
import org.themarioga.engine.cah.services.intf.game.RoundService;
import org.themarioga.commons.engine.enums.CommonErrorEnum;
import org.themarioga.commons.engine.exceptions.ApplicationException;
import org.themarioga.commons.engine.util.Assert;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class RoundServiceImpl implements RoundService {

    private final Logger logger = LoggerFactory.getLogger(RoundServiceImpl.class);

    private final RoundDao roundDao;
    private final PlayerService playerService;

    private final Random random = new SecureRandom();

    @Autowired
    public RoundServiceImpl(RoundDao roundDao, PlayerService playerService) {
        this.roundDao = roundDao;
        this.playerService = playerService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Round createRound(Game game, int roundNumber) {
        logger.debug("Creating round for game {}", game);

        // Check round exists
        Assert.assertNotNull(game, CommonErrorEnum.GAME_NOT_FOUND);

        // Create the round object
        Round round = new Round();
        round.setGame(game);
        round.setRoundNumber(roundNumber);
        round.setStatus(RoundStatusEnum.PLAYING);
        round.setCreationDate(new Date());

        // Set current black card
        round.setRoundBlackCard(getBlackCardFromGameDeck(game));

        // Set current president if needed
        if (game.getVotationMode() == VotationModeEnum.DICTATORSHIP) {
            round.setRoundPresident(playerService.findPlayerByGameAndUser(game, game.getCreator()));
        } else if (round.getGame().getVotationMode() == VotationModeEnum.CLASSIC) {
            round.setRoundPresident(getPresidentForNextRound(round));
        }

        return roundDao.createOrUpdate(round);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void deleteRound(Round round) {
        logger.debug("Deleting round {}", round);

        // Check round exists
        Assert.assertNotNull(round, CAHErrorEnum.ROUND_NOT_FOUND);

        // Check if the round is ready to end
        if (round.getStatus() != RoundStatusEnum.ENDING)
            throw new RoundWrongStatusException();

        roundDao.delete(round);
        roundDao.getCurrentSession().flush();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Round setStatus(Round round, RoundStatusEnum status) {
        logger.debug("Setting round {} to status {}", round, status);

        // Check round exists
        Assert.assertNotNull(round, CommonErrorEnum.GAME_NOT_FOUND);

        // Set status
        round.setStatus(status);

        return roundDao.createOrUpdate(round);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Round addCardToPlayedCards(Round round, Player player, Card card) {
        logger.debug("Player {} playing card {} for the round {}", player, card, round);

        // Check round exists
        Assert.assertNotNull(round, CAHErrorEnum.ROUND_NOT_STARTED);

        // Check if the round is ready to start
        if (round.getStatus() != RoundStatusEnum.PLAYING)
            throw new RoundWrongStatusException();

        // Check if the player already played
        if (round.getPlayedCards().stream().anyMatch(playedCard -> playedCard.getPlayer().getId().equals(player.getId())))
            throw new PlayerAlreadyPlayedCardException();

        // Check if the card was already played
        if (round.getPlayedCards().stream().anyMatch(playedCard -> playedCard.getCard().getId().equals(card.getId())))
            throw new CardAlreadyPlayedException();

        // Set the played card
        PlayedCard playedCard = new PlayedCard();
        playedCard.setRound(round);
        playedCard.setPlayer(player);
        playedCard.setCard(card);
        round.getPlayedCards().add(playedCard);

        return roundDao.createOrUpdate(round);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Round voteCard(Round round, Player player, Card card) {
        logger.debug("Player {} voting card {} for the round {}", player, card, round);

        // Check round exists
        Assert.assertNotNull(round, CommonErrorEnum.GAME_NOT_FOUND);

        // Check if the round is ready to start
        if (round.getStatus() != RoundStatusEnum.VOTING)
            throw new RoundWrongStatusException();

        // Check if the player can vote
        if ((round.getGame().getVotationMode().equals(VotationModeEnum.DICTATORSHIP) || round.getGame().getVotationMode().equals(VotationModeEnum.CLASSIC)) && !player.getId().equals(round.getRoundPresident().getId()))
            throw new PlayerCannotVoteCardException();

        // Check if the player already voted
        if (round.getVotedCards().stream().anyMatch(votedCard -> votedCard.getPlayer().getId().equals(player.getId())))
            throw new PlayerAlreadyVotedCardException();

        // Check if the card have been played this round
        if (round.getPlayedCards().stream().noneMatch(playedCard -> playedCard.getCard().getId().equals(card.getId())))
            throw new CardNotPlayedException();

        // Set the player vote
        VotedCard votedCard = new VotedCard();
        votedCard.setRound(round);
        votedCard.setPlayer(player);
        votedCard.setCard(card);
        round.getVotedCards().add(votedCard);

        return roundDao.createOrUpdate(round);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Round setNextBlackCard(Round round, Card nextBlackCard) {
        logger.debug("Setting next black card to round {}", round);

        // Check the card type is black
        if (nextBlackCard.getType() != CardTypeEnum.BLACK)
            throw new CardDoesntExistsException();

        // Set the round black card
        round.setRoundBlackCard(nextBlackCard);

        return roundDao.createOrUpdate(round);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Card getMostVotedCard(Round round) {
        logger.debug("Getting most voted card of the game of the round {}", round);

        List<Card> mostVotedCards = roundDao.getMostVotedCards(round);

        if (mostVotedCards.isEmpty())
            return null;

        if (mostVotedCards.size() == 1)
            return mostVotedCards.get(0);

        // Break ties at random, but the same way every time for the same round: the engine picks the
        // winner to give the point and the platform asks again to announce it, and two independent
        // draws could announce a card that didn't get the point. Sorting first keeps it independent
        // of the database row order
        List<Card> tied = new ArrayList<>(mostVotedCards);
        tied.sort(Comparator.comparing(Card::getId));

        Random tieBreak = round.getId() != null ? new Random(round.getId().getMostSignificantBits() ^ round.getId().getLeastSignificantBits()) : random;

        return tied.get(tieBreak.nextInt(tied.size()));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PlayedCard getPlayedCardByCard(Round round, Card card) {
        logger.debug("Getting played card from round {} and card {}", round, card);

        return roundDao.getPlayedCardByCard(round, card);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean checkIfEveryoneHavePlayedACard(Round round) {
        int cardsNeededToVote = round.getGame().getPlayers().size();
        if (round.getGame().getVotationMode() == VotationModeEnum.DICTATORSHIP || round.getGame().getVotationMode() == VotationModeEnum.CLASSIC)
            cardsNeededToVote--;

        return roundDao.countPlayedCards(round) == cardsNeededToVote;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean checkIfEveryoneHaveVotedACard(Round round) {
        int votesNeededToEnd = round.getGame().getPlayers().size();
        if (round.getGame().getVotationMode() == VotationModeEnum.DICTATORSHIP || round.getGame().getVotationMode() == VotationModeEnum.CLASSIC)
            votesNeededToEnd = 1;

        return roundDao.countVotedCards(round) == votesNeededToEnd;
    }

    private Card getBlackCardFromGameDeck(Game game) {
        logger.debug("Getting black card from deck to game {}", game);

        // Draw a random card instead of always the first one, since the deck's
        // persisted order is not itself randomized
        List<Card> blackCardsDeck = game.getBlackCardsDeck();
        Card nextBlackCard = blackCardsDeck.get(random.nextInt(blackCardsDeck.size()));
        blackCardsDeck.remove(nextBlackCard);

        return nextBlackCard;
    }

    private Player getPresidentForNextRound(Round round) {
        List<Player> players = new ArrayList<>(round.getGame().getPlayers());
        players.sort(Comparator.comparing(org.themarioga.commons.engine.models.Player::getJoinOrder));

        return players.get(round.getRoundNumber() % players.size());
    }

}
