package cn.qijiv.domain.strategy.service.rule.chain;

/** 责任链节点装配接口。 */
public interface ILogicChainArmory {

    /**
     * 获取当前节点的下一个节点。
     *
     * @return 下一个责任链节点；链尾节点通常为 {@code null}
     */
    ILogicChain next();

    /**
     * 将下一个节点追加到当前节点之后。
     *
     * <p>返回刚追加的节点，工厂可以用返回值继续向后装配，形成链式拼接。</p>
     *
     * @param next 要追加的责任链节点
     * @return 刚追加的节点
     */
    ILogicChain appendNext(ILogicChain next);
}
