package com.zazoapp.client.ui;

/**
 * Created by skamenkovych@codeminders.com on 5/5/2015.
 */
public abstract class CancelableTask implements Runnable {

    private volatile boolean isCanceled;

    @Override
    public void run() {
        if (!isCanceled) {
            doTask();
        }
    }

    /**
     * Override this method instead {@link #run()} to use ability of cancel
     */
    protected abstract void doTask();

    /**
     * Call this to cancel task. If task has already being run it couldn't be cancelled
     */
    public void cancel() {
        isCanceled = true;
    }
}
