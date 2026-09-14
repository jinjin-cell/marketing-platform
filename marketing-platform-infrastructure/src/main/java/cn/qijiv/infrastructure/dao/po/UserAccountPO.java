package cn.qijiv.infrastructure.dao.po;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class UserAccountPO {
    private Long id;
    private String accountName;
    private String userId;
    private String passwordHash;
    private String passwordSalt;
    private String accountStatus;
    private Date createTime;
    private Date updateTime;
}
