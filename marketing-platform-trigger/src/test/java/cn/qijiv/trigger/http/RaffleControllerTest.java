package cn.qijiv.trigger.http;

import cn.qijiv.trigger.api.dto.RaffleAwardListRequestDTO;
import cn.qijiv.trigger.api.dto.RaffleAwardListResponseDTO;
import cn.qijiv.trigger.api.dto.RaffleStrategyRequestDTO;
import cn.qijiv.trigger.api.dto.RaffleStrategyResponseDTO;
import cn.qijiv.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.service.IRaffleAward;
import cn.qijiv.domain.strategy.service.IRaffleRule;
import cn.qijiv.domain.strategy.service.IRaffleStrategy;
import cn.qijiv.domain.strategy.service.armory.IStrategyArmory;
import cn.qijiv.trigger.security.AuthenticatedUser;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import cn.qijiv.types.model.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 抽奖控制器单元测试：验证装配、奖品列表与随机抽奖接口的响应封装。 */
class RaffleControllerTest {

    /** Mock 的奖品查询服务。 */
    private IRaffleAward raffleAward;
    /** Mock 的抽奖规则查询服务。 */
    private IRaffleRule raffleRule;
    /** Mock 的抽奖策略服务。 */
    private IRaffleStrategy raffleStrategy;
    /** Mock 的策略装配服务。 */
    private IStrategyArmory strategyArmory;
    /** Mock 的活动账户额度服务。 */
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;
    /** 被测的抽奖控制器。 */
    private RaffleStrategyController controller;

    /** 初始化各 Mock 依赖并构建被测控制器。 */
    @BeforeEach
    void setUp() {
        raffleAward = mock(IRaffleAward.class);
        raffleRule = mock(IRaffleRule.class);
        raffleStrategy = mock(IRaffleStrategy.class);
        strategyArmory = mock(IStrategyArmory.class);
        raffleActivityAccountQuotaService = mock(IRaffleActivityAccountQuotaService.class);
        controller = new RaffleStrategyController(raffleAward, raffleRule, raffleStrategy, strategyArmory, raffleActivityAccountQuotaService);
    }

    @AfterEach
    void clearAuthenticatedUser() {
        AuthenticatedUser.clear();
    }

    /** 验证策略装配接口返回成功与装配结果。 */
    @Test
    void strategyArmoryReturnsAssemblyResult() {
        when(strategyArmory.assembleLotteryStrategy(100001L)).thenReturn(true);

        Response<Boolean> response = controller.strategyArmory(100001L);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals(Boolean.TRUE, response.getData());
        verify(strategyArmory).assembleLotteryStrategy(100001L);
    }

    /** 验证奖品列表接口正确映射领域字段到响应 DTO。 */
    @Test
    void queryRaffleAwardListMapsDomainFields() {
        StrategyAwardEntity award = StrategyAwardEntity.builder()
                .awardId(101)
                .awardTitle("随机积分")
                .awardSubTitle("1至100积分")
                .sort(3)
                .ruleModels("tree_lock_1")
                .build();
        when(raffleAward.queryRaffleStrategyAwardListByActivityId(100301L))
                .thenReturn(Collections.singletonList(award));
        when(raffleRule.queryAwardRuleLockCount(any()))
                .thenReturn(Collections.emptyMap());
        when(raffleActivityAccountQuotaService.queryRaffleActivityAccountPartakeCount(100301L, "qijiv"))
                .thenReturn(0);
        RaffleAwardListRequestDTO request = new RaffleAwardListRequestDTO();
        request.setActivityId(100301L);
        request.setUserId("qijiv");

        Response<List<RaffleAwardListResponseDTO>> response =
                controller.queryRaffleAwardList(request);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        RaffleAwardListResponseDTO result = response.getData().get(0);
        assertEquals(Integer.valueOf(101), result.getAwardId());
        assertEquals("随机积分", result.getAwardTitle());
        assertEquals("1至100积分", result.getAwardSubTitle());
        assertEquals(Integer.valueOf(3), result.getSort());
        assertEquals(Boolean.TRUE, result.getIsAwardUnlock());
        assertEquals(Integer.valueOf(0), result.getWaitUnlockCount());
    }

    @Test
    void queryRaffleAwardListUsesAuthenticatedIdentityInsteadOfRequestBody() {
        when(raffleAward.queryRaffleStrategyAwardListByActivityId(100301L))
                .thenReturn(Collections.emptyList());
        when(raffleRule.queryAwardRuleLockCount(any())).thenReturn(Collections.emptyMap());
        when(raffleActivityAccountQuotaService.queryRaffleActivityAccountPartakeCount(100301L, "u_token"))
                .thenReturn(1);
        RaffleAwardListRequestDTO request = new RaffleAwardListRequestDTO();
        request.setActivityId(100301L);
        request.setUserId("forged-user");
        AuthenticatedUser.set("u_token");

        Response<List<RaffleAwardListResponseDTO>> response = controller.queryRaffleAwardList(request);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals("u_token", request.getUserId());
        verify(raffleActivityAccountQuotaService)
                .queryRaffleActivityAccountPartakeCount(100301L, "u_token");
    }

    /** 验证随机抽奖接口正确映射奖项与排序，并携带默认用户与策略 ID。 */
    @Test
    void randomRaffleMapsAwardAndSort() {
        when(raffleStrategy.performRaffle(any(RaffleFactorEntity.class)))
                .thenReturn(RaffleAwardEntity.builder().awardId(102).sort(5).build());
        RaffleStrategyRequestDTO request = new RaffleStrategyRequestDTO();
        request.setStrategyId(100001L);

        Response<RaffleStrategyResponseDTO> response = controller.randomRaffle(request);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals(Integer.valueOf(102), response.getData().getAwardId());
        assertEquals(Integer.valueOf(5), response.getData().getAwardIndex());
        ArgumentCaptor<RaffleFactorEntity> captor = ArgumentCaptor.forClass(RaffleFactorEntity.class);
        verify(raffleStrategy).performRaffle(captor.capture());
        assertEquals("system", captor.getValue().getUserId());
        assertEquals(Long.valueOf(100001L), captor.getValue().getStrategyId());
    }

    /** 验证空请求返回非法参数，且不调用领域服务。 */
    @Test
    void invalidRequestReturnsIllegalParameterWithoutCallingDomain() {
        Response<RaffleStrategyResponseDTO> response = controller.randomRaffle(null);

        assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), response.getCode());
        verifyNoInteractions(raffleStrategy);
    }

    /** 验证业务异常能原样透传到响应编码与信息。 */
    @Test
    void randomRafflePreservesBusinessError() {
        when(raffleStrategy.performRaffle(any(RaffleFactorEntity.class)))
                .thenThrow(new AppException("1001", "策略未装配"));
        RaffleStrategyRequestDTO request = new RaffleStrategyRequestDTO();
        request.setStrategyId(100001L);

        Response<RaffleStrategyResponseDTO> response = controller.randomRaffle(request);

        assertEquals("1001", response.getCode());
        assertEquals("策略未装配", response.getInfo());
    }
}
