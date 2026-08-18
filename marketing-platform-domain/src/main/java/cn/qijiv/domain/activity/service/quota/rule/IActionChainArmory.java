package cn.qijiv.domain.activity.service.quota.rule;

/**
 * 活动操作链接工厂接口
 * 
 * @author qijiv
 * @since 2026/7/18
 */
public interface IActionChainArmory {

    /**
     * 获取责任链下一个节点
     *
     * @return 下一个操作链接点
     */
    IActionChain next();

    /**
     * 追加责任链下一个节点
     *
     * @param next 下一个操作链接点
     * @return 追加后的下一个节点
     */
    IActionChain appendNext(IActionChain next);

}

