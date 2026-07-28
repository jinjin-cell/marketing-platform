package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.repository.IActivityRepository;
import org.springframework.stereotype.Service;

/**
 * 抽奖活动服务
 * 
 * @author qijiv
 * @since 2026/7/18
 */
@Service
public class RaffleActivityService extends AbstractRaffleActivity {

    public RaffleActivityService(IActivityRepository activityRepository) {
        super(activityRepository);
    }

}
