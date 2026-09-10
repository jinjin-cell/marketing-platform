package cn.qijiv.infrastructure.dao;

import cn.qijiv.infrastructure.dao.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 奖品 DAO
 *
 * @author jinlujia
 * @since 2026-07-18
 */
@Mapper
public interface IAwardDao {

    /**
     *
     * 查询奖品列表
     *
     * @return 奖品列表
     */
    List<AwardPO> queryAwardList();

    /**
     *
     * 根据奖品ID查询奖品配置
     *
     * @param awardId 奖品ID
     * @return 奖品配置
     */
    String queryAwardConfigByAwardId(Integer awardId);


    /**
     *
     * 根据奖品ID查询奖品key
     *
     * @param awardId 奖品ID
     * @return 奖品key
     */
    String queryAwardKeyByAwardId(Integer awardId);


}
