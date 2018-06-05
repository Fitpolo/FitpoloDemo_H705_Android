package com.fitpolo.demo.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import com.fitpolo.demo.AppConstants;
import com.fitpolo.demo.R;
import com.fitpolo.demo.service.MokoService;
import com.fitpolo.demo.utils.Utils;
import com.fitpolo.support.MokoSupport;

public class GuideActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isWriteStoragePermissionOpen()) {
                showRequestPermissionDialog();
                return;
            }
        }
        delayGotoMain();
    }


    private void delayGotoMain() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, AppConstants.REQUEST_CODE_ENABLE_BT);
            return;
        }
        if (!Utils.isLocServiceEnable(this)) {
            showOpenLocationDialog();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isLocationPermissionOpen()) {
                showRequestPermissionDialog2();
                return;
            } else {
                AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int checkOp = appOpsManager.checkOp(AppOpsManager.OPSTR_COARSE_LOCATION, Process.myUid(), getPackageName());
                if (checkOp != AppOpsManager.MODE_ALLOWED) {
                    showOpenSettingsDialog2();
                    return;
                }
            }
        }
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startService(new Intent(GuideActivity.this, MokoService.class));
                startActivity(new Intent(GuideActivity.this, MainActivity.class));
                GuideActivity.this.finish();
            }
        }.start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case AppConstants.PERMISSION_REQUEST_CODE: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                        boolean shouldShowRequest = shouldShowRequestPermissionRationale(permissions[0]);
                        if (shouldShowRequest) {
                            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                showRequestPermissionDialog2();
                            } else {
                                showRequestPermissionDialog();
                            }
                        } else {
                            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                showOpenSettingsDialog2();
                            } else {
                                showOpenSettingsDialog();
                            }
                        }
                    } else {
                        delayGotoMain();
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_ENABLE_BT) {
            delayGotoMain();
        }
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSION) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isWriteStoragePermissionOpen()) {
                    showOpenSettingsDialog();
                } else {
                    delayGotoMain();
                }
            }
        }
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSION_2) {
            delayGotoMain();
        }
        if (requestCode == AppConstants.REQUEST_CODE_LOCATION_SETTINGS) {
            if (!Utils.isLocServiceEnable(this)) {
                showOpenLocationDialog();
            } else {
                delayGotoMain();
            }
        }
    }

    private void showOpenSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_storage_close_title)
                .setMessage(R.string.permission_storage_close_content)
                .setPositiveButton(getString(R.string.permission_open), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        // 根据包名打开对应的设置界面
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, AppConstants.REQUEST_CODE_PERMISSION);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showRequestPermissionDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_storage_need_title)
                .setMessage(R.string.permission_storage_need_content)
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(GuideActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, AppConstants.PERMISSION_REQUEST_CODE);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showOpenLocationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.location_need_title)
                .setMessage(R.string.location_need_content)
                .setPositiveButton(getString(R.string.permission_open), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, AppConstants.REQUEST_CODE_LOCATION_SETTINGS);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showOpenSettingsDialog2() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_location_close_title)
                .setMessage(R.string.permission_location_close_content)
                .setPositiveButton(getString(R.string.permission_open), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        // 根据包名打开对应的设置界面
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, AppConstants.REQUEST_CODE_PERMISSION_2);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }

    private void showRequestPermissionDialog2() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_location_need_title)
                .setMessage(R.string.permission_location_need_content)
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(GuideActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, AppConstants.PERMISSION_REQUEST_CODE);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        return;
                    }
                }).create();
        dialog.show();
    }
}