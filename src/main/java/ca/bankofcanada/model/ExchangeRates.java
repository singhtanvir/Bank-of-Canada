package ca.bankofcanada.model;

import java.math.BigDecimal;

/* @table_name: boc_exchange_rates */
public class ExchangeRates {
    private String symbol;
    private BigDecimal rate;

    public ExchangeRates() {
    }

    public ExchangeRates(String currency, BigDecimal rate) {
        this.symbol = currency;
        this.rate = rate;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String currency) {
        this.symbol = currency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "ExchangeRates{" +
                "currency='" + symbol + '\'' +
                ", rate=" + rate +
                '}';
    }
}

