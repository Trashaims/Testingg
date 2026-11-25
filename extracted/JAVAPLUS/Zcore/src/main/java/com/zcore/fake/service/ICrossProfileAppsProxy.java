package com.zcore.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.content.pm.BRICrossProfileAppsStub;
import black.android.os.BRServiceManager;
import com.zcore.fake.hook.BinderInvocationStub;
import com.zcore.fake.hook.MethodHook;
import com.zcore.fake.hook.ProxyMethod;
import com.zcore.utils.MethodParameterUtils;

/**
 * @author gm
 * @function
 * @date :2024/4/20 22:12
 **/
public class ICrossProfileAppsProxy extends BinderInvocationStub {
    public ICrossProfileAppsProxy() {
        super(BRServiceManager.get().getService("crossprofileapps"));
    }

    @Override
    protected Object getWho() {
        return BRICrossProfileAppsStub.get().asInterface(BRServiceManager.get().getService("crossprofileapps"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("crossprofileapps");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getTargetUserProfiles")
    public static class getTargetUserProfiles extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("startActivityAsUser")
    public static class startActivityAsUser extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}
