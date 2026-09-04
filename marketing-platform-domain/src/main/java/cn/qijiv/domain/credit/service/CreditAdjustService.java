package cn.qijiv.domain.credit.service;

import cn.qijiv.domain.credit.model.aggregate.TradeAggregate;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.CreditOrderEntity;
import cn.qijiv.domain.credit.model.entity.TradeEntity;
import cn.qijiv.domain.credit.repository.ICreditRepository;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 积分调额服务【正逆向，增减积分】
 * @author qijiv
 * @since 2026/9/3
 */
@Slf4j
@Service
public class CreditAdjustService implements ICreditAdjustService {

    @Resource
    private ICreditRepository creditRepository;

    @Override
    public String createOrder(TradeEntity tradeEntity) {
        validateTrade(tradeEntity);
        log.info("调整账户积分额度开始 userId:{} tradeName:{} tradeType:{} amount:{}", tradeEntity.getUserId(), tradeEntity.getTradeName(), tradeEntity.getTradeType(), tradeEntity.getAmount());
        // 1. 创建账户积分实体
        CreditAccountEntity creditAccountEntity = TradeAggregate.createCreditAccountEntity(
                tradeEntity.getUserId(),
                tradeEntity.getAmount());

        // 2. 创建账户订单实体
        CreditOrderEntity creditOrderEntity = TradeAggregate.createCreditOrderEntity(
                tradeEntity.getUserId(),
                tradeEntity.getTradeName(),
                tradeEntity.getTradeType(),
                tradeEntity.getAmount(),
                tradeEntity.getOutBusinessNo());

        // 3. 构建交易聚合对象
        TradeAggregate tradeAggregate = TradeAggregate.builder()
                .userId(tradeEntity.getUserId())
                .creditAccountEntity(creditAccountEntity)
                .creditOrderEntity(creditOrderEntity)
                .build();

        // 4. 保存积分交易订单，返回生效订单号（业务号已存在时返回原单号，幂等）
        String orderId = creditRepository.saveUserCreditTradeOrder(tradeAggregate);
        log.info("调整账户积分额度完成 userId:{} orderId:{}", tradeEntity.getUserId(), orderId);

        return orderId;
    }

    private void validateTrade(TradeEntity tradeEntity) {
        if (null == tradeEntity
                || StringUtils.isBlank(tradeEntity.getUserId())
                || null == tradeEntity.getTradeName()
                || null == tradeEntity.getTradeType()
                || null == tradeEntity.getAmount()
                || 0 == tradeEntity.getAmount().signum()
                || StringUtils.isBlank(tradeEntity.getOutBusinessNo())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
    }

}

