package cn.qijiv.trigger.api.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class AuthResponseDTO implements Serializable {
    private String accountName;
    private String userId;
    private String accessToken;
    private Long expiresInSeconds;
}
