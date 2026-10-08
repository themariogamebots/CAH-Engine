package org.themarioga.engine.cah.enums;

import org.themarioga.commons.engine.enums.ErrorEnum;

public enum CAHErrorEnum implements ErrorEnum {
    CARD_NOT_FOUND(30L, "Carta no encontrada"),
    CARD_ALREADY_EXISTS(31L, "Carta ya existente"),
    CARD_TEXT_TOO_LONG(32L, "Texto de la carta demasiado largo"),
    CARD_ALREADY_PLAYED(33L, "Carta ya jugada"),
    CARD_ALREADY_VOTED(34L, "Carta ya votada"),
    DICTIONARY_NOT_FOUND(35L, "Diccionario no encontrado"),
    DICTIONARY_NOT_FILLED(36L, "Diccionario no completado"),
    DICTIONARY_ALREADY_EXISTS(37L, "Diccionario ya existe"),
    DICTIONARY_ALREADY_FILLED(38L, "Diccionario ya completado"),
    DICTIONARY_ALREADY_SHARED(39L, "Diccionario ya compartido"),
    DICTIONARY_NOT_YOURS(40L, "Diccionario no encontrado"),
    DICTIONARY_COLLAB_ALREADY_EXISTS(41L, "Colaborador ya existente"),
    DICTIONARY_COLLAB_NOT_FOUND(42L, "Colaborador no encontrado"),
    DICTIONARY_COLLAB_CREATOR_CANT_BE_ALTERED(43L, "Colaborador es creador"),
    PLAYER_CANNOT_DRAW_CARD(44L, "El jugador no puede sacar esa carta"),
    PLAYER_CANNOT_PLAY_CARD(45L, "El jugador no puede jugar esa carta"),
    PLAYER_CANNOT_VOTE_CARD(46L, "El jugador no puede votar"),
    PLAYER_ALREADY_PLAYED_CARD(47L, "El jugador no puede jugar esa carta"),
    PLAYER_ALREADY_VOTED_CARD(48L, "El jugador no puede votar"),
    ROUND_NOT_FOUND(49L, "La ronda no se ha iniciado"),
    ROUND_NOT_STARTED(50L, "La ronda no se ha iniciado"),
    ROUND_NOT_ENDING(51L, "La ronda no ha acabado"),
    ROUND_WRONG_STATUS(52L, "Mesa en estado incorrecto"),
    ROUND_PRESIDENT_CANNOT_PLAY_CARD(53L, "El presidente de la ronda no puede jugar una carta"),
    DICTIONARY_ALREADY_PUBLISHED(54L, "Diccionario ya publicado"),
    DICTIONARY_NOT_PUBLISHED(55L, "Diccionario no publicado"),
    DICTIONARY_MAX_COLLABORATORS_REACHED(56L, "Número máximo de colaboradores alcanzado"),
    CARD_NOT_PLAYED(57L, "Carta no jugada"),
    AI_PLAYER_NOT_FOUND(58L, "No hay ningún jugador IA"),
    GAME_NOT_ENOUGH_HUMANS(59L, "No hay suficientes jugadores humanos");

    final Long errorCode;
    final String errorDesc;

    CAHErrorEnum(Long errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    public Long getErrorCode() {
        return errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public static CAHErrorEnum getByCode(Long errorCode) {
        for (CAHErrorEnum CAHErrorEnum : values()) {
            if (CAHErrorEnum.getErrorCode().equals(errorCode)) return CAHErrorEnum;
        }

        return null;
    }

    public static CAHErrorEnum getByDesc(String errorDesc) {
        for (CAHErrorEnum CAHErrorEnum : values()) {
            if (CAHErrorEnum.getErrorDesc().equals(errorDesc)) return CAHErrorEnum;
        }

        return null;
    }

}

