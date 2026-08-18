package cn.qijiv.domain.activity.service.quota.rule;

/**
 * 活动操作链接抽象类
 * 
 * @author qijiv
 * @since 2026/7/18
 */
public abstract class AbstractActionChain implements IActionChain {

    /** 责任链下一个节点 */
    private IActionChain next;

    /**
     * 获取责任链下一个节点
     *
     * @return 下一个操作链接点
     */
    @Override
    public IActionChain next() {
        return next;
    }

    /**
     * 追加责任链下一个节点
     *
     * @param next 下一个操作链接点
     * @return 追加后的下一个节点
     */
    @Override
    public IActionChain appendNext(IActionChain next) {
        this.next = next;
        return next;
    }

}

