package com.zcore.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.VersionedPackage;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import black.android.app.BRActivityThread;
import black.android.app.BRContextImpl;
import com.zcore.ZCoreCore;
import com.zcore.app.BActivityThread;
import com.zcore.core.env.AppSystemEnv;
import com.zcore.core.system.pm.BPackageManagerService;
import com.zcore.core.system.pm.BPackageSettings;
import com.zcore.fake.frameworks.BPackageManager;
import com.zcore.fake.hook.BinderInvocationStub;
import com.zcore.fake.hook.MethodHook;
import com.zcore.fake.hook.ProxyMethod;
import com.zcore.fake.service.base.PkgMethodProxy;
import com.zcore.fake.service.base.ValueMethodProxy;
import com.zcore.utils.MethodParameterUtils;
import com.zcore.utils.Reflector;
import com.zcore.utils.Slog;
import com.zcore.utils.compat.BuildCompat;
import com.zcore.utils.compat.ParceledListSliceCompat;

/**
 * Created by @Jagdish on 3/30/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class IPackageManagerProxy extends BinderInvocationStub {
    public static final String TAG = "PackageManagerStub";

    public IPackageManagerProxy() {
        super(BRActivityThread.get().sPackageManager().asBinder());
    }

    @Override
    protected Object getWho() {
        return BRActivityThread.get().sPackageManager();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRActivityThread.get()._set_sPackageManager(proxyInvocation);
        replaceSystemService("package");
        Object systemContext = BRActivityThread.get(ZCoreCore.mainThread()).getSystemContext();
        PackageManager mPackageManager = BRContextImpl.get(systemContext).mPackageManager();
        if (mPackageManager != null) {
            try {
                Reflector.on("android.app.ApplicationPackageManager").field("mPM").set(mPackageManager, proxyInvocation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
        addMethodHook(new PkgMethodProxy("shouldShowRequestPermissionRationale"));
        if (!BuildCompat.isT()) {
            addMethodHook(new PkgMethodProxy("clearPackagePreferredActivities"));
        }
    }

    
    @ProxyMethod("resolveIntent")
public static class ResolveIntent extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
        Intent intent = (Intent) args[0];
        String resolvedType = (String) args[1];
        Object rawFlags = args[2];
        int flags = rawFlags instanceof Integer ? (Integer) rawFlags : ((Number) rawFlags).intValue();

        // 1) Try resolving with system (original) resolver first.
        ResolveInfo systemRi = null;
        try {
            Object result = method.invoke(who, args); // original/system resolve
            if (result instanceof ResolveInfo) {
                systemRi = (ResolveInfo) result;
            }
        } catch (Throwable t) {
            // ignore and continue to virtual resolver
        }

        // Helper: decide if this intent *looks like* a Twitter intent
        boolean isTwitterIntent = false;
        try {
            String pkg = intent.getPackage();
            ComponentName comp = intent.getComponent();
            String data = (intent.getData() != null) ? intent.getData().getHost() : null;
            if ("com.twitter.android".equals(pkg)) isTwitterIntent = true;
            if (comp != null && "com.twitter.android".equals(comp.getPackageName())) isTwitterIntent = true;
            if (data != null && data.contains("twitter.com")) isTwitterIntent = true;
            // also check action / extras if you want more rules
        } catch (Exception ignore) {}

        // Helper: check if ResolveInfo points to a SYSTEM app
        boolean systemRiIsSystemApp = false;
        if (systemRi != null && systemRi.activityInfo != null && systemRi.activityInfo.applicationInfo != null) {
            try {
                int appFlags = systemRi.activityInfo.applicationInfo.flags;
                // ApplicationInfo.FLAG_SYSTEM indicates system app
                final int FLAG_SYSTEM = 1 << 0; // ApplicationInfo.FLAG_SYSTEM == 1
                final int FLAG_UPDATED_SYSTEM_APP = 1 << 7; // ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 128
                if ((appFlags & FLAG_SYSTEM) != 0 || (appFlags & FLAG_UPDATED_SYSTEM_APP) != 0) {
                    systemRiIsSystemApp = true;
                }
                // Also treat it as system app if its package name is exactly com.twitter.android
                if ("com.twitter.android".equals(systemRi.activityInfo.packageName)) {
                    systemRiIsSystemApp = true;
                }
            } catch (Throwable ignored) {}
        }

        // If it's a Twitter intent and system resolver returned a system Twitter app, prefer it.
        if (isTwitterIntent && systemRi != null && systemRiIsSystemApp) {
            return systemRi;
        }

        // 2) Otherwise fall back to BlackBox (virtual) package manager resolve
        ResolveInfo ri = null;
        try {
            ri = ZCoreCore.getBPackageManager()
                    .resolveIntent(intent, resolvedType, flags, BActivityThread.getUserId());
        } catch (Throwable t) {
            // ignore
        }

        // 3) Return virtual resolve if found; else return system resolve (if any); else fallback to invoking original
        if (ri != null) {
            return ri;
        } else if (systemRi != null) {
            return systemRi;
        } else {
            // final fallback: call original once more
            return method.invoke(who, args);
        }
    }
}

    @ProxyMethod("resolveService")
    public static class ResolveService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            ResolveInfo resolveInfo;
            long flags = (long) args[2];
            resolveInfo = ZCoreCore.getBPackageManager().resolveService(intent, Math.toIntExact(flags), resolvedType, BActivityThread.getUserId());
            if (resolveInfo != null) {
                return resolveInfo;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setComponentEnabledSetting")
    public static class SetComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("getPackageInfo")
    public static class GetPackageInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];
            PackageInfo packageInfo;
            long flags = (long) args[1];
            packageInfo = ZCoreCore.getBPackageManager().getPackageInfo(packageName, Math.toIntExact(flags), BActivityThread.getUserId());
            
            if (packageInfo != null) {
                return packageInfo;
            }
            if (AppSystemEnv.isOpenPackage(packageName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getPackageUid")
    public static class GetPackageUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setApplicationBlockedSettingAsUser")
    public static class setApplicationBlockedSettingAsUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUserId(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderInfo")
    public static class GetProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            ProviderInfo providerInfo;
            
            long flags = (long) args[1];
            providerInfo = ZCoreCore.getBPackageManager().getProviderInfo(componentName, Math.toIntExact(flags), BActivityThread.getUserId());
            
            if (providerInfo != null)
                return providerInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getReceiverInfo")
    public static class GetReceiverInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            ActivityInfo receiverInfo;
            
            long flags = (long) args[1];
            receiverInfo = ZCoreCore.getBPackageManager().getReceiverInfo(componentName, Math.toIntExact(flags), BActivityThread.getUserId());
                
            if (receiverInfo != null)
                return receiverInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getActivityInfo")
    public static class GetActivityInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            ActivityInfo activityInfo;
            
            long flags = (long) args[1];
            activityInfo = ZCoreCore.getBPackageManager().getActivityInfo(componentName, Math.toIntExact(flags), BActivityThread.getUserId());
                
            if (activityInfo != null)
                return activityInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                MethodParameterUtils.replaceLastUserId(args);
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getPackageUidEtc")
    public static class getPackageUidEtc extends GetPackageUid {

    }

    @ProxyMethod("getServiceInfo")
    public static class GetServiceInfo extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            ServiceInfo serviceInfo;
            long flags = (long) args[1];
            serviceInfo = ZCoreCore.getBPackageManager().getServiceInfo(componentName, Math.toIntExact(flags), BActivityThread.getUserId());
            MethodParameterUtils.replaceLastUserId(args);
            if (serviceInfo != null)
                return serviceInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getInstalledApplications")
    public static class GetInstalledApplications extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            List<ApplicationInfo> installedApplications;
            long flags = (long) args[0];
            installedApplications = ZCoreCore.getBPackageManager().getInstalledApplications(Math.toIntExact(flags), BActivityThread.getUserId());
            return ParceledListSliceCompat.create(installedApplications);
        }
    }

    @ProxyMethod("getPackageGids")
    public static class getPackageGids extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUserId(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getInstalledPackages")
    public static class GetInstalledPackages extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            List<PackageInfo> installedPackages;
            long flags = (long) args[0];
            installedPackages = ZCoreCore.getBPackageManager().getInstalledPackages(Math.toIntExact(flags), BActivityThread.getUserId());
            return ParceledListSliceCompat.create(installedPackages);
        }
    }

    @ProxyMethod("getApplicationInfo")
    public static class GetApplicationInfo extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];
            ApplicationInfo applicationInfo;
            
            long flags = (long) args[1];
            applicationInfo = ZCoreCore.getBPackageManager().getApplicationInfo(packageName, Math.toIntExact(flags), BActivityThread.getUserId());
            
            if (applicationInfo != null) {
                return applicationInfo;
            }

            if (AppSystemEnv.isOpenPackage(packageName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("queryContentProviders")
    public static class QueryContentProviders extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            List<ProviderInfo> providers;
            long flags = (long) args[2];
            providers = ZCoreCore.getBPackageManager().queryContentProviders(BActivityThread.getAppProcessName(), BActivityThread.getBUid(), Math.toIntExact(flags), BActivityThread.getUserId());
            return ParceledListSliceCompat.create(providers);
        }
    }

    @ProxyMethod("queryIntentReceivers")
    public static class QueryBroadcastReceivers extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String type = MethodParameterUtils.getFirstParam(args, String.class);
            List<ResolveInfo> resolves;
            Long flags = MethodParameterUtils.getFirstParam(args, Long.class);
            resolves = ZCoreCore.getBPackageManager().queryBroadcastReceivers(intent, Math.toIntExact(flags), type, BActivityThread.getUserId());
            Slog.d(TAG, "queryIntentReceivers: " + resolves);
            if (BuildCompat.isN()) {
                return ParceledListSliceCompat.create(resolves);
            }
            return resolves;
        }
    }

    @ProxyMethod("resolveContentProvider")
    public static class ResolveContentProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String authority = (String) args[0];
            ProviderInfo providerInfo;
            
            long flags = (long) args[1];
            providerInfo = ZCoreCore.getBPackageManager().resolveContentProvider(authority, Math.toIntExact(flags), BActivityThread.getUserId());
                
            if (providerInfo == null) {
                return method.invoke(who, args);
            }
            return providerInfo;
        }
    }

    @ProxyMethod("canRequestPackageInstalls")
    public static class CanRequestPackageInstalls extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getPackagesForUid")
    public static class GetPackagesForUid extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int uid = (Integer) args[0];
            if (uid == ZCoreCore.getHostUid()) {
                args[0] = BActivityThread.getBUid();
                uid = (int) args[0];
            }

            String[] packagesForUid = ZCoreCore.getBPackageManager().getPackagesForUid(uid);
            Slog.d(TAG, args[0] + " , " + BActivityThread.getAppProcessName() + " getPackagesForUid: " + Arrays.toString(packagesForUid));
            return packagesForUid;
        }
    }

    @ProxyMethod("getInstallerPackageName")
    public static class GetInstallerPackageName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // fake google play
            return "com.android.vending";
        }
    }

    @ProxyMethod("getPermissionInfo")
    public static class getPermissionInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String name = (String) args[0];
            int flags = (int) args[args.length - 1];
            PermissionInfo info = BPackageManager.get().getPermissionInfo(name, flags);
            if (info != null) {
                return info;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getPermissions")
    public static class getPermissions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceLastUserId(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getSharedLibraries")
    public static class GetSharedLibraries extends MethodHook {
        private static final int MATCH_ANY_USER = 4194304;
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags =  (int) getIntOrLongValue(args[1]);

            if ((flags & MATCH_ANY_USER) != 0) {
                flags &= ~MATCH_ANY_USER;
                args[1] = flags;
            }
            args[0] = ZCoreCore.getHostPkg();

            return method.invoke(who, args);
        }
    }

    public static long getIntOrLongValue(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        if (obj instanceof Long) {
            return ((Long) obj).longValue();
        }
        return -1L;
    }

    @ProxyMethod("getComponentEnabledSetting")
    public static class getComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        }
    }

    @ProxyMethod("checkPermission")
    public static class CheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String permName = (String) args[0];
            String pkgName = (String) args[1];
            Slog.e(TAG,"checkPermission pkg = "+pkgName);
            return BPackageManager.get().checkPermission(permName,pkgName,BActivityThread.getUserId());
        }
    }

    @ProxyMethod("canForwardTo")
    public static class canForwardTo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int sourceUserId = (int) args[2];
            int targetUserId = (int) args[3];
            return sourceUserId == targetUserId;
        }
    }

    @ProxyMethod("getPreferredActivities")
    public static class getPreferredActivities extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceLastAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getPackageInfoVersioned")
    public static class getPackageInfoVersioned extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            VersionedPackage versionedPackage = (VersionedPackage) args[0];
            String packageName = versionedPackage.getPackageName();
            PackageInfo packageInfo;
            
            long flags = (long) args[1];
            packageInfo = ZCoreCore.getBPackageManager().getPackageInfo(packageName, Math.toIntExact(flags), BActivityThread.getUserId());

            if (packageInfo != null) {
                return packageInfo;
            }

            if (AppSystemEnv.isOpenPackage(packageName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("queryIntentActivities")
    public static class QueryIntentActivities extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            List<ResolveInfo> intentActivities;
            long flags = (long) args[2];
            intentActivities = ZCoreCore.getBPackageManager().queryIntentActivities(intent, Math.toIntExact(flags), resolvedType, BActivityThread.getUserId());
            return ParceledListSliceCompat.create(intentActivities);
        }
    }

    @ProxyMethod("checkPackageStartable")
    public static class checkPackageStartable extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String pkg = (String) args[0];
            if (ZCoreCore.get().isInstalled(pkg,BActivityThread.getUserId())) {
                return 0;
            }
            MethodParameterUtils.replaceLastUserId(args);
            return method.invoke(who, args);
        }
    }
}
