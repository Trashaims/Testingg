package com.zcore.fake.service.context.providers;

import android.os.Build;
import android.os.IInterface;
import black.android.content.AttributionSourceContext;
import java.lang.reflect.Method;

import black.android.content.BRAttributionSource;
import com.zcore.ZCoreCore;
import com.zcore.app.BActivityThread;
import com.zcore.fake.hook.ClassInvocationStub;
import com.zcore.utils.compat.BuildCompat;
import com.zcore.utils.compat.ContextCompat;
import top.niunaijun.blackreflection.utils.ClassUtil;

/**
 * Created by Milk on 4/8/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class ContentProviderStub extends ClassInvocationStub implements BContentProvider {
    public static final String TAG = "ContentProviderStub";
    private IInterface mBase;
    private String mAppPkg;

    public IInterface wrapper(final IInterface contentProviderProxy, final String appPkg) {
        mBase = contentProviderProxy;
        mAppPkg = appPkg;
        injectHook();
        return (IInterface) getProxyInvocation();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {

    }

    @Override
    protected void onBindMethod() {

    }

    
    @Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if ("asBinder".equals(method.getName())) {
        return method.invoke(mBase, args);
    }
    if (args != null && args.length > 0) {
        Object arg = args[0];
        if (Build.VERSION.SDK_INT == 34) {
            if (arg != null && arg.getClass().getName().equals(ClassUtil.classReady(AttributionSourceContext.class).getName())) {
                // Remove the third argument for Android 34 (UpsideDownCake)
                ContextCompat.fixAttributionSourceState(arg, ZCoreCore.getHostUid());
            }
        } else if (BuildCompat.isS() && arg != null && arg.getClass().getName().equals(ClassUtil.classReady(AttributionSourceContext.class).getName())) {
            ContextCompat.fixAttributionSourceState(arg, ZCoreCore.getHostUid());
        }
    }
    try {
        return method.invoke(mBase, args);
    } catch (Throwable e) {
        throw e.getCause();
    }
}
    
    /*
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("asBinder".equals(method.getName())) {
            return method.invoke(mBase, args);
        }
        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof String) {
                args[0] = mAppPkg;
            } else if (arg.getClass().getName().equals(BRAttributionSource.getRealClass().getName())) {
                ContextCompat.fixAttributionSourceState(arg, BActivityThread.getBUid());
            }
        }
        try {
            return method.invoke(mBase, args);
        } catch (Throwable e) {
            throw e.getCause();
        }
    }*/

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
