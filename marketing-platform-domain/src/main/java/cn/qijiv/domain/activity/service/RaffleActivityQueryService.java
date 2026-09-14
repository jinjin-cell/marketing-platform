package cn.qijiv.domain.activity.service;

import cn.qijiv.domain.activity.model.entity.ActivityEntity;
import cn.qijiv.domain.activity.model.entity.ActivityOrderEntity;
import cn.qijiv.domain.activity.repository.IActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 活动查询服务实现
 *
 * @author qijiv
 * @since 2026/09/14
 */
@Slf4j
@Service
public class RaffleActivityQueryService implements IRaffleActivityQueryService {

    @Resource
    private IActivityRepository activityRepository;

    @Override
    public List<ActivityEntity> queryActivityList() {
        return activityRepository.queryActivityList();
    }

    @Override
    public List<ActivityOrderEntity> queryActivityOrderList(String userId) {
        if (StringUtils.isBlank(userId)) {
            return new ArrayList<>();
        }
        return activityRepository.queryActivityOrderList(userId);
    }
}
