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

    @Override
    public void cleanup() {
        /* Depends on implementation, might not need to do this if the parallelism is at a ticker level.
         For eg. my MatchingEngine instance only looks at AMZN, then that object pool will only contain tickers AMZN
         In that case, i dont need to modify.
         Obviously  parallelism comes at a cost i.e. i might inject state which i might not need or a higher level risk analytics might become more difficult
        */
        ticker = null;
        buyPrice = null;
        sellPrice = null;
        buySize = -1;
        sellSize = -1;
    }
}
