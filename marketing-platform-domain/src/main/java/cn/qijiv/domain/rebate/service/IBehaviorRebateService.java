package cn.qijiv.domain.rebate.service;

import cn.qijiv.domain.rebate.model.entity.BehaviorEntity;
import cn.qijiv.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.qijiv.domain.rebate.model.valobj.BehaviorTypeVO;

import java.util.List;

/**
 * 行为返利服务接口
 *
 * @author qijiv
 * @since 2026-08-26
 */
public interface IBehaviorRebateService {

    /**
     * 创建行为返利入账订单
     *
     * @param behaviorEntity 用户行为
     * @return 返利订单ID列表
     */
    List<String> createOrder(BehaviorEntity behaviorEntity);

    /**
     * 根据外部业务单号查询返利订单
     *
     * @param userId         用户ID
     * @param outBusinessNo  外部业务单号
     * @return 返利订单列表
     */
    List<BehaviorRebateOrderEntity> queryOrderByOutBusinessNo(String userId, String outBusinessNo);

    /**
     * 查询指定日期区间内已完成的行为返利日期
     *
     * @param userId         用户ID
     * @param behaviorTypeVO 行为类型
     * @param beginDate      起始日期 yyyy-MM-dd（含）
     * @param endDate        结束日期 yyyy-MM-dd（含）
     * @return 日期列表 yyyy-MM-dd，升序去重
     */
    List<String> queryBehaviorDates(String userId, BehaviorTypeVO behaviorTypeVO, String beginDate, String endDate);
}
