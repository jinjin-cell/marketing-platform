package cn.qijiv.trigger.api;


import cn.qijiv.trigger.api.dto.*;
import cn.qijiv.types.model.Response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 抽奖活动接口
 *
 * <p>接口方法由 {@code RaffleActivityController} 以 HTTP 端点形式对外暴露，
 * 不会在代码中直接调用，因此抑制“未使用”检查。
 *
 * @author qijiv
 * @since 2026-08-24
 */
@SuppressWarnings("unused")
public interface IRaffleActivityService {

    /**
     * 活动装配，数据预热缓存
     * @param activityId 活动ID
     * @return 装配结果
     */
    Response<Boolean> armory(Long activityId);

    /**
     * 活动抽奖接口
     * @param request 请求对象
     * @return 返回结果
     */
    Response<ActivityDrawResponseDTO> draw(ActivityDrawRequestDTO request);

    /**
     * 日历签到并发放行为返利。
     *
     * @param userId 用户ID
     * @return 是否受理成功
     */
    Response<Boolean> calendarSignRebate(String userId);

    /**
     * 判断是否完成日历签到返利接口
     *
     * @param userId 用户ID
     * @return 签到结果 true 已签到，false 未签到
     */
    Response<Boolean> isCalendarSignRebate(String userId);

    /**
     * 查询用户活动账户
     *
     * @param request 请求对象「活动ID、用户ID」
     * @return 返回结果「总额度、月额度、日额度」
     */
    Response<UserActivityAccountResponseDTO> queryUserActivityAccount(UserActivityAccountRequestDTO request);


    /**
     * 查询sku商品集合
     *
     * @param activityId 活动ID
     * @return 商品集合
     */
    Response<List<SkuProductResponseDTO>> querySkuProductListByActivityId(Long activityId);

    /**
     * 查询用户积分值
     *
     * @param userId 用户ID
     * @return 可用积分
     */
    Response<BigDecimal> queryUserCreditAccount(String userId);


    /**
     * 使用用户积分兑换活动商品。
     *
     * @param request 用户、SKU 与幂等业务号
     * @return 活动订单号
     */
    Response<Boolean> creditPayExchangeSku(SkuProductShopCartRequestDTO request);



}
