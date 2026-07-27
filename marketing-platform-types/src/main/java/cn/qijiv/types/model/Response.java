package cn.qijiv.types.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response<T> implements Serializable {

    private T data;
    private String code;
    private String info;
}
