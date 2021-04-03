package com.insd;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SmallObject implements Poolable {
    private String ticker;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private int buySize;
    private int sellSize;
}
