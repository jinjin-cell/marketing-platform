package cn.qijiv.trigger.http;

import cn.qijiv.api.IRaffleActivityService;
import cn.qijiv.api.dto.ActivityDrawRequestDTO;
import cn.qijiv.api.dto.ActivityDrawResponseDTO;
import cn.qijiv.api.dto.UserActivityAccountRequestDTO;
import cn.qijiv.api.dto.UserActivityAccountResponseDTO;
import cn.qijiv.domain.activity.model.entity.ActivityAccountEntity;
import cn.qijiv.domain.activity.model.entity.UserRaffleOrderEntity;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.activity.service.IRaffleActivityPartakeService;
import cn.qijiv.domain.activity.service.armory.IActivityArmory;
import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;
import cn.qijiv.domain.award.model.valobj.AwardStateVO;
import cn.qijiv.domain.award.service.IAwardService;
import cn.qijiv.domain.rebate.model.entity.BehaviorEntity;
import cn.qijiv.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.qijiv.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.qijiv.domain.rebate.service.IBehaviorRebateService;
import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.service.IRaffleStrategy;
import cn.qijiv.domain.strategy.service.armory.IStrategyArmory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import cn.qijiv.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/activity/")
public class RaffleActivityController implements IRaffleActivityService {

    @Resource
    private IRaffleActivityPartakeService raffleActivityPartakeService;
    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;
    @Resource
    private IRaffleStrategy raffleStrategy;
    @Resource
    private IAwardService awardService;
    @Resource
    private IActivityArmory activityArmory;
    @Resource
    private IStrategyArmory strategyArmory;
    @Resource
    private IBehaviorRebateService behaviorRebateService;

    private static final DateTimeFormatter DATE_FORMAT_DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 活动装配 - 数据预热，把活动配置的对应的 sku 一起装配。
     *
     * @param activityId 活动ID
     * @return 装配结果
     *
     * <p>接口：{@code /api/v1/raffle/activity/armory}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/armory?activityId=100301'}
     */

