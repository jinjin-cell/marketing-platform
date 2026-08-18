package cn.qijiv.domain.strategy.service.rule.chain;

import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;

import java.util.Objects;

/**
 * 责任链节点基类，统一维护 next 节点以及向后传递的逻辑。
 *
 * <p>具体节点只需要关注自己的规则判断，不满足条件时调用 {@link #nextLogic(String, Long)}。</p>
 */
public abstract class AbstractLogicChain implements ILogicChain {

    /** 当前节点的下一个责任链节点，链尾节点为 {@code null}。 */
    private ILogicChain next;

    /**
     * 将下一个节点追加到当前节点之后。
     *
     * @param next 要追加的责任链节点
     * @return 刚追加的节点
     */
    @Override
    public ILogicChain appendNext(ILogicChain next) {
        // 工厂装配时逐个设置 next；禁止空节点，避免抽奖过程中出现隐蔽的空指针异常。
        this.next = Objects.requireNonNull(next, "责任链下一节点不能为空");
        return next;
    }

    /**
     * 获取当前节点的下一个节点。
     *
     * @return 下一个责任链节点；链尾节点为 {@code null}
     */
    @Override
    public ILogicChain next() {
        return next;
    }

    /**
     * 当前规则未接管时，继续交给下一个节点处理。
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @return 下一个节点返回的责任链抽奖结果
     */
    protected DefaultChainFactory.StrategyAwardVO nextLogic(String userId, Long strategyId) {
        if (next == null) {
            throw new IllegalStateException("责任链缺少默认兜底节点，ruleModel: " + ruleModel());
        }
        // 当前规则未接管抽奖，继续交给后续节点处理。
        return next.logic(userId, strategyId);
    }

    /**
     * 返回当前节点的规则模型名称，同时也是 Spring Bean 名称。
     *
     * @return 规则模型名称
     */
    protected abstract String ruleModel();
}
