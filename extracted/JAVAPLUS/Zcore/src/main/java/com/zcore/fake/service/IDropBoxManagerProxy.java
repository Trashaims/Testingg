package com.zcore.fake.service;

import android.content.Context;
import android.os.DropBoxManager;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import black.com.android.internal.os.BRIDropBoxManagerService;
import black.com.android.internal.os.BRIDropBoxManagerServiceStub;
import com.zcore.ZCoreCore;
import com.zcore.fake.hook.BinderInvocationStub;
import com.zcore.fake.hook.MethodHook;
import com.zcore.fake.hook.ProxyMethod;
import com.zcore.utils.MethodParameterUtils;

/**
 * @author gm
 * @function
 * @date :2024/4/20 22:27
 **/
public class IDropBoxManagerProxy extends BinderInvocationStub {
    public IDropBoxManagerProxy() {
        super(BRServiceManager.get().getService(Context.DROPBOX_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIDropBoxManagerServiceStub.get().asInterface(BRServiceManager.get().getService(Context.DROPBOX_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.DROPBOX_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getNextEntry")
    public static class unregisterAudioFocusClient extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getNextEntryWithAttribution")
    public static class getNextEntryWithAttribution extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}
