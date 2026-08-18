package cn.qijiv.infrastructure.persistent.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库路由注解，标记需要按用户 ID 进行分库分表路由的 DAO 方法。
 *
 * @author qijiv
 * @since 2026/7/30
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DBRouter {
}
