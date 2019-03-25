package com.liu.autiorecord;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.liu.autiorecord.utils.PermissionManager;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class BaseActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private String[] data;
    /**
     * -----------------动态权限的检查与申请-----------------------
     **/
    public void checkPermissions(Context context, int requestCode, String[] perms) {
        boolean result = PermissionManager.checkPermission(this, perms);
        if (!result) {
            /**
             * activity:
             * tip:提示的文字内容
             * requestCode:请求唯一码
             * perms:要请求的权限，1个或者多个都可以
             */
            PermissionManager.requestPermission(this, "为了正常使用该页面所有的功能，请允许使用以下权限!", requestCode, perms);
        }
    }
    /**
     * 重写onRequestPermissionsResult，用于接受请求结果
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Toast.makeText(this, "用户授权成功",Toast.LENGTH_SHORT);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        //请求权限失败
        Toast.makeText(this, "用户授权失败，app部分功能可能无法正常使用",Toast.LENGTH_SHORT);
        /**
         * 若是在权限弹窗中，用户勾选了'NEVER ASK AGAIN.'或者'不在提示'，且拒绝权限。
         * 这时候，需要跳转到设置界面去，让用户手动开启。
         */
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            //当从软件设置界面，返回当前程序时候
            case AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE:
                break;
        }
    }
}
