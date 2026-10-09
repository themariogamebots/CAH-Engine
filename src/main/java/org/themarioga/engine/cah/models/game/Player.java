package org.themarioga.engine.cah.models.game;

import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Player extends org.themarioga.commons.engine.models.Player implements Serializable {

    @Column(nullable = false)
    private Integer points = 0;

    /**
     * Jugador controlado por el motor en lugar de por una persona. El default de la columna es lo
     * que permite añadirla con partidas en curso (ver la migración V2.1.0_1 de CAH-Telegram).
     */
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean ai = false;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "player", orphanRemoval = true)
    private List<PlayerHandCard> hand = new ArrayList<>(0);

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PlayedCard playedCard;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private VotedCard votedCard;

    /**
     * La partida a la que pertenece, ya con el tipo de CAH: la clase base la declara con el tipo
     * genérico y obligaba a castear en cada consumidor.
     * <p>
     * La asociación es perezosa y apunta a la clase base abstracta, así que si la partida no está ya
     * cargada en la sesión Hibernate devuelve un proxy de la clase base, que no se puede castear: hay
     * que desenvolverlo. Pasa en cuanto un update de Telegram carga primero al jugador.
     */
    @Override
    public Game getGame() {
        return (Game) Hibernate.unproxy(super.getGame());
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Boolean getAi() {
        return ai;
    }

    public void setAi(Boolean ai) {
        this.ai = ai;
    }

    /**
     * Atajo null-safe de {@link #getAi()}.
     */
    public boolean isAi() {
        return Boolean.TRUE.equals(ai);
    }

    public List<PlayerHandCard> getHand() {
        return hand;
    }

    public void setHand(List<PlayerHandCard> hand) {
        this.hand = hand;
    }

    public PlayedCard getPlayedCard() {
        return playedCard;
    }

    public void setPlayedCard(PlayedCard playedCard) {
        this.playedCard = playedCard;
    }

    public VotedCard getVotedCard() {
        return votedCard;
    }

    public void setVotedCard(VotedCard votedCard) {
        this.votedCard = votedCard;
    }

    @Override
    public String toString() {
        return "Player{" + super.toString() + ", points=" + points + ", ai=" + ai + '}';
    }

}
