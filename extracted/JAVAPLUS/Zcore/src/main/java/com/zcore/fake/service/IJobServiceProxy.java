package com.zcore.fake.service;

import android.content.Context;

import black.android.app.job.BRIJobSchedulerStub;
import black.android.os.BRServiceManager;
import com.zcore.fake.hook.BinderInvocationStub;

/**
 * Binder proxy for {@link Context#JOB_SCHEDULER_SERVICE}.
 */
public class IJobServiceProxy extends BinderInvocationStub {

    public IJobServiceProxy() {
        super(BRServiceManager.get().getService(Context.JOB_SCHEDULER_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIJobSchedulerStub.get().asInterface(
                BRServiceManager.get().getService(Context.JOB_SCHEDULER_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
