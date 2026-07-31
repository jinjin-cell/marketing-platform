package cn.qijiv.domain.activity.service.rule;

/**
 * 活动操作链接抽象类
 * 
 * @author qijiv
 * @since 2026/7/18
 */
public abstract class AbstractActionChain implements IActionChain {

    private IActionChain next;

    @Override
    public IActionChain next() {
        return next;
    }

    @Override
    public IActionChain appendNext(IActionChain next) {
        this.next = next;
        return next;
    }

}

