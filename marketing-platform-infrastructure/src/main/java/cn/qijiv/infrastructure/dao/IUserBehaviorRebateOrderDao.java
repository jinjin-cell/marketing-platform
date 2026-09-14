package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.db.annotation.DBRouter;
import cn.qijiv.infrastructure.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.dao.po.UserBehaviorRebateOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户行为返利订单数据访问层
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserBehaviorRebateOrderDao {

    /**
     * 新增用户行为返利订单
     *
     * @param userBehaviorRebateOrder 用户行为返利订单
     */
    void insert(UserBehaviorRebateOrderPO userBehaviorRebateOrder);

    @DBRouter
    List<UserBehaviorRebateOrderPO> queryOrderByOutBusinessNo(UserBehaviorRebateOrderPO request);

    /**
     * 查询指定日期区间内已完成的行为返利日期（按日期升序去重）
     *
     * @param userId       用户ID
     * @param behaviorType 行为类型
     * @param beginDate    起始日期 yyyy-MM-dd（含）
     * @param endDate      结束日期 yyyy-MM-dd（含）
     * @return 日期列表 yyyy-MM-dd
     */
    @DBRouter
    List<String> queryBehaviorDates(@Param("userId") String userId,
                                    @Param("behaviorType") String behaviorType,
                                    @Param("beginDate") String beginDate,
                                    @Param("endDate") String endDate);

}
