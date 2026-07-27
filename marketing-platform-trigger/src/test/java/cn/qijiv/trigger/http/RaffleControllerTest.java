package cn.qijiv.trigger.http;

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
import org.junit.jupiter.api.BeforeEach;
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

class RaffleControllerTest {

    private IRaffleAward raffleAward;
    private IRaffleStrategy raffleStrategy;
    private IStrategyArmory strategyArmory;
    private RaffleController controller;

    @BeforeEach
    void setUp() {
        raffleAward = mock(IRaffleAward.class);
        raffleStrategy = mock(IRaffleStrategy.class);
        strategyArmory = mock(IStrategyArmory.class);
        controller = new RaffleController(raffleAward, raffleStrategy, strategyArmory);
    }

    @Test
    void strategyArmoryReturnsAssemblyResult() {
        when(strategyArmory.assembleLotteryStrategy(100001L)).thenReturn(true);

        Response<Boolean> response = controller.strategyArmory(100001L);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals(Boolean.TRUE, response.getData());
        verify(strategyArmory).assembleLotteryStrategy(100001L);
    }

    @Test
    void queryRaffleAwardListMapsDomainFields() {
        StrategyAwardEntity award = StrategyAwardEntity.builder()
                .awardId(101)
                .awardTitle("随机积分")
                .awardSubTitle("1至100积分")
                .sort(3)
                .build();
        when(raffleAward.queryRaffleStrategyAwardList(100001L))
                .thenReturn(Collections.singletonList(award));
        RaffleAwardListRequestDTO request = new RaffleAwardListRequestDTO();
        request.setStrategyId(100001L);

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
    }

    @Test
    void randomRaffleMapsAwardAndSort() {
        when(raffleStrategy.performRaffle(any(RaffleFactorEntity.class)))
                .thenReturn(RaffleAwardEntity.builder().awardId(102).sort(5).build());
        RaffleRequestDTO request = new RaffleRequestDTO();
        request.setStrategyId(100001L);

        Response<RaffleResponseDTO> response = controller.randomRaffle(request);

        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals(Integer.valueOf(102), response.getData().getAwardId());
        assertEquals(Integer.valueOf(5), response.getData().getAwardIndex());
        ArgumentCaptor<RaffleFactorEntity> captor = ArgumentCaptor.forClass(RaffleFactorEntity.class);
        verify(raffleStrategy).performRaffle(captor.capture());
        assertEquals("system", captor.getValue().getUserId());
        assertEquals(Long.valueOf(100001L), captor.getValue().getStrategyId());
    }

    @Test
    void invalidRequestReturnsIllegalParameterWithoutCallingDomain() {
        Response<RaffleResponseDTO> response = controller.randomRaffle(null);

        assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), response.getCode());
        verifyNoInteractions(raffleStrategy);
    }

    @Test
    void randomRafflePreservesBusinessError() {
        when(raffleStrategy.performRaffle(any(RaffleFactorEntity.class)))
                .thenThrow(new AppException("1001", "策略未装配"));
        RaffleRequestDTO request = new RaffleRequestDTO();
        request.setStrategyId(100001L);

        Response<RaffleResponseDTO> response = controller.randomRaffle(request);

        assertEquals("1001", response.getCode());
        assertEquals("策略未装配", response.getInfo());
    }
}
