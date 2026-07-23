package cn.qijiv.domain.strategy.service.rule.chain;

import cn.qijiv.domain.strategy.service.rule.chain.factory.DefaultChainFactory;

import java.util.Objects;

/**
 * 责任链节点基类，统一维护 next 节点以及向后传递的逻辑。
 *
 * <p>具体节点只需要关注自己的规则判断，不满足条件时调用 {@link #nextLogic(String, Long)}。</p>
 */
public abstract class AbstractLogicChain implements ILogicChain {

    private ILogicChain next;

    @Override
    public ILogicChain appendNext(ILogicChain next) {
        // 工厂装配时逐个设置 next；禁止空节点，避免抽奖过程中出现隐蔽的空指针异常。
        this.next = Objects.requireNonNull(next, "责任链下一节点不能为空");
        return next;
    }

    @Override
    public ILogicChain next() {
        return next;
    }

    protected DefaultChainFactory.StrategyAwardVO nextLogic(String userId, Long strategyId) {
        if (next == null) {
            throw new IllegalStateException("责任链缺少默认兜底节点，ruleModel: " + ruleModel());
        }
        // 当前规则未接管抽奖，继续交给后续节点处理。
        return next.logic(userId, strategyId);
    }

    protected abstract String ruleModel();
}
