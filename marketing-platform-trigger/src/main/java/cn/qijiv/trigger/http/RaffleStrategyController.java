package cn.qijiv.trigger.http;

import cn.qijiv.api.IRaffleStrategyService;
import cn.qijiv.api.dto.RaffleAwardListRequestDTO;
import cn.qijiv.api.dto.RaffleAwardListResponseDTO;
import cn.qijiv.api.dto.RaffleStrategyRequestDTO;
import cn.qijiv.api.dto.RaffleStrategyResponseDTO;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.service.IRaffleAward;
import cn.qijiv.domain.strategy.service.IRaffleRule;
import cn.qijiv.domain.strategy.service.IRaffleStrategy;
import cn.qijiv.domain.strategy.service.armory.IStrategyArmory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import cn.qijiv.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 抽奖服务HTTP接口。 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin:*}")
@RequestMapping("/api/${app.config.api-version:v1}/raffle/strategy/")
public class RaffleStrategyController implements IRaffleStrategyService {

    /** 系统用户ID，随机抽奖场景下使用 */
    private static final String SYSTEM_USER_ID = "system";

    /** 奖品列表查询服务 */
    private final IRaffleAward raffleAward;
    /** 抽奖规则查询服务 */
    private final IRaffleRule raffleRule;
    /** 抽奖策略服务 */
    private final IRaffleStrategy raffleStrategy;
    /** 策略装配服务 */
    private final IStrategyArmory strategyArmory;
    /** 活动账户额度服务 */
    private final IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    public RaffleStrategyController(
            IRaffleAward raffleAward,
            IRaffleRule raffleRule,
            IRaffleStrategy raffleStrategy,
            IStrategyArmory strategyArmory,
            IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService) {
        this.raffleAward = raffleAward;
        this.raffleRule = raffleRule;
        this.raffleStrategy = raffleStrategy;
        this.strategyArmory = strategyArmory;
        this.raffleActivityAccountQuotaService = raffleActivityAccountQuotaService;
    }

    /** 将策略概率表和奖品库存装配到Redis。 */
    @Override
    @RequestMapping(value = "strategy_armory", method = RequestMethod.GET)
    public Response<Boolean> strategyArmory(@RequestParam Long strategyId) {
        try {
            validateStrategyId(strategyId);
            log.info("抽奖策略装配开始 strategyId:{}", strategyId);
            Response<Boolean> response = success(strategyArmory.assembleLotteryStrategy(strategyId));
            log.info("抽奖策略装配完成 strategyId:{} response:{}",
                    strategyId, JSON.toJSONString(response));
            return response;
        } catch (AppException e) {
            log.warn("抽奖策略装配参数错误 strategyId:{} info:{}", strategyId, e.getInfo());
            return failure(e);
        } catch (Exception e) {
            log.error("抽奖策略装配失败 strategyId:{}", strategyId, e);
            return systemFailure();
        }
    }

