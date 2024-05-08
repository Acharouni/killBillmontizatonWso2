package org.wso2.apim.monetization.impl;

public class KillBillMonetizationException extends Exception {

    public KillBillMonetizationException(String msg){
        super(msg);
    }
    public KillBillMonetizationException(String msg, Throwable e) {
        super(msg, e);
    }

    public KillBillMonetizationException(Throwable throwable) {
        super(throwable);
    }

}
