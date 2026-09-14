package cn.qijiv.trigger.http;

import cn.qijiv.domain.activity.model.entity.*;
import cn.qijiv.domain.activity.model.valobj.ActivityStateVO;
import cn.qijiv.domain.activity.model.valobj.OrderStateVO;
import cn.qijiv.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.activity.service.IRaffleActivityPartakeService;
import cn.qijiv.domain.activity.service.IRaffleActivityQueryService;
import cn.qijiv.domain.activity.service.IRaffleActivitySkuProductService;
import cn.qijiv.domain.activity.service.armory.IActivityArmory;
import cn.qijiv.domain.award.model.entity.UserAwardRecordEntity;
import cn.qijiv.domain.award.model.valobj.AwardStateVO;
import cn.qijiv.domain.award.service.IAwardService;
import cn.qijiv.domain.credit.model.entity.CreditAccountEntity;
import cn.qijiv.domain.credit.model.entity.CreditOrderRecordEntity;
import cn.qijiv.domain.credit.model.entity.TradeEntity;
import cn.qijiv.domain.credit.model.valobj.TradeNameVO;
import cn.qijiv.domain.credit.model.valobj.TradeTypeVO;
import cn.qijiv.domain.credit.service.ICreditAdjustService;
import cn.qijiv.domain.rebate.model.entity.BehaviorEntity;
import cn.qijiv.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.qijiv.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.qijiv.domain.rebate.service.IBehaviorRebateService;
import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.service.IRaffleStrategy;
import cn.qijiv.domain.strategy.service.armory.IStrategyArmory;
import cn.qijiv.trigger.api.IRaffleActivityService;
import cn.qijiv.trigger.api.dto.*;
import cn.qijiv.trigger.security.AuthenticatedUser;
import cn.qijiv.types.annotations.RateLimiterAccessInterceptor;
import cn.qijiv.types.annotations.SentinelGuard;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import cn.qijiv.types.model.Response;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/activity/")
@DubboService(version = "1.0")
public class RaffleActivityController implements IRaffleActivityService {

    @Resource
    private IRaffleActivityPartakeService raffleActivityPartakeService;
    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;
    @Resource
    private IRaffleActivitySkuProductService raffleActivitySkuProductService;
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
    @Resource
    private ICreditAdjustService creditAdjustService;
    @Resource
    private IRaffleActivityQueryService raffleActivityQueryService;

    private static final DateTimeFormatter DATE_FORMAT_DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 活动装配 - 数据预热，把活动配置的对应的 sku 一起装配。
     *
     * @param activityId 活动ID
     * @return 装配结果
     *
     * <p>仅通过 Dubbo/内部服务调用，不暴露公网 HTTP 路由。
     */

