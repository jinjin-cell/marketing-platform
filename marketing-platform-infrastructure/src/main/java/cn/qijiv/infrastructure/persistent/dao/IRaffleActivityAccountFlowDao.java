package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.RaffleActivityAccountFlowPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 抽奖活动账户次数流水 DAO
 *
 * @author jinlujia
 * @since 2026-07-28
 */
@Mapper
public interface IRaffleActivityAccountFlowDao {

    int insert(RaffleActivityAccountFlowPO raffleActivityAccountFlow);

    List<RaffleActivityAccountFlowPO> queryRaffleActivityAccountFlowByUserId(
            @Param("userId") String userId);

}
