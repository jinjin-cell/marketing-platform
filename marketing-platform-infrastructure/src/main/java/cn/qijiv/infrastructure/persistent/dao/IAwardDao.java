package cn.qijiv.infrastructure.persistent.dao;

import cn.qijiv.infrastructure.persistent.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 奖品 DAO
 *
 * @author jinlujia
 * @date 2026/07/18
 */
@Mapper
public interface IAwardDao {

    /**
     * 查询奖品列表
     *
     * @return 奖品列表
     */
    List<AwardPO> queryAwardList();

}
