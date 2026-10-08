package org.themarioga.engine.cah.services.intf;

import org.themarioga.engine.cah.enums.VotationModeEnum;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.dictionaries.Dictionary;
import org.themarioga.engine.cah.models.game.Game;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.commons.engine.models.Room;
import org.themarioga.commons.engine.models.User;

public interface CAHService {

    /**
     * Prefijo del username de los usuarios que crea el motor para sus jugadores IA. Las plataformas
     * lo usan para no buscarles un chat que no tienen.
     */
    String AI_USERNAME_PREFIX = "ai:";

    Game createGame(String roomName);

    Game createGame(Room room);

    Game setVotationMode(Room room, VotationModeEnum type);

    Game setMaxNumberOfPlayers(Room room, int maxNumberOfPlayers);

    Game setNumberOfPointsToWin(Room room, int numberOfCards);

    Game setNumberOfRoundsToEnd(Room room, int numberOfRoundsToEnd);

    Game setDictionary(Room room, Dictionary dictionary);

    Game deleteGameByCreator(Room room);

    Game addPlayer(Room room);

    Game addAIPlayer(Room room, String name);

    Game removeAIPlayer(Room room);

    Game kickPlayer(Room room, User userKicked);

    Game leavePlayer(Room room);

    Game voteForDeletion(Room room);

    Game startGame(Room room);

    Game playCard(Room room, Card card);

    Game voteCard(Room room, Card card);

    Game nextRound(Game game);

    Player getWinner(Game game);

    void endGame(Game game);
}
