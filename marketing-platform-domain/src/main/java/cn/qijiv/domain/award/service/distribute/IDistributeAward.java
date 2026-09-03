package cn.qijiv.domain.award.service.distribute;

import cn.qijiv.domain.award.model.entity.DistributeAwardEntity;

/**
 * @author qijiv
 * @since  2026/09/03
 */
public interface IDistributeAward {

    void giveOutPrizes(DistributeAwardEntity distributeAwardEntity);

}

