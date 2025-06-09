package net.redvoiss.sms.service.task;

import java.util.concurrent.Callable;

public interface ProgressableTask extends Callable {
    public long getProgress();
}