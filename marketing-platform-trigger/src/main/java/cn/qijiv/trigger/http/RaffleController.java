package cn.qijiv.trigger.http;

import cn.qijiv.api.IRaffleService;
import cn.qijiv.api.dto.RaffleAwardListRequestDTO;
import cn.qijiv.api.dto.RaffleAwardListResponseDTO;
import cn.qijiv.api.dto.RaffleRequestDTO;
import cn.qijiv.api.dto.RaffleResponseDTO;
import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.service.IRaffleAward;
import cn.qijiv.domain.strategy.service.IRaffleStrategy;
import cn.qijiv.domain.strategy.service.armory.IStrategyArmory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import cn.qijiv.types.model.Response;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 抽奖服务HTTP接口。 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin:*}")
@RequestMapping("/api/${app.config.api-version:v1}/raffle/")
public class RaffleController implements IRaffleService {

    /** 系统用户ID，随机抽奖场景下使用 */
    private static final String SYSTEM_USER_ID = "system";

    /** 奖品列表查询服务 */
    private final IRaffleAward raffleAward;
    /** 抽奖策略服务 */
    private final IRaffleStrategy raffleStrategy;
    /** 策略装配服务 */
    private final IStrategyArmory strategyArmory;

    public RaffleController(
            IRaffleAward raffleAward,
            IRaffleStrategy raffleStrategy,
            IStrategyArmory strategyArmory) {
        this.raffleAward = raffleAward;
        this.raffleStrategy = raffleStrategy;
        this.strategyArmory = strategyArmory;
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
            @RequestBody RaffleAwardListRequestDTO requestDTO) {
        Long strategyId = requestDTO == null ? null : requestDTO.getStrategyId();
        try {
            validateStrategyId(strategyId);
            log.info("查询抽奖奖品列表开始 strategyId:{}", strategyId);
            List<StrategyAwardEntity> strategyAwards =
                    raffleAward.queryRaffleStrategyAwardList(strategyId);
            if (strategyAwards == null) {
                strategyAwards = Collections.emptyList();
            }

            List<RaffleAwardListResponseDTO> result = new ArrayList<>(strategyAwards.size());
            for (StrategyAwardEntity strategyAward : strategyAwards) {
                if (strategyAward == null) {
                    continue;
                }
                result.add(RaffleAwardListResponseDTO.builder()
                        .awardId(strategyAward.getAwardId())
                        .awardTitle(strategyAward.getAwardTitle())
                        .awardSubTitle(strategyAward.getAwardSubTitle())
                        .sort(strategyAward.getSort())
                        .build());
            }

            Response<List<RaffleAwardListResponseDTO>> response = success(result);
            log.info("查询抽奖奖品列表完成 strategyId:{} response:{}",
                    strategyId, JSON.toJSONString(response));
            return response;
        } catch (AppException e) {
            log.warn("查询抽奖奖品列表参数错误 strategyId:{} info:{}", strategyId, e.getInfo());
            return failure(e);
        } catch (Exception e) {
            log.error("查询抽奖奖品列表失败 strategyId:{}", strategyId, e);
            return systemFailure();
        }
    }

    /** 执行一次随机抽奖。 */
    @Override
    @RequestMapping(value = "random_raffle", method = RequestMethod.POST)
    public Response<RaffleResponseDTO> randomRaffle(@RequestBody RaffleRequestDTO requestDTO) {
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

            Response<RaffleResponseDTO> response = success(RaffleResponseDTO.builder()
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
