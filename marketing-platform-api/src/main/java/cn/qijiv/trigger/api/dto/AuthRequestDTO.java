package cn.qijiv.trigger.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AuthRequestDTO implements Serializable {
    private String accountName;
    private String password;
}
