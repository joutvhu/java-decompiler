package com.joutvhu.javadec.engine.util;

public class ConfusedCFRException extends RuntimeException {
    public ConfusedCFRException(String s)
    {
        super(s);
    }
    public ConfusedCFRException(Exception e)
    {
        super(e);
    }
}
