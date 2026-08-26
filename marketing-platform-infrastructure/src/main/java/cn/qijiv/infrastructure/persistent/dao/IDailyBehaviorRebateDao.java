package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.DailyBehaviorRebatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 日常行为返利配置数据访问层
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Mapper
public interface IDailyBehaviorRebateDao {

    /**
     * 按行为类型查询已开启的返利配置
     *
     * @param behaviorType 行为类型
     * @return 返利配置列表
     */
    List<DailyBehaviorRebatePO> queryDailyBehaviorRebateByBehaviorType(
            @Param("behaviorType") String behaviorType);

}
