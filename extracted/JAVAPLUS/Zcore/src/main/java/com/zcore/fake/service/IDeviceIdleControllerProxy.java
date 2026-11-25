package com.zcore.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRIDeviceIdleControllerStub;
import black.android.os.BRServiceManager;
import com.zcore.fake.hook.BinderInvocationStub;
import com.zcore.fake.hook.MethodHook;
import com.zcore.fake.hook.ProxyMethod;
import com.zcore.utils.MethodParameterUtils;

/**
 * @author gm
 * @function
 * @date :2024/4/23 16:23
 **/
public class IDeviceIdleControllerProxy extends BinderInvocationStub {
    public IDeviceIdleControllerProxy() {
        super(BRServiceManager.get().getService("deviceidle"));
    }

    @Override
    protected Object getWho() {
        return BRIDeviceIdleControllerStub.get().asInterface(BRServiceManager.get().getService("deviceidle"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("deviceidle");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("addPowerSaveWhitelistApp")
    public static class addPowerSaveWhitelistApp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("removePowerSaveWhitelistApp")
    public static class removePowerSaveWhitelistApp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("removeSystemPowerWhitelistApp")
    public static class removeSystemPowerWhitelistApp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("restoreSystemPowerWhitelistApp")
    public static class restoreSystemPowerWhitelistApp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isPowerSaveWhitelistExceptIdleApp")
    public static class isPowerSaveWhitelistExceptIdleApp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isPowerSaveWhitelistApp")
    public static class isPowerSaveWhitelistApp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }
}
