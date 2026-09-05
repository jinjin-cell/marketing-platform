package cn.qijiv.domain.activity.service.quota;

import cn.qijiv.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.qijiv.domain.activity.model.entity.*;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.activity.service.quota.policy.ITradePolicy;
import cn.qijiv.domain.activity.service.quota.rule.IActionChain;
import cn.qijiv.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 抽奖活动账户额度抽象类
 */
@Slf4j
public abstract class AbstractRaffleActivityAccountQuota extends RaffleActivityAccountQuotaSupport implements IRaffleActivityAccountQuotaService {


    private final Map<String, ITradePolicy> tradePolicyGroup;
    /**
     * 构造方法注入活动仓库与责任链工厂
     *
     * @param activityRepository 活动仓库
     * @param defaultActivityChainFactory 默认活动责任链工厂
     */
    public AbstractRaffleActivityAccountQuota(IActivityRepository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory, Map<String, ITradePolicy> tradePolicyGroup) {
        super(activityRepository, defaultActivityChainFactory);
        this.tradePolicyGroup = tradePolicyGroup;
    }

    /**
     * 创建SKU账户充值订单；校验参数、幂等处理、查询活动信息、执行责任链校验并保存订单
     *
     * @param skuRechargeEntity 活动商品充值实体对象
     * @return 订单ID
     */
    @Override
    public UnpaidActivityOrderEntity createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity) {
        // 1. 参数校验
        if (null == skuRechargeEntity) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        String userId = skuRechargeEntity.getUserId();
        Long sku = skuRechargeEntity.getSku();
        String outBusinessNo = skuRechargeEntity.getOutBusinessNo();
        if (null == sku || StringUtils.isBlank(userId) || StringUtils.isBlank(outBusinessNo)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 2.1 查询未支付订单「一个月以内的未支付订单」
        UnpaidActivityOrderEntity unpaidCreditOrder =  activityRepository.queryUnpaidActivityOrder(skuRechargeEntity);
        if (null != unpaidCreditOrder) return unpaidCreditOrder;



        // 2.2 幂等校验。Bloom 判定不存在时跳过数据库，判定可能存在时再查询数据库确认。
        ActivityOrderEntity existingOrder = activityRepository.queryActivityOrderByOutBusinessNo(userId, outBusinessNo);
        if (null != existingOrder) {
            if (OrderStateVO.wait_pay == existingOrder.getState()) {
                CreateQuotaOrderAggregate existingOrderAggregate = CreateQuotaOrderAggregate.builder()
                        .userId(existingOrder.getUserId())
                        .activityId(existingOrder.getActivityId())
                        .activityOrderEntity(existingOrder)
                        .build();
                ITradePolicy tradePolicy = tradePolicyGroup.get(skuRechargeEntity.getOrderTradeType().getCode());
                tradePolicy.trade(existingOrderAggregate);
            }
            return UnpaidActivityOrderEntity.builder()
                    .userId(existingOrder.getUserId())
                    .orderId(existingOrder.getOrderId())
                    .outBusinessNo(existingOrder.getOutBusinessNo())
                    .payAmount(existingOrder.getPayAmount())
                    .build();
        }

        // 3. 查询基础信息
        // 3.1 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = queryActivitySku(sku);
        // 3.2 查询活动信息
        ActivityEntity activityEntity = queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        // 3.3 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());

        // 4. 活动动作规则校验 todo 后续处理规则过滤流程，暂时也不处理责任链结果
        IActionChain actionChain = defaultActivityChainFactory.openActionChain();
        actionChain.action(activitySkuEntity, activityEntity, activityCountEntity);

        // 5. 构建订单聚合对象
        CreateQuotaOrderAggregate createQuotaOrderAggregate = buildOrderAggregate(skuRechargeEntity, activitySkuEntity, activityEntity, activityCountEntity);

        // 6. 保存订单
        ITradePolicy tradePolicy = tradePolicyGroup.get(skuRechargeEntity.getOrderTradeType().getCode());
        tradePolicy.trade(createQuotaOrderAggregate);

        // 7. 返回订单信息
        ActivityOrderEntity activityOrderEntity = createQuotaOrderAggregate.getActivityOrderEntity();
        return UnpaidActivityOrderEntity.builder()
                .userId(userId)
                .orderId(activityOrderEntity.getOrderId())
                .outBusinessNo(activityOrderEntity.getOutBusinessNo())
                .payAmount(activityOrderEntity.getPayAmount())
                .build();

    }

    /**
     * 构建活动充值订单聚合对象
     *
     * @param skuRechargeEntity 活动商品充值实体对象
     * @param activitySkuEntity 活动SKU实体
     * @param activityEntity 活动实体
     * @param activityCountEntity 活动次数配置实体
     * @return 创建充值订单聚合对象
     */
    protected abstract CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);


}
