package org.themarioga.engine.cah.dao.impl.game;

import org.springframework.stereotype.Repository;
import org.themarioga.commons.engine.dao.AbstractHibernateDao;
import org.themarioga.engine.cah.dao.intf.game.RoundResultDao;
import org.themarioga.engine.cah.models.game.RoundResult;

import java.util.List;
import java.util.UUID;

@Repository
public class RoundResultDaoImpl extends AbstractHibernateDao<RoundResult> implements RoundResultDao {

    public RoundResultDaoImpl() {
        setClazz(RoundResult.class);
    }

    @Override
    public List<RoundResult> findByBlackCardId(UUID blackCardId) {
        return getCurrentSession().createQuery("SELECT r FROM RoundResult r WHERE r.blackCardId = :blackCardId", RoundResult.class).setParameter("blackCardId", blackCardId).getResultList();
    }

}