    /** 查询抽奖盘展示所需的奖品列表。 */
    @Override
    @RequestMapping(value = "query_raffle_award_list", method = RequestMethod.POST)
    public Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(
            @RequestBody RaffleAwardListRequestDTO request) {
        try {
            log.info("查询抽奖奖品列表配置开始 userId:{} activityId:{}", request.getUserId(), request.getActivityId());
            // 1. 参数校验
            if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 查询奖品配置
            List<StrategyAwardEntity> strategyAwardEntities = raffleAward.queryRaffleStrategyAwardListByActivityId(request.getActivityId());
            // 3. 获取奖品绑定的规则树ID
            List<String> treeIds = strategyAwardEntities.stream()
                    .map(StrategyAwardEntity::getRuleModels)
                    .filter(ruleModel -> ruleModel != null && !ruleModel.isEmpty())
                    .collect(Collectors.toList());
            // 4. 查询规则配置 - 获取奖品的解锁限制，抽奖N次后解锁
            Map<String, Integer> ruleLockCountMap = raffleRule.queryAwardRuleLockCount(treeIds);
            // 5. 查询抽奖次数 - 用户已经参与的抽奖次数
            Integer dayPartakeCount = raffleActivityAccountQuotaService.queryRaffleActivityAccountDayPartakeCount(request.getActivityId(), request.getUserId());
            // 6. 遍历填充数据
            List<RaffleAwardListResponseDTO> raffleAwardListResponseDTOS = new ArrayList<>(strategyAwardEntities.size());
            for (StrategyAwardEntity strategyAward : strategyAwardEntities) {
                Integer awardRuleLockCount = ruleLockCountMap.get(strategyAward.getRuleModels());
                raffleAwardListResponseDTOS.add(RaffleAwardListResponseDTO.builder()
                        .awardId(strategyAward.getAwardId())
                        .awardTitle(strategyAward.getAwardTitle())
                        .awardSubTitle(strategyAward.getAwardSubTitle())
                        .sort(strategyAward.getSort())
                        .awardRuleLockCount(awardRuleLockCount)
                        .isAwardUnlock(null == awardRuleLockCount || dayPartakeCount >= awardRuleLockCount)
                        .waitUnlockCount(null == awardRuleLockCount || awardRuleLockCount <= dayPartakeCount ? 0 : awardRuleLockCount - dayPartakeCount)
                        .build());
            }
            Response<List<RaffleAwardListResponseDTO>> response = Response.<List<RaffleAwardListResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(raffleAwardListResponseDTOS)
                    .build();
            log.info("查询抽奖奖品列表配置完成 userId:{} activityId:{} response: {}", request.getUserId(), request.getActivityId(), JSON.toJSONString(response));
            // 返回结果
            return response;
        } catch (AppException e) {
            log.warn("查询抽奖奖品列表参数错误 userId:{} activityId:{} info:{}", request.getUserId(), request.getActivityId(), e.getInfo());
            return failure(e);
        } catch (Exception e) {
            log.error("查询抽奖奖品列表配置失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<List<RaffleAwardListResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /** 执行一次随机抽奖。 */
    @Override
    @RequestMapping(value = "random_raffle", method = RequestMethod.POST)
    public Response<RaffleStrategyResponseDTO> randomRaffle(@RequestBody RaffleStrategyRequestDTO requestDTO) {
        Long strategyId = requestDTO == null ? null : requestDTO.getStrategyId();


        try {
            validateStrategyId(strategyId);
            log.info("随机抽奖开始 strategyId:{}", strategyId);
            RaffleAwardEntity raffleResult = raffleStrategy.performRaffle(
                    RaffleFactorEntity.builder()
                            .userId(SYSTEM_USER_ID)
                            .strategyId(strategyId)
                            .build());
            if (raffleResult == null || raffleResult.getAwardId() == null) {
                throw new IllegalStateException("抽奖服务未返回有效奖品");
            }

            Response<RaffleStrategyResponseDTO> response = success(RaffleStrategyResponseDTO.builder()
                    .awardId(raffleResult.getAwardId())
                    .awardIndex(raffleResult.getSort())
                    .build());
            log.info("随机抽奖完成 strategyId:{} response:{}",
                    strategyId, JSON.toJSONString(response));
            return response;
        } catch (AppException e) {
            log.warn("随机抽奖业务失败 strategyId:{} info:{}", strategyId, e.getInfo());
            return failure(e);
        } catch (Exception e) {
            log.error("随机抽奖失败 strategyId:{}", strategyId, e);
            return systemFailure();
        }
    }

    /**
     * 校验策略ID是否合法
     *
     * @param strategyId 策略ID
     */
    private void validateStrategyId(Long strategyId) {
        if (strategyId == null || strategyId <= 0) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
    }

    /**
     * 构造成功响应
     *
     * @param data 响应数据
     * @return 统一响应对象
     */
    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    /**
     * 构造业务失败响应
     *
     * @param exception 业务异常
     * @return 统一响应对象
     */
    private <T> Response<T> failure(AppException exception) {
        return Response.<T>builder()
                .code(exception.getCode())
                .info(exception.getInfo())
                .build();
    }

    /**
     * 构造系统异常响应
     *
     * @return 统一响应对象
     */
    private <T> Response<T> systemFailure() {
        return Response.<T>builder()
                .code(ResponseCode.UN_ERROR.getCode())
                .info(ResponseCode.UN_ERROR.getInfo())
                .build();
    }
}
