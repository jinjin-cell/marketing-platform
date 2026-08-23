package cn.qijiv.domain.award.model.aggregate;

import cn.qijiv.domain.award.model.entity.TaskEntity;
import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户获奖记录聚合实体
 *
 * @author qijiv
 * @since 2026-08-19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAwardRecordAggregate {

    private UserAwardRecordEntity userAwardRecordEntity;

    private TaskEntity taskEntity;

}

