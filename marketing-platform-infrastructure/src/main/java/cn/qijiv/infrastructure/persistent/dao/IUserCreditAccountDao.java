package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.db.annotation.DBRouter;
import cn.qijiv.infrastructure.persistent.db.annotation.DBRouterStrategy;
import cn.qijiv.infrastructure.persistent.po.UserCreditAccountPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户积分账户数据访问层
 *
 * @author qijiv
 * @since 2026-08-26
 */
@Mapper
@DBRouterStrategy(splitTable = false)
public interface IUserCreditAccountDao {

    /**
     * 新增用户积分账户
     *
     * @param userCreditAccount 用户积分账户
     */
    void insert(UserCreditAccountPO userCreditAccount);

    /**
     * 按用户ID查询积分账户
     *
     * @param userCreditAccountReq 查询条件（携带 userId）
     * @return 积分账户；不存在返回 null
     */
    @DBRouter
    UserCreditAccountPO queryUserCreditAccountByUserId(UserCreditAccountPO userCreditAccountReq);

    /**
     * 更新用户积分账户余额
     *
     * @param userCreditAccountReq 更新条件（携带 userId 和 amount）
     * @return 影响行数
     */
    int updateAddAmount(UserCreditAccountPO userCreditAccountReq);

}
