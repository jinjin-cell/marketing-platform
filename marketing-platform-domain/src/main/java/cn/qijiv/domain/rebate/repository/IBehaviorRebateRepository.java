package cn.qijiv.domain.rebate.repository;

import cn.qijiv.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.qijiv.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.qijiv.domain.rebate.model.valobj.DailyBehaviorRebateVO;

import java.util.List;

/**
 * 行为返利仓储接口
 *
 * @author qijiv
 * @since 2026-08-26
 */
public interface IBehaviorRebateRepository {

    /**
     * 查询已开启的行为返利配置
     *
     * @param behaviorTypeVO 行为类型
     * @return 返利配置列表
     */
    List<DailyBehaviorRebateVO> queryDailyBehaviorRebateConfig(BehaviorTypeVO behaviorTypeVO);

    /**
     * 保存用户返利订单及消息任务
     *
     * @param userId 用户ID
     * @param behaviorRebateAggregates 返利聚合对象列表
     */
    void saveUserRebateRecord(String userId, List<BehaviorRebateAggregate> behaviorRebateAggregates);

}
