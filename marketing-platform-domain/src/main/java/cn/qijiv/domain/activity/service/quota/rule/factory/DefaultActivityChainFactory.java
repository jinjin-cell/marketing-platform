package cn.qijiv.domain.activity.service.quota.rule.factory;

import cn.qijiv.domain.activity.service.quota.rule.IActionChain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 默认活动责任链工厂
 */
@Service
public class DefaultActivityChainFactory {

    /** 活动责任链入口 */
    private final IActionChain actionChain;

    /**
     * 1. 通过构造函数注入。
     * 2. Spring 可以自动注入 IActionChain 接口实现类到 map 对象中，key 就是 bean 的名字。
     * 3. 活动下单动作的责任链是固定的，所以直接在构造函数中组装即可。
     */
    public DefaultActivityChainFactory(Map<String, IActionChain> actionChainGroup) {
        actionChain = actionChainGroup.get(ActionModel.activity_base_action.code);
        actionChain.appendNext(actionChainGroup.get(ActionModel.activity_sku_stock_action.getCode()));
    }

    /**
     * 获取已组装好的责任链入口
     *
     * @return 责任链入口节点
     */
    public IActionChain openActionChain() {
        return this.actionChain;
    }

    /**
     * 活动责任链动作模型
     */
    @Getter
    @AllArgsConstructor
    public enum ActionModel {

        /** 活动库存、时间校验 */
        activity_base_action("activity_base_action", "活动的库存、时间校验"),
        /** 活动SKU库存扣减 */
        activity_sku_stock_action("activity_sku_stock_action", "活动sku库存"),
        ;

        /** 动作编码 */
        private final String code;
        /** 动作说明 */
        private final String info;

    }

}