    @RequestMapping(value = "armory", method = RequestMethod.GET)
    @Override
    public Response<Boolean> armory(@RequestParam Long activityId) {
        try {
            log.info("活动装配，数据预热开始，活动ID：{}", activityId);
            // 0. 参数校验
            if (null == activityId) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 1. 活动装配
            boolean activityAssembled = activityArmory.assembleActivitySkuByActivityId(activityId);
            // 2. 策略装配
            boolean strategyAssembled = strategyArmory.assembleLotteryStrategyByActivityId(activityId);
            if (!activityAssembled || !strategyAssembled) {
                log.warn("活动装配，数据预热失败，活动ID：{}，活动库存装配结果：{}，策略装配结果：{}", activityId, activityAssembled, strategyAssembled);
                return Response.<Boolean>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info(ResponseCode.UN_ERROR.getInfo())
                        .data(false)
                        .build();
            }
            Response<Boolean> response = Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
            log.info("活动装配，数据预热完成，活动ID：{}", activityId);
            return response;
        } catch (AppException e) {
            log.warn("活动装配，数据预热参数错误，活动ID：{}，错误信息：{}", activityId, e.getInfo());
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("活动装配，数据预热失败，活动ID：{}", activityId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 抽奖接口。
     *
     * @param request 请求对象
     * @return 抽奖结果
     *
     * <p>接口：{@code /api/v1/raffle/activity/draw}
     * <br>示例：{@code curl --request POST --url http://localhost:8091/api/v1/raffle/activity/draw}
     */
    @RequestMapping(value = "draw", method = RequestMethod.POST)
    @Override
    public Response<ActivityDrawResponseDTO> draw(@RequestBody ActivityDrawRequestDTO request) {
        try {
            log.info("活动抽奖开始，用户ID：{}，活动ID：{}", request.getUserId(), request.getActivityId());
            // 1. 参数校验
            if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 参与活动 - 创建参与记录订单
            UserRaffleOrderEntity orderEntity = raffleActivityPartakeService.createOrder(request.getUserId(), request.getActivityId());
            log.info("活动抽奖创建订单，用户ID：{}，活动ID：{}，订单ID：{}", request.getUserId(), request.getActivityId(), orderEntity.getOrderId());
            // 3. 抽奖策略 - 执行抽奖
            RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                    .userId(orderEntity.getUserId())
                    .strategyId(orderEntity.getStrategyId())
                    .endDateTime(orderEntity.getEndDateTime())
                    .build());
            // 4. 存放结果 - 写入中奖记录
            UserAwardRecordEntity userAwardRecord = UserAwardRecordEntity.builder()
                    .userId(orderEntity.getUserId())
                    .activityId(orderEntity.getActivityId())
                    .strategyId(orderEntity.getStrategyId())
                    .orderId(orderEntity.getOrderId())
                    .awardId(raffleAwardEntity.getAwardId())
                    .awardTitle(raffleAwardEntity.getAwardTitle())
                    .awardTime(new Date())
                    .awardState(AwardStateVO.create)
                    .build();
            awardService.saveUserAwardRecord(userAwardRecord);
            // 5. 返回结果
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(ActivityDrawResponseDTO.builder()
                            .awardId(raffleAwardEntity.getAwardId())
                            .awardTitle(raffleAwardEntity.getAwardTitle())
                            .awardIndex(raffleAwardEntity.getSort())
                            .build())
                    .build();
        } catch (AppException e) {
            log.error("活动抽奖失败，用户ID：{}，活动ID：{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("活动抽奖失败，用户ID：{}，活动ID：{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }


    /**
     * 日历签到返利接口
     *
     * @param userId 用户ID
     * @return 签到返利结果
     * <p>
     * 接口：<a href="http://localhost:8091/api/v1/raffle/activity/calendar_sign_rebate">/api/v1/raffle/activity/calendar_sign_rebate</a>
     * 入参：xiaofuge
     * <p>
     * curl -X POST <a href="http://localhost:8091/api/v1/raffle/activity/calendar_sign_rebate">...</a> -d "userId=xiaofuge" -H "Content-Type: application/x-www-form-urlencoded"
     */
    @RequestMapping(value = "calendar_sign_rebate", method = RequestMethod.POST)
    @Override
    public Response<Boolean> calendarSignRebate(@RequestParam String userId) {
        try {
            log.info("日历签到返利开始，用户ID：{}", userId);
            if (StringUtils.isBlank(userId)) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            BehaviorEntity behaviorEntity = new BehaviorEntity();
            behaviorEntity.setUserId(userId);
            behaviorEntity.setBehaviorTypeVO(BehaviorTypeVO.SIGN);
            behaviorEntity.setOutBusinessNo(LocalDate.now().format(DATE_FORMAT_DAY));
            List<String> orderIds = behaviorRebateService.createOrder(behaviorEntity);
            log.info("日历签到返利完成，用户ID：{}，订单ID列表：{}", userId, JSON.toJSONString(orderIds));
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch (AppException e) {
            log.error("日历签到返利异常，用户ID：{}", userId, e);
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("日历签到返利失败，用户ID：{}", userId);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    /**
     * 判断是否签到接口
     * <p>
     * curl -X POST <a href="http://localhost:8091/api/v1/raffle/activity/is_calendar_sign_rebate">...</a> -d "userId=xiaofuge" -H "Content-Type: application/x-www-form-urlencoded"
     */
    @RequestMapping(value = "is_calendar_sign_rebate", method = RequestMethod.POST)
    @Override
    public Response<Boolean> isCalendarSignRebate(@RequestParam String userId) {
        try {
            log.info("查询用户是否完成日历签到返利开始，用户ID：{}", userId);
            if (StringUtils.isBlank(userId)) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            String outBusinessNo = LocalDate.now().format(DATE_FORMAT_DAY);
            List<BehaviorRebateOrderEntity> behaviorRebateOrderEntities = behaviorRebateService.queryOrderByOutBusinessNo(userId, outBusinessNo);
            log.info("查询用户是否完成日历签到返利完成，用户ID：{}，订单数量：{}", userId, behaviorRebateOrderEntities.size());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(!behaviorRebateOrderEntities.isEmpty()) // 只要不为空，则表示已经做了签到
                    .build();
        } catch (Exception e) {
            log.error("查询用户是否完成日历签到返利失败，用户ID：{}", userId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    /**
     * 查询账户额度
     * <p>
     * curl --request POST \
     * --url <a href="http://localhost:8091/api/v1/raffle/activity/query_user_activity_account">...</a> \
     * --header 'content-type: application/json' \
     * --data '{
     * "userId":"xiaofuge",
     * "activityId": 100301
     * }'
     */
    @RequestMapping(value = "query_user_activity_account", method = RequestMethod.POST)
    @Override
    public Response<UserActivityAccountResponseDTO> queryUserActivityAccount(@RequestBody UserActivityAccountRequestDTO request) {
        String userId = request == null ? null : request.getUserId();
        Long activityId = request == null ? null : request.getActivityId();
        try {
            log.info("查询用户活动账户开始，用户ID：{}，活动ID：{}", userId, activityId);
            // 1. 参数校验
            if (StringUtils.isBlank(userId) || activityId == null) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            ActivityAccountEntity activityAccountEntity = raffleActivityAccountQuotaService.queryActivityAccountEntity(activityId, userId);
            if (activityAccountEntity == null) {
                // 未充值过额度的用户也返回稳定结构，前端无需额外处理 null。
                activityAccountEntity = ActivityAccountEntity.builder()
                        .totalCount(0).totalCountSurplus(0).dayCount(0).dayCountSurplus(0)
                        .monthCount(0).monthCountSurplus(0).build();
            }
            UserActivityAccountResponseDTO userActivityAccountResponseDTO = UserActivityAccountResponseDTO.builder()
                    .totalCount(activityAccountEntity.getTotalCount())
                    .totalCountSurplus(activityAccountEntity.getTotalCountSurplus())
                    .dayCount(activityAccountEntity.getDayCount())
                    .dayCountSurplus(activityAccountEntity.getDayCountSurplus())
                    .monthCount(activityAccountEntity.getMonthCount())
                    .monthCountSurplus(activityAccountEntity.getMonthCountSurplus())
                    .build();
            log.info("查询用户活动账户完成，用户ID：{}，活动ID：{}，响应数据：{}", userId, activityId, JSON.toJSONString(userActivityAccountResponseDTO));
            return Response.<UserActivityAccountResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(userActivityAccountResponseDTO)
                    .build();
        } catch (AppException e) {
            log.warn("查询用户活动账户参数错误，用户ID：{}，活动ID：{}", userId, activityId);
            return Response.<UserActivityAccountResponseDTO>builder().code(e.getCode()).info(e.getInfo()).build();
        } catch (Exception e) {
            log.error("查询用户活动账户失败，用户ID：{}，活动ID：{}", userId, activityId, e);
            return Response.<UserActivityAccountResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }




}
