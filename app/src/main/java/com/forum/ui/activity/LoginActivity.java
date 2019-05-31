package com.forum.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import com.example.car.R;
import com.forum.model.entity.LoginResult;
import com.forum.model.storage.LoginShared;
import com.forum.presenter.contract.ILoginPresenter;
import com.forum.presenter.implement.LoginPresenter;
import com.forum.ui.dialog.AlertDialogUtils;
import com.forum.ui.dialog.ProgressDialog;
import com.forum.ui.listener.DialogCancelCallListener;
import com.forum.ui.listener.NavigationFinishClickListener;
import com.forum.ui.util.ThemeUtils;
import com.forum.ui.util.ToastUtils;
import com.forum.ui.view.ILoginView;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.cnodejs.android.oauthlogin.CNodeOAuthLoginActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;

public class LoginActivity extends FullLayoutActivity implements ILoginView {

    public static final int REQUEST_DEFAULT = 0;

    private static final int REQUEST_PERMISSIONS_QR_CODE = 0;
    private static final int REQUEST_QR_CODE_LOGIN = 1;
    private static final int REQUEST_GITHUB_LOGIN = 2;

    public static void startForResult(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startForResult(@NonNull Activity activity) {
        startForResult(activity, REQUEST_DEFAULT);
    }

    public static boolean checkLogin(@NonNull final Activity activity, final int requestCode) {
        if (TextUtils.isEmpty(LoginShared.getAccessToken(activity))) {
            AlertDialogUtils.createBuilderWithAutoTheme(activity)
                    .setMessage(R.string.need_login_tip)
                    .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startForResult(activity, requestCode);
                        }

                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return false;
        } else {
            return true;
        }
    }

    public static boolean checkLogin(@NonNull Activity activity) {
        return checkLogin(activity, REQUEST_DEFAULT);
    }

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.edt_access_token)
    MaterialEditText edtAccessToken;

    private ProgressDialog progressDialog;

    private ILoginPresenter loginPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.configThemeBeforeOnCreate(this, R.style.AppThemeLight, R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_login);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(new NavigationFinishClickListener(this));

        progressDialog = ProgressDialog.createWithAutoTheme(this);

        loginPresenter = new LoginPresenter(this, this);
    }

    @OnClick(R.id.btn_login)
    void onBtnLoginClick() {
        loginPresenter.loginAsyncTask(edtAccessToken.getText().toString().trim());
    }

    @OnClick(R.id.btn_qr_code_login)
    void onBtnQrCodeLoginClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ScanQRCodeActivity.requestPermissions(this, REQUEST_PERMISSIONS_QR_CODE);
        } else {
            ScanQRCodeActivity.startForResult(this, REQUEST_QR_CODE_LOGIN);
        }
    }

    @OnClick(R.id.btn_github_login)
    void onBtnGithubLoginClick() {
        startActivityForResult(new Intent(this, CNodeOAuthLoginActivity.class), REQUEST_GITHUB_LOGIN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_QR_CODE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ScanQRCodeActivity.onPermissionsDenied(this);
            } else {
                ScanQRCodeActivity.startForResult(this, REQUEST_QR_CODE_LOGIN);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_QR_CODE_LOGIN) {
                edtAccessToken.setText(data.getStringExtra(ScanQRCodeActivity.EXTRA_QR_CODE));
                edtAccessToken.setSelection(edtAccessToken.length());
                onBtnLoginClick();
            } else if (requestCode == REQUEST_GITHUB_LOGIN) {
                edtAccessToken.setText(data.getStringExtra(CNodeOAuthLoginActivity.EXTRA_ACCESS_TOKEN));
                edtAccessToken.setSelection(edtAccessToken.length());
                onBtnLoginClick();
            }
        }
    }

    @OnClick(R.id.btn_login_tip)
    void onBtnLoginTipClick() {
        AlertDialogUtils.createBuilderWithAutoTheme(this)
                .setMessage(R.string.how_to_get_access_token_tip_content)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    @Override
    public void onAccessTokenError(@NonNull String message) {
        edtAccessToken.setError(message);
        edtAccessToken.requestFocus();
    }

    @Override
    public void onLoginOk(@NonNull String accessToken, @NonNull LoginResult loginResult) {
        LoginShared.login(this, accessToken, loginResult);
        ToastUtils.with(this).show(R.string.login_success);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onLoginStart(@NonNull Call<LoginResult> call) {
        progressDialog.setOnCancelListener(new DialogCancelCallListener(call));
        progressDialog.show();
    }

    @Override
    public void onLoginFinish() {
        progressDialog.setOnCancelListener(null);
        progressDialog.dismiss();
    }

}
