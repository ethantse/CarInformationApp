package com.forum.model.api;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.forum.model.entity.ErrorResult;
import com.forum.model.entity.Result;
import com.forum.ui.util.ActivityUtils;

import java.lang.ref.WeakReference;

import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForegroundCallback<T extends Result> implements Callback<T>, CallbackLifecycle<T> {

    private final WeakReference<Activity> activityWeakReference;

    public ForegroundCallback(@NonNull Activity activity) {
        activityWeakReference = new WeakReference<>(activity);
    }

    @Nullable
    protected final Activity getActivity() {
        return activityWeakReference.get();
    }

    @Override
    public final void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        Activity activity = getActivity();
        if (ActivityUtils.isAlive(activity)) {
            boolean interrupt;
            if (response.isSuccessful()) {
                interrupt = onResultOk(response.code(), response.headers(), response.body());
            } else {
                interrupt = onResultError(response.code(), response.headers(), ErrorResult.from(response));
            }
            if (!interrupt) {
                onFinish();
            }
        }
    }

    @Override
    public final void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        Activity activity = getActivity();
        if (ActivityUtils.isAlive(activity)) {
            boolean interrupt;
            if (call.isCanceled()) {
                interrupt = onCallCancel();
            } else {
                interrupt = onCallException(t, ErrorResult.from(t));
            }
            if (!interrupt) {
                onFinish();
            }
        }
    }

    @Override
    public boolean onResultOk(int code, Headers headers, T result) {
        return false;
    }

    @Override
    public boolean onResultError(int code, Headers headers, ErrorResult errorResult) {
        return false;
    }

    @Override
    public boolean onCallCancel() {
        return false;
    }

    @Override
    public boolean onCallException(Throwable t, ErrorResult errorResult) {
        return false;
    }

    @Override
    public void onFinish() {}

}
