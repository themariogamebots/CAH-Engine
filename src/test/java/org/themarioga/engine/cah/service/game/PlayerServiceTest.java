package org.themarioga.engine.cah.service.game;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.themarioga.engine.cah.dao.intf.game.PlayerDao;
import org.themarioga.engine.cah.exceptions.player.PlayerCannotPlayCardException;
import org.themarioga.engine.cah.models.dictionaries.Card;
import org.themarioga.engine.cah.models.game.Game;
import org.themarioga.engine.cah.models.game.Player;
import org.themarioga.engine.cah.models.game.PlayerHandCard;
import org.themarioga.engine.cah.services.impl.game.PlayerServiceImpl;
import org.themarioga.commons.engine.exceptions.player.PlayerAlreadyExistsException;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.engine.services.intf.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @InjectMocks
    private PlayerServiceImpl playerService;

    @Mock
    private PlayerDao playerDao;

    @Mock
    private UserService userService;

    private Player player;
    private Game game;
    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        player = new Player();
        player.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        player.setGame(game);
        player.setUser(user);
        player.setPoints(0);

        card = new Card();
        card.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        java.util.Date now = new java.util.Date();
        game.setCreationDate(now);
        user.setCreationDate(now);
        player.setCreationDate(now);
        card.setCreationDate(now);
    }

    @Test
    void testCreate() {
        when(playerDao.findPlayerByUser(user)).thenReturn(null);
        when(playerDao.createOrUpdate(any(Player.class))).thenAnswer(i -> {
            Player p = i.getArgument(0);
            p.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return p;
        });

        Player createdPlayer = playerService.create(game, user);

        Assertions.assertNotNull(createdPlayer);
        Assertions.assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), createdPlayer.getId());
        Assertions.assertEquals(game, createdPlayer.getGame());
        Assertions.assertEquals(user, createdPlayer.getUser());
        verify(playerDao).createOrUpdate(any(Player.class));
    }

    @Test
    void testCreate_Duplicated() {
        when(playerDao.findPlayerByUser(user)).thenReturn(player);

        Assertions.assertThrows(PlayerAlreadyExistsException.class, () -> playerService.create(game, user));
    }

    @Test
    void testDelete() {
        doNothing().when(playerDao).delete(player);

        playerService.delete(player);

        verify(playerDao).delete(player);
    }

    @Test
    void testInsertWhiteCardsIntoPlayerHand() {
        List<Card> cards = new ArrayList<>();
        cards.add(card);

        playerService.insertWhiteCardsIntoPlayerHand(player, cards);

        Assertions.assertEquals(1, player.getHand().size());
        verify(playerDao).create(player);
    }

    @Test
    void testIncrementPoints() {
        when(playerDao.createOrUpdate(player)).thenReturn(player);

        Player updatedPlayer = playerService.incrementPoints(player);

        Assertions.assertEquals(1, updatedPlayer.getPoints());
    }

    @Test
    void testRemoveCardFromHand() {
        PlayerHandCard playerHandCard = new PlayerHandCard();
        playerHandCard.setPlayer(player);
        playerHandCard.setCard(card);
        player.getHand().add(playerHandCard);

        EntityManager entityManager = mock(EntityManager.class);
        when(playerDao.getEntityManager()).thenReturn(entityManager);
        when(playerDao.createOrUpdate(player)).thenReturn(player);

        Player updatedPlayer = playerService.removeCardFromHand(player, card);

        Assertions.assertEquals(0, updatedPlayer.getHand().size());
        verify(entityManager).remove(playerHandCard);
    }

    @Test
    void testRemoveCardFromHand_NonExistentCard() {
        Assertions.assertThrows(PlayerCannotPlayCardException.class, () -> playerService.removeCardFromHand(player, card));
    }

    @Test
    void testFindPlayerById() {
        when(playerDao.findOne(player.getId())).thenReturn(player);

        Player foundPlayer = playerService.findById(player.getId());

        Assertions.assertEquals(player.getId(), foundPlayer.getId());
    }

    @Test
    void testFindPlayerByUser() {
        when(playerDao.findPlayerByUser(user)).thenReturn(player);

        Player foundPlayer = playerService.findByUser(user);

        Assertions.assertEquals(player.getId(), foundPlayer.getId());
    }

    @Test
    void testFindPlayerByUserId() {
        when(userService.getById(user.getId())).thenReturn(user);
        when(playerDao.findPlayerByUser(user)).thenReturn(player);

        Player foundPlayer = playerService.findByUserId(user.getId());

        Assertions.assertEquals(player.getId(), foundPlayer.getId());
    }

    @Test
    void testFindPlayerByGameAndUser() {
        when(playerDao.findPlayerByUserAndGame(user, game)).thenReturn(player);

        Player foundPlayer = playerService.findPlayerByGameAndUser(game, user);

        Assertions.assertEquals(player.getId(), foundPlayer.getId());
    }

}
