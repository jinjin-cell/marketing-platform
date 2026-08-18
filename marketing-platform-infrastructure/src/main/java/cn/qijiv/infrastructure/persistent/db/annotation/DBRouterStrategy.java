package cn.qijiv.infrastructure.persistent.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库路由策略注解，标记 DAO 接口，声明该 DAO 是否需要分表路由。
 *
 * @author qijiv
 * @since 2026/7/30
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DBRouterStrategy {

    /**
     * 是否分表
     *
     * @return true 分表，false 不分表
     */
    boolean splitTable() default false;
}
