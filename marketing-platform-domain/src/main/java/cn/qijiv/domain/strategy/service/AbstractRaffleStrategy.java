package cn.qijiv.domain.strategy.service;

import cn.qijiv.domain.strategy.model.entity.RaffleAwardEntity;
import cn.qijiv.domain.strategy.model.entity.RaffleFactorEntity;
import cn.qijiv.domain.strategy.model.entity.StrategyAwardEntity;
import cn.qijiv.domain.strategy.repository.IStrategyRepository;
import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.qijiv.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * 抽奖策略模板。
 *
 * <p>模板模式把所有抽奖实现都必须遵守的执行顺序固定在
 * {@link #performRaffle(RaffleFactorEntity)} 中。子类只负责实现责任链抽奖和规则树过滤，
 * 从而避免以后新增抽奖策略时漏掉参数校验、规则过滤或者结果转换。</p>
 */
@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    /** 领域仓储，负责向领域层提供策略和规则树数据。 */
    protected final IStrategyRepository repository;
    /** 责任链工厂，负责按策略配置装配抽奖前置规则。 */
    protected final DefaultChainFactory defaultChainFactory;
    /** 规则树工厂，负责创建抽奖后置决策树引擎。 */
    protected final DefaultTreeFactory defaultTreeFactory;

    /**
     * 注入领域仓储、责任链工厂和规则树工厂。
     *
     * @param repository   领域仓储
     * @param chainFactory 责任链工厂
     * @param treeFactory  规则树工厂
     */
    protected AbstractRaffleStrategy(
            IStrategyRepository repository,
            DefaultChainFactory chainFactory,
            DefaultTreeFactory treeFactory) {
        this.repository = repository;
        this.defaultChainFactory = chainFactory;
        this.defaultTreeFactory = treeFactory;
    }

    /**
     * 执行一次完整抽奖。
     *
     * <p>这是模板方法，调用顺序固定为：参数校验 -> 责任链抽奖 -> 规则树过滤 -> 返回结果。</p>
     *
     * @param raffleFactorEntity 用户ID和策略ID组成的抽奖因子
     * @return 最终可发放的奖品
     */
    @Override
    public final RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        // 第一步：在业务入口统一校验，后面的责任链和规则树只处理合法参数。
        if (raffleFactorEntity == null
                || raffleFactorEntity.getStrategyId() == null
                || StringUtils.isBlank(raffleFactorEntity.getUserId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        String userId = raffleFactorEntity.getUserId();
        Long strategyId = raffleFactorEntity.getStrategyId();

        // 第二步：责任链先处理黑名单、权重，最后由 default 节点完成普通概率抽奖。
        DefaultChainFactory.StrategyAwardVO chainStrategyAwardVO = raffleLogicChain(userId, strategyId);
        if (chainStrategyAwardVO == null) {
            throw new IllegalStateException("抽奖责任链未返回结果，strategyId: " + strategyId);
        }
        log.info("抽奖策略计算-责任链 userId:{} strategyId:{} awardId:{} logicModel:{}",
                userId, strategyId, chainStrategyAwardVO.getAwardId(), chainStrategyAwardVO.getLogicModel());
        if (chainStrategyAwardVO.getAwardId() == null || chainStrategyAwardVO.getLogicModel() == null) {
            throw new IllegalStateException("抽奖责任链未返回有效结果，strategyId: " + strategyId);
        }
        if (!DefaultChainFactory.DEFAULT_CHAIN.equals(chainStrategyAwardVO.getLogicModel())) {
            return buildRaffleAwardEntity(strategyId, chainStrategyAwardVO.getAwardId(), null);
        }

        // 第三步：只有普通概率抽奖结果才进入规则树，依次完成次数、库存和兜底判断。
        DefaultTreeFactory.StrategyAwardVO treeStrategyAwardVO = raffleLogicTree(
                userId, strategyId, chainStrategyAwardVO.getAwardId(), raffleFactorEntity.getEndDateTime());
        if (treeStrategyAwardVO == null || treeStrategyAwardVO.getAwardId() == null) {
            throw new IllegalStateException("抽奖规则树未返回有效结果，strategyId: " + strategyId);
        }
        log.info("抽奖策略计算-规则树 userId:{} strategyId:{} awardId:{} awardRuleValue:{}",
                userId, strategyId, treeStrategyAwardVO.getAwardId(), treeStrategyAwardVO.getAwardRuleValue());

        // 第四步：把规则树内部数据转换成抽奖服务对外返回的领域实体。
        return buildRaffleAwardEntity(strategyId, treeStrategyAwardVO.getAwardId(), treeStrategyAwardVO.getAwardRuleValue());
    }

    /**
     * 根据策略奖品配置构建抽奖结果实体。
     *
     * @param strategyId  策略ID
     * @param awardId     奖品ID
     * @param awardConfig 奖品规则配置，可为空
     * @return 抽奖结果实体
     */
    protected final RaffleAwardEntity buildRaffleAwardEntity(
            Long strategyId, Integer awardId, String awardConfig) {
        StrategyAwardEntity strategyAward = repository.queryStrategyAwardEntity(strategyId, awardId);
        if (strategyAward == null) {
            throw new IllegalStateException(
                    "抽奖结果未匹配到策略奖品，strategyId: " + strategyId
                            + ", awardId: " + awardId);
        }
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .awardTitle(strategyAward.getAwardTitle())
                .awardConfig(awardConfig)
                .sort(strategyAward.getSort())
                .build();
    }

    /** 执行抽奖前置责任链，由具体抽奖策略决定如何打开并运行责任链。 */
    protected abstract DefaultChainFactory.StrategyAwardVO raffleLogicChain(
            String userId, Long strategyId);

    /** 执行抽奖后置规则树，由具体抽奖策略负责查询树配置并启动引擎。 */
    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(
            String userId, Long strategyId, Integer awardId);

    /**
     * 执行抽奖后置规则树，并把活动结束时间透传给库存扣减节点，
     * 用于给奖品库存锁设置与活动结束时间对齐的缓存有效期。
     *
     * @param userId      用户ID
     * @param strategyId  策略ID
     * @param awardId     奖品ID
     * @param endDateTime 活动结束时间，可为空
     * @return 规则树过滤后的奖品结果
     */
    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(
            String userId, Long strategyId, Integer awardId, Date endDateTime);
}
