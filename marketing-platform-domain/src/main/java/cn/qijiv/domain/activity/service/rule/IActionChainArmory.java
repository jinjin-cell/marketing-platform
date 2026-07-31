package cn.qijiv.domain.activity.service.rule;

/**
 * 活动操作链接工厂接口
 * 
 * @author qijiv
 * @since 2026/7/18
 */
public interface IActionChainArmory {

    IActionChain next();

    IActionChain appendNext(IActionChain next);

}

