package org.themarioga.engine.cah.models.game;

import jakarta.persistence.*;
import org.themarioga.commons.engine.models.Base;
import org.themarioga.engine.cah.enums.VotationModeEnum;

import java.io.Serializable;
import java.util.UUID;

/**
 * Cómo le fue a una carta blanca en una ronda: una fila por carta jugada, escrita al cerrarse la
 * votación. Es el histórico del que podrá aprender una IA qué cartas suelen ganar con cada negra.
 * <p>
 * Las cartas y el diccionario van como ids sueltos, sin clave ajena: se pueden borrar, y el
 * histórico no tiene por qué perderse ni impedir que se borren.
 */
@Entity
@Table(name = "round_result", indexes = {@Index(columnList = "black_card_id, white_card_id"), @Index(columnList = "white_card_id")})
public class RoundResult extends Base implements Serializable {

    @Column(nullable = false)
    private UUID dictionaryId;

    @Column(nullable = false)
    private UUID blackCardId;

    @Column(nullable = false)
    private UUID whiteCardId;

    /**
     * Votos que recibió la carta, contando los de las IAs.
     */
    @Column(nullable = false)
    private Integer votes;

    /**
     * De esos votos, los que vinieron de una IA: para poder descontarlos al aprender.
     */
    @Column(nullable = false)
    private Integer aiVotes;

    @Column(nullable = false)
    private Boolean won;

    /**
     * Cuántas cartas se jugaron en la ronda: ganar entre dos no vale lo mismo que entre ocho.
     */
    @Column(nullable = false)
    private Integer candidates;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private VotationModeEnum votationMode;

    /**
     * La carta la jugó una IA.
     */
    @Column(nullable = false)
    private Boolean aiPlayer;

    public UUID getDictionaryId() {
        return dictionaryId;
    }

    public void setDictionaryId(UUID dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    public UUID getBlackCardId() {
        return blackCardId;
    }

    public void setBlackCardId(UUID blackCardId) {
        this.blackCardId = blackCardId;
    }

    public UUID getWhiteCardId() {
        return whiteCardId;
    }

    public void setWhiteCardId(UUID whiteCardId) {
        this.whiteCardId = whiteCardId;
    }

    public Integer getVotes() {
        return votes;
    }

    public void setVotes(Integer votes) {
        this.votes = votes;
    }

    public Integer getAiVotes() {
        return aiVotes;
    }

    public void setAiVotes(Integer aiVotes) {
        this.aiVotes = aiVotes;
    }

    public Boolean getWon() {
        return won;
    }

    public void setWon(Boolean won) {
        this.won = won;
    }

    public Integer getCandidates() {
        return candidates;
    }

    public void setCandidates(Integer candidates) {
        this.candidates = candidates;
    }

    public VotationModeEnum getVotationMode() {
        return votationMode;
    }

    public void setVotationMode(VotationModeEnum votationMode) {
        this.votationMode = votationMode;
    }

    public Boolean getAiPlayer() {
        return aiPlayer;
    }

    public void setAiPlayer(Boolean aiPlayer) {
        this.aiPlayer = aiPlayer;
    }

    @Override
    public String toString() {
        return "RoundResult{id=" + getId() + ", blackCardId=" + blackCardId + ", whiteCardId=" + whiteCardId + ", votes=" + votes + ", won=" + won + '}';
    }

}
