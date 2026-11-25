package com.zcore.core.system.user;

import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.zcore.core.system.ISystemService;

/**
 * Thin user registry for the virtual environment; creates users on demand during installs.
 */
public class BUserManagerService extends IBUserManagerService.Stub implements ISystemService {

    private static final BUserManagerService sInstance = new BUserManagerService();
    private final ConcurrentHashMap<Integer, BUserInfo> mUsers = new ConcurrentHashMap<>();

    public static BUserManagerService get() {
        return sInstance;
    }

    private BUserManagerService() {
        try {
            newUserInfo(BUserHandle.USER_SYSTEM, "system");
            newUserInfo(BUserHandle.USER_XPOSED, "xposed");
        } catch (RemoteException e) {
            throw new AssertionError(e);
        }
    }

    private BUserInfo newUserInfo(int userId, String name) throws RemoteException {
        BUserInfo info = new BUserInfo();
        info.id = userId;
        info.status = BUserStatus.ENABLE;
        info.name = name;
        info.createTime = System.currentTimeMillis();
        mUsers.put(userId, info);
        return info;
    }

    @Override
    public void systemReady() {
    }

    @Override
    public BUserInfo getUserInfo(int userId) throws RemoteException {
        if (userId == BUserHandle.USER_ALL) {
            BUserInfo all = new BUserInfo();
            all.id = userId;
            all.status = BUserStatus.ENABLE;
            all.name = "all";
            all.createTime = 0L;
            return all;
        }
        return mUsers.get(userId);
    }

    /** Same as {@link #exists(int)} for in-process callers — avoids checked {@link RemoteException}. */
    public boolean existsUser(int userId) {
        try {
            return exists(userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public boolean exists(int userId) throws RemoteException {
        return existsImpl(userId);
    }

    private boolean existsImpl(int userId) {
        if (userId == BUserHandle.USER_NULL) {
            return false;
        }
        if (userId == BUserHandle.USER_ALL || userId == BUserHandle.USER_XPOSED || userId == BUserHandle.USER_SYSTEM) {
            return true;
        }
        return mUsers.containsKey(userId);
    }

    @Override
    public BUserInfo createUser(int userId) throws RemoteException {
        if (exists(userId) && userId != BUserHandle.USER_ALL) {
            return getUserInfo(userId);
        }
        return newUserInfo(userId, "user-" + userId);
    }

    @Override
    public List<BUserInfo> getUsers() throws RemoteException {
        return new ArrayList<>(mUsers.values());
    }

    /** Called from in-process Java (not Binder); same snapshot as {@link #getUsers()}. */
    public List<BUserInfo> getAllUsers() {
        try {
            return getUsers();
        } catch (RemoteException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteUser(int userId) throws RemoteException {
        mUsers.remove(userId);
    }
}
