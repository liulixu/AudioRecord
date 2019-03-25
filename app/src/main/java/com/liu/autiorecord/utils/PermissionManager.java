package com.liu.autiorecord.utils;

import android.app.Activity;

import pub.devrel.easypermissions.EasyPermissions;

public class PermissionManager {

    /**
     *
     * @param activity
     * @param perms 已获取权限
     * @return false :未获取权限，主动请求权限
     */
    public static boolean checkPermission(Activity activity,String[] perms){
        return EasyPermissions.hasPermissions(activity,perms);
    }

    /**
     * 请求权限
     * @param activity
     * @param tip
     * @param requestCode
     * @param perms
     */
    public static void requestPermission(Activity activity,String tip ,int requestCode,String[] perms){
        EasyPermissions.requestPermissions(activity,tip,requestCode,perms);
    }
}