    @Override
    public Response<Boolean> armory(Long activityId) {
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
    @RateLimiterAccessInterceptor(key = "userId", fallbackMethod = "drawRateLimiterError", permitsPerSecond = 1.0d, blacklistCount = 10)
    @SentinelGuard("activityDraw")
    @RequestMapping(value = "draw", method = RequestMethod.POST)
    @Override
    public Response<ActivityDrawResponseDTO> draw(@RequestBody ActivityDrawRequestDTO request) {
        String userId = request == null ? null : request.getUserId();
        userId = AuthenticatedUser.resolve(userId);
        if (request != null) request.setUserId(userId);
        Long activityId = request == null ? null : request.getActivityId();
        try {
            // 1. 参数校验
            log.info("活动抽奖开始，用户ID：{}，活动ID：{}", userId, activityId);
            if (request == null || StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
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
                    .awardConfig(raffleAwardEntity.getAwardConfig())
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
                            .awardConfig(raffleAwardEntity.getAwardConfig())
                            .build())
                    .build();
        } catch (AppException e) {
            // 业务校验异常属于预期内分支，记录 WARN 即可，不打印全栈，避免 ERROR 日志噪音；
            // 业务性失败（如额度不足）不应计入熔断，故不做 Tracer.trace
            log.warn("活动抽奖失败(业务校验)，用户ID：{}，活动ID：{}，错误码：{}，错误信息：{}", userId, activityId, e.getCode(), e.getInfo());
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            // 意外异常：计入 Sentinel 熔断统计（供异常比例/异常数降级规则使用）
            try {
                if (ContextUtil.getContext() != null) {
                    Tracer.trace(e);
                }
            } catch (Throwable ignore) {
                // trace 失败不影响主流程
            }
            log.error("活动抽奖失败，用户ID：{}，活动ID：{}", userId, activityId, e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    public Response<ActivityDrawResponseDTO> drawRateLimiterError(@RequestBody ActivityDrawRequestDTO request) {
        if (request != null) request.setUserId(AuthenticatedUser.resolve(request.getUserId()));
        log.info("活动抽奖限流 userId:{} activityId:{}", request.getUserId(), request.getActivityId());
        return Response.<ActivityDrawResponseDTO>builder()
                .code(ResponseCode.RATE_LIMITER.getCode())
                .info(ResponseCode.RATE_LIMITER.getInfo())
                .build();
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
        userId = AuthenticatedUser.resolve(userId);
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
        userId = AuthenticatedUser.resolve(userId);
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
     * 查询指定日期区间内的日历签到记录，供前端渲染整月签到标记。
     *
     * <p>签到标记必须来自服务端：仅靠浏览器 localStorage 记录，换设备、换域名或清理缓存后历史会丢失。
     * <br>接口：{@code /api/v1/raffle/activity/query_calendar_sign_rebate_list}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/query_calendar_sign_rebate_list?userId=xiaofuge&beginDate=2026-09-01&endDate=2026-09-30'}
     */
    @RequestMapping(value = "query_calendar_sign_rebate_list", method = RequestMethod.GET)
    @Override
    public Response<CalendarSignRebateResponseDTO> queryCalendarSignRebateList(@RequestParam String userId,
                                                                               @RequestParam(required = false) String beginDate,
                                                                               @RequestParam(required = false) String endDate) {
        userId = AuthenticatedUser.resolve(userId);
        try {
            log.info("查询日历签到记录开始，用户ID：{}，区间：{} ~ {}", userId, beginDate, endDate);
            // 1. 参数校验：区间默认为当月，且跨度不超过 62 天（日历一屏最多跨 3 个月）
            if (StringUtils.isBlank(userId)) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            LocalDate begin = StringUtils.isBlank(beginDate)
                    ? LocalDate.now().withDayOfMonth(1)
                    : LocalDate.parse(beginDate, DATE_FORMAT_DAY);
            LocalDate end = StringUtils.isBlank(endDate)
                    ? begin.with(TemporalAdjusters.lastDayOfMonth())
                    : LocalDate.parse(endDate, DATE_FORMAT_DAY);
            if (end.isBefore(begin) || ChronoUnit.DAYS.between(begin, end) > 62) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询区间内已完成的签到日期
            List<String> signDates = behaviorRebateService.queryBehaviorDates(userId, BehaviorTypeVO.SIGN,
                    begin.format(DATE_FORMAT_DAY), end.format(DATE_FORMAT_DAY));
            log.info("查询日历签到记录完成，用户ID：{}，签到天数：{}", userId, signDates.size());
            // 3. 一并返回服务端当天日期，前端据此校准“今天”，避免浏览器时区与服务端不一致导致错位
            return Response.<CalendarSignRebateResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(CalendarSignRebateResponseDTO.builder()
                            .serverDate(LocalDate.now().format(DATE_FORMAT_DAY))
                            .beginDate(begin.format(DATE_FORMAT_DAY))
                            .endDate(end.format(DATE_FORMAT_DAY))
                            .signDates(signDates)
                            .build())
                    .build();
        } catch (AppException e) {
            log.warn("查询日历签到记录参数错误，用户ID：{}，原因：{}", userId, e.getInfo());
            return Response.<CalendarSignRebateResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询日历签到记录失败，用户ID：{}", userId, e);
            return Response.<CalendarSignRebateResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
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
        userId = AuthenticatedUser.resolve(userId);
        if (request != null) request.setUserId(userId);
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

    /**
     * 查询sku商品集合
     *
     * <p>接口：{@code /api/v1/raffle/activity/query_sku_product_list}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/query_sku_product_list?activityId=100301'}
     */
    @RequestMapping(value = "query_sku_product_list", method = RequestMethod.GET)
    @Override
    public Response<List<SkuProductResponseDTO>> querySkuProductListByActivityId(@RequestParam Long activityId) {
        try {
            log.info("查询sku商品集合开始 activityId:{}", activityId);
            // 1. 参数校验
            if (null == activityId) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询商品&封装数据
            List<SkuProductEntity> skuProductEntities = raffleActivitySkuProductService.querySkuProductEntityListByActivityId(activityId);
            List<SkuProductResponseDTO> skuProductResponseDTOS = new ArrayList<>(skuProductEntities.size());
            for (SkuProductEntity skuProductEntity : skuProductEntities) {

                SkuProductResponseDTO.ActivityCount activityCount = new SkuProductResponseDTO.ActivityCount();
                activityCount.setTotalCount(skuProductEntity.getActivityCount().getTotalCount());
                activityCount.setMonthCount(skuProductEntity.getActivityCount().getMonthCount());
                activityCount.setDayCount(skuProductEntity.getActivityCount().getDayCount());

                SkuProductResponseDTO skuProductResponseDTO = new SkuProductResponseDTO();
                skuProductResponseDTO.setSku(skuProductEntity.getSku());
                skuProductResponseDTO.setActivityId(skuProductEntity.getActivityId());
                skuProductResponseDTO.setActivityCountId(skuProductEntity.getActivityCountId());
                skuProductResponseDTO.setStockCount(skuProductEntity.getStockCount());
                skuProductResponseDTO.setStockCountSurplus(skuProductEntity.getStockCountSurplus());
                skuProductResponseDTO.setProductAmount(skuProductEntity.getProductAmount());
                skuProductResponseDTO.setActivityCount(activityCount);
                skuProductResponseDTOS.add(skuProductResponseDTO);
            }

            log.info("查询sku商品集合完成 activityId:{} skuProductResponseDTOS:{}", activityId, JSON.toJSONString(skuProductResponseDTOS));
            return Response.<List<SkuProductResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(skuProductResponseDTOS)
                    .build();
        } catch (Exception e) {
            log.error("查询sku商品集合失败 activityId:{}", activityId, e);
            return Response.<List<SkuProductResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 查询用户积分值
     *
     * <p>接口：{@code /api/v1/raffle/activity/query_user_credit}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/query_user_credit?userId=xiaofuge'}
     */
    @RequestMapping(value = "query_user_credit", method = RequestMethod.GET)
    @Override
    public Response<BigDecimal> queryUserCreditAccount(@RequestParam String userId) {
        userId = AuthenticatedUser.resolve(userId);
        try {
            log.info("查询用户积分值开始 userId:{}", userId);
            // 1. 参数校验
            if (StringUtils.isBlank(userId)) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询用户积分账户
            CreditAccountEntity creditAccountEntity = creditAdjustService.queryUserCreditAccount(userId);
            log.info("查询用户积分值完成 userId:{} adjustAmount:{}", userId, creditAccountEntity.getAdjustAmount());
            return Response.<BigDecimal>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(creditAccountEntity.getAdjustAmount())
                    .build();
        } catch (AppException e) {
            log.warn("查询用户积分值参数错误 userId:{}", userId);
            return Response.<BigDecimal>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询用户积分值失败 userId:{}", userId, e);
            return Response.<BigDecimal>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }



    /**
     * 积分兑换商品
     */
    @RequestMapping(value = "credit_pay_exchange_sku", method = RequestMethod.POST)
    @Override
    public Response<Boolean> creditPayExchangeSku(@RequestBody SkuProductShopCartRequestDTO request) {
        if (request != null) request.setUserId(AuthenticatedUser.resolve(request.getUserId()));
        try {
            log.info("积分兑换商品开始 userId:{} sku:{}", request == null ? null : request.getUserId(), request == null ? null : request.getSku());
            // 1. 参数校验
            if (null == request || StringUtils.isBlank(request.getUserId()) || null == request.getSku()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 创建兑换商品sku订单，outBusinessNo 每次创建出一个单号。
            UnpaidActivityOrderEntity unpaidActivityOrder = raffleActivityAccountQuotaService.createSkuRechargeOrder(SkuRechargeEntity.builder()
                    .userId(request.getUserId())
                    .sku(request.getSku())
                    .outBusinessNo(RandomStringUtils.randomNumeric(12))
                    .orderTradeType(OrderTradeTypeVO.credit_pay_trade)
                    .build());
            log.info("积分兑换商品，创建订单完成 userId:{} sku:{} outBusinessNo:{}", request.getUserId(), request.getSku(), unpaidActivityOrder.getOutBusinessNo());

            // 3.支付兑换商品
            String orderId = creditAdjustService.createOrder(TradeEntity.builder()
                    .userId(unpaidActivityOrder.getUserId())
                    .tradeName(TradeNameVO.CONVERT_SKU)
                    .tradeType(TradeTypeVO.REVERSE)
                    .amount(unpaidActivityOrder.getPayAmount().negate())
                    .outBusinessNo(unpaidActivityOrder.getOutBusinessNo())
                    .build());
            log.info("积分兑换商品，支付订单完成  userId:{} sku:{} orderId:{}", request.getUserId(), request.getSku(), orderId);

            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch (AppException e) {
            log.warn("积分兑换商品参数错误 userId:{} sku:{}", request == null ? null : request.getUserId(), request == null ? null : request.getSku());
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .data(false)
                    .build();
        } catch (Exception e) {
            log.error("积分兑换商品失败 userId:{} sku:{}", request == null ? null : request.getUserId(), request == null ? null : request.getSku(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }

    /**
     * 查询用户中奖记录
     *
     * <p>接口：{@code /api/v1/raffle/activity/query_user_award_record}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/query_user_award_record?userId=qijiv&activityId=100301'}
     */
    @RequestMapping(value = "query_user_award_record", method = RequestMethod.GET)
    @Override
    public Response<List<UserAwardRecordResponseDTO>> queryUserAwardRecordList(@RequestParam String userId, @RequestParam Long activityId) {
        userId = AuthenticatedUser.resolve(userId);
        try {
            log.info("查询用户中奖记录开始 userId:{} activityId:{}", userId, activityId);
            // 1. 参数校验
            if (StringUtils.isBlank(userId) || null == activityId) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询中奖记录&封装数据
            List<UserAwardRecordEntity> userAwardRecordEntities = awardService.queryUserAwardRecordList(userId, activityId);
            List<UserAwardRecordResponseDTO> userAwardRecordResponseDTOS = new ArrayList<>(userAwardRecordEntities.size());
            for (UserAwardRecordEntity userAwardRecordEntity : userAwardRecordEntities) {
                userAwardRecordResponseDTOS.add(UserAwardRecordResponseDTO.builder()
                        .awardId(userAwardRecordEntity.getAwardId())
                        .awardTitle(userAwardRecordEntity.getAwardTitle())
                        .awardTime(userAwardRecordEntity.getAwardTime())
                        .awardState(userAwardRecordEntity.getAwardState().getCode())
                        .build());
            }
            log.info("查询用户中奖记录完成 userId:{} activityId:{} 记录数:{}", userId, activityId, userAwardRecordResponseDTOS.size());
            return Response.<List<UserAwardRecordResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(userAwardRecordResponseDTOS)
                    .build();
        } catch (AppException e) {
            log.warn("查询用户中奖记录参数错误 userId:{} activityId:{}", userId, activityId);
            return Response.<List<UserAwardRecordResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询用户中奖记录失败 userId:{} activityId:{}", userId, activityId, e);
            return Response.<List<UserAwardRecordResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 查询用户积分流水（积分明细）
     *
     * <p>接口：{@code /api/v1/raffle/activity/query_user_credit_order_list}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/query_user_credit_order_list?userId=xiaofuge&limit=50'}
     */
    @RequestMapping(value = "query_user_credit_order_list", method = RequestMethod.GET)
    @Override
    public Response<List<UserCreditOrderResponseDTO>> queryUserCreditOrderList(@RequestParam String userId,
                                                                               @RequestParam(required = false) Integer limit) {
        userId = AuthenticatedUser.resolve(userId);
        try {
            log.info("查询用户积分明细开始 userId:{} limit:{}", userId, limit);
            // 1. 参数校验
            if (StringUtils.isBlank(userId)) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询积分流水
            List<CreditOrderRecordEntity> creditOrderRecords = creditAdjustService.queryUserCreditOrderList(userId, limit);
            List<UserCreditOrderResponseDTO> responseDTOS = new ArrayList<>(creditOrderRecords.size());
            for (CreditOrderRecordEntity record : creditOrderRecords) {
                responseDTOS.add(UserCreditOrderResponseDTO.builder()
                        .orderId(record.getOrderId())
                        .tradeName(record.getTradeName())
                        .tradeType(record.getTradeType())
                        .tradeTypeDesc(tradeTypeDesc(record.getTradeType()))
                        .tradeAmount(record.getTradeAmount())
                        .outBusinessNo(record.getOutBusinessNo())
                        .createTime(record.getCreateTime())
                        .build());
            }
            log.info("查询用户积分明细完成 userId:{} 记录数:{}", userId, responseDTOS.size());
            return Response.<List<UserCreditOrderResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();
        } catch (AppException e) {
            log.warn("查询用户积分明细参数错误 userId:{}", userId);
            return Response.<List<UserCreditOrderResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询用户积分明细失败 userId:{}", userId, e);
            return Response.<List<UserCreditOrderResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 查询用户活动订单（兑换/充值记录）
     *
     * <p>接口：{@code /api/v1/raffle/activity/query_user_activity_order_list}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/query_user_activity_order_list?userId=xiaofuge'}
     */
    @RequestMapping(value = "query_user_activity_order_list", method = RequestMethod.GET)
    @Override
    public Response<List<UserActivityOrderResponseDTO>> queryUserActivityOrderList(@RequestParam String userId) {
        userId = AuthenticatedUser.resolve(userId);
        try {
            log.info("查询用户兑换记录开始 userId:{}", userId);
            // 1. 参数校验
            if (StringUtils.isBlank(userId)) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询活动订单
            List<ActivityOrderEntity> activityOrders = raffleActivityQueryService.queryActivityOrderList(userId);
            List<UserActivityOrderResponseDTO> responseDTOS = new ArrayList<>(activityOrders.size());
            for (ActivityOrderEntity order : activityOrders) {
                responseDTOS.add(UserActivityOrderResponseDTO.builder()
                        .orderId(order.getOrderId())
                        .sku(order.getSku())
                        .activityId(order.getActivityId())
                        .activityName(order.getActivityName())
                        .totalCount(order.getTotalCount())
                        .dayCount(order.getDayCount())
                        .monthCount(order.getMonthCount())
                        .payAmount(order.getPayAmount())
                        .state(null == order.getState() ? null : order.getState().getCode())
                        .stateDesc(orderStateDesc(order.getState()))
                        .orderTime(order.getOrderTime())
                        .build());
            }
            log.info("查询用户兑换记录完成 userId:{} 记录数:{}", userId, responseDTOS.size());
            return Response.<List<UserActivityOrderResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();
        } catch (AppException e) {
            log.warn("查询用户兑换记录参数错误 userId:{}", userId);
            return Response.<List<UserActivityOrderResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询用户兑换记录失败 userId:{}", userId, e);
            return Response.<List<UserActivityOrderResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 查询活动列表（多活动切换）
     *
     * <p>接口：{@code /api/v1/raffle/activity/query_activity_list}
     * <br>示例：{@code curl --request GET --url 'http://localhost:8091/api/v1/raffle/activity/query_activity_list'}
     */
    @RequestMapping(value = "query_activity_list", method = RequestMethod.GET)
    @Override
    public Response<List<ActivityInfoResponseDTO>> queryActivityList() {
        try {
            log.info("查询活动列表开始");
            List<ActivityEntity> activityEntities = raffleActivityQueryService.queryActivityList();
            List<ActivityInfoResponseDTO> responseDTOS = new ArrayList<>(activityEntities.size());
            for (ActivityEntity activity : activityEntities) {
                responseDTOS.add(ActivityInfoResponseDTO.builder()
                        .activityId(activity.getActivityId())
                        .activityName(activity.getActivityName())
                        .activityDesc(activity.getActivityDesc())
                        .state(null == activity.getState() ? null : activity.getState().getCode())
                        .stateDesc(activityStateDesc(activity.getState()))
                        .beginDateTime(activity.getBeginDateTime())
                        .endDateTime(activity.getEndDateTime())
                        .build());
            }
            log.info("查询活动列表完成 活动数:{}", responseDTOS.size());
            return Response.<List<ActivityInfoResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();
        } catch (Exception e) {
            log.error("查询活动列表失败", e);
            return Response.<List<ActivityInfoResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /** 积分交易类型编码 → 中文说明 */
    private static String tradeTypeDesc(String tradeType) {
        for (TradeTypeVO tradeTypeVO : TradeTypeVO.values()) {
            if (tradeTypeVO.getCode().equals(tradeType)) {
                return tradeTypeVO.getInfo();
            }
        }
        return tradeType;
    }

    /** 活动订单状态 → 中文说明 */
    private static String orderStateDesc(OrderStateVO state) {
        return null == state ? null : state.getDesc();
    }

    /** 活动状态 → 中文说明 */
    private static String activityStateDesc(ActivityStateVO state) {
        return null == state ? null : state.getDesc();
    }

}
