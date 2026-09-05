package cn.qijiv.domain.activity.service.product;

import cn.qijiv.domain.activity.model.entity.SkuProductEntity;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import cn.qijiv.domain.activity.service.IRaffleActivitySkuProductService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author qijiv
 * @date 2026/09/05
 */
@Service
public class RaffleActivitySkuProductService implements IRaffleActivitySkuProductService {

    @Resource
    private IActivityRepository repository;

    /**
     * 根据活动id查询sku商品列表
     *
     * @param activityId 活动id
     * @return sku商品列表
     */
    @Override
    public List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId) {
        return repository.querySkuProductEntityListByActivityId(activityId);
    }

}

