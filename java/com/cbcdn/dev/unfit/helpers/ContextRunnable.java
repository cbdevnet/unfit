package com.cbcdn.dev.unfit.helpers;

import android.content.Context;

public abstract class ContextRunnable implements Runnable {
    protected Context passedContext;

    public ContextRunnable(Context c){
        this.passedContext = c;
    }
}
