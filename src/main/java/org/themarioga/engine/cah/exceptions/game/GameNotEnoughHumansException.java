package org.themarioga.engine.cah.exceptions.game;

import org.themarioga.engine.cah.enums.CAHErrorEnum;
import org.themarioga.engine.cah.exceptions.CAHApplicationException;

public class GameNotEnoughHumansException extends CAHApplicationException {

    public GameNotEnoughHumansException() {
        super(CAHErrorEnum.GAME_NOT_ENOUGH_HUMANS);
    }

}
