package org.themarioga.engine.cah.dao.intf.game;

import org.themarioga.commons.engine.dao.InterfaceHibernateDao;
import org.themarioga.engine.cah.models.game.RoundResult;

import java.util.List;
import java.util.UUID;

public interface RoundResultDao extends InterfaceHibernateDao<RoundResult> {

    List<RoundResult> findByBlackCardId(UUID blackCardId);

}
