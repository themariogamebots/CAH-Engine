package org.themarioga.engine.cah.exceptions.player;

import org.themarioga.engine.cah.enums.CAHErrorEnum;
import org.themarioga.engine.cah.exceptions.CAHApplicationException;

public class AIPlayerDoesntExistsException extends CAHApplicationException {

    public AIPlayerDoesntExistsException() {
        super(CAHErrorEnum.AI_PLAYER_NOT_FOUND);
    }

}
