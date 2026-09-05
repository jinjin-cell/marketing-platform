package cn.qijiv.api.dto;

import lombok.Data;

/**
 * 商品购物车请求对象
 * @author qijiv
 * @since 2026-09-05
 */
@Data
public class SkuProductShopCartRequestDTO {

    /**
     * 用户ID
     */
    private String userId;
    /**
     * sku 商品
     */
    private Long sku;

}

