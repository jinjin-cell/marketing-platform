package cn.qijiv.domain.activity.service.rule.impl;

import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivityCountEntity;
import cn.qijiv.domain.activity.model.entity.ActivitySkuEntity;
import cn.qijiv.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.armory.IActivityDispatch;
import cn.qijiv.domain.activity.service.rule.AbstractActionChain;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 活动商品库存处理操作链接
 * 
 * @author qijiv
 * @since 2026/7/18
 */
@Slf4j
@Component("activity_sku_stock_action")
public class ActivitySkuStockActionChain extends AbstractActionChain {

    @Resource
    private IActivityDispatch activityDispatch;
    @Resource
    private IActivityRepository activityRepository;

    @Override
    public boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        log.info("活动责任链-商品库存处理【有效期、状态、库存(sku)】开始。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());
        // 扣减库存
        boolean status = activityDispatch.subtractionActivitySkuStock(activitySkuEntity.getSku(), activityEntity.getEndDateTime());
        // true；库存扣减成功
        if (status) {
            log.info("活动责任链-商品库存处理【有效期、状态、库存(sku)】成功。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());

            // 写入延迟队列，延迟消费更新库存记录
            activityRepository.activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO.builder()
                    .sku(activitySkuEntity.getSku())
                    .activityId(activityEntity.getActivityId())
                    .build());

            return true;
        }

        throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(), ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());
    }

}

