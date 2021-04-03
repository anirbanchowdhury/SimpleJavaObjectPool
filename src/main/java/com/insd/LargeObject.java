package com.insd;

import lombok.Data;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

@Data
public class LargeObject implements Poolable {
    //Basket of tickers
    private String ticker1,ticker2,ticker3,ticker4,ticker5 ;
    private BigDecimal buyPrice1,buyPrice2,buyPrice3,buyPrice4,buyPrice5;
    private BigDecimal sellPrice1,sellPrice2,sellPrice3,sellPrice4,sellPrice5;
    private int buySize1,buySize2,buySize3,buySize4,buySize5;
    private int sellSize1,sellSize2,sellSize3,sellSize4,sellSize5;

    private ByteBuffer buffer;
    public LargeObject(){
        buffer = ByteBuffer.allocate(1024);
    }
}
