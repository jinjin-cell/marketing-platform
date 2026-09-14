package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.dao.po.UserAccountPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserAccountDao {
    UserAccountPO queryByAccountName(String accountName);

    int insert(UserAccountPO userAccount);
}
