package cn.qijiv.api;


import cn.qijiv.api.dto.ActivityDrawRequestDTO;
import cn.qijiv.api.dto.ActivityDrawResponseDTO;
import cn.qijiv.types.model.Response;

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


}
