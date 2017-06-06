package com.hendraanggrian.rx.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
public final class RxActivity {

    @NonNull final static SparseArray<EmitterWrapper<?>> REQUESTS = new SparseArray<>();
    @Nullable static WeakReference<Random> RANDOM_REQUEST_CODE;
    private static final int MAX_REQUEST_CODE = 65535; // 16-bit int

    private RxActivity() {
    }

    @NonNull
    public static Observable<Intent> startForOK(@NonNull final Activity activity, @NonNull Intent intent) {
        return createStarter(Intent.class, makeStartableForActivity(activity), intent, null);
    }

    @NonNull
    public static Observable<Intent> startForOK(@NonNull final Activity activity, @NonNull Intent intent, @Nullable Bundle options) {
        return createStarter(Intent.class, makeStartableForActivity(activity), intent, options);
    }

    @NonNull
    public static Observable<ActivityResult> startForResult(@NonNull final Activity activity, @NonNull Intent intent) {
        return createStarter(ActivityResult.class, makeStartableForActivity(activity), intent, null);
    }

    @NonNull
    public static Observable<ActivityResult> startForResult(@NonNull final Activity activity, @NonNull Intent intent, @Nullable Bundle options) {
        return createStarter(ActivityResult.class, makeStartableForActivity(activity), intent, options);
    }

    @NonNull
    public static Observable<Intent> startForOK(@NonNull final Fragment fragment, @NonNull Intent intent) {
        return createStarter(Intent.class, makeStartableForFragment(fragment), intent, null);
    }

    @NonNull
    public static Observable<Intent> startForOK(@NonNull final Fragment fragment, @NonNull Intent intent, @Nullable Bundle options) {
        return createStarter(Intent.class, makeStartableForFragment(fragment), intent, options);
    }

    @NonNull
    public static Observable<ActivityResult> startForResult(@NonNull final Fragment fragment, @NonNull Intent intent) {
        return createStarter(ActivityResult.class, makeStartableForFragment(fragment), intent, null);
    }

    @NonNull
    public static Observable<ActivityResult> startForResult(@NonNull final Fragment fragment, @NonNull Intent intent, @Nullable Bundle options) {
        return createStarter(ActivityResult.class, makeStartableForFragment(fragment), intent, options);
    }

    @NonNull
    public static Observable<Intent> startForOK(@NonNull final android.support.v4.app.Fragment fragment, @NonNull Intent intent) {
        return createStarter(Intent.class, makeStartableForSupportFragment(fragment), intent, null);
    }

    @NonNull
    public static Observable<Intent> startForOK(@NonNull final android.support.v4.app.Fragment fragment, @NonNull Intent intent, @Nullable Bundle options) {
        return createStarter(Intent.class, makeStartableForSupportFragment(fragment), intent, options);
    }

    @NonNull
    public static Observable<ActivityResult> startForResult(@NonNull final android.support.v4.app.Fragment fragment, @NonNull Intent intent) {
        return createStarter(ActivityResult.class, makeStartableForSupportFragment(fragment), intent, null);
    }

    @NonNull
    public static Observable<ActivityResult> startForResult(@NonNull final android.support.v4.app.Fragment fragment, @NonNull Intent intent, @Nullable Bundle options) {
        return createStarter(ActivityResult.class, makeStartableForSupportFragment(fragment), intent, options);
    }

    @NonNull
    private static ActivityStartable makeStartableForActivity(@NonNull final Activity activity) {
        return new ActivityStartable() {
            @NonNull
            @Override
            public PackageManager getPackageManager() {
                return activity.getPackageManager();
            }

            @Override
            public void startActivityForResult(@NonNull Intent intent, int requestCode) {
                activity.startActivityForResult(intent, requestCode);
            }

            @TargetApi(16)
            @RequiresApi(16)
            @Override
            public void startActivityForResult(@NonNull Intent intent, int requestCode, @Nullable Bundle options) {
                activity.startActivityForResult(intent, requestCode, options);
            }
        };
    }

    @NonNull
    private static ActivityStartable makeStartableForFragment(@NonNull final Fragment fragment) {
        return new ActivityStartable() {
            @NonNull
            @Override
            public PackageManager getPackageManager() {
                if (Build.VERSION.SDK_INT >= 23)
                    return fragment.getContext().getPackageManager();
                else
                    return fragment.getActivity().getPackageManager();
            }

            @Override
            public void startActivityForResult(@NonNull Intent intent, int requestCode) {
                fragment.startActivityForResult(intent, requestCode);
            }

            @TargetApi(16)
            @RequiresApi(16)
            @Override
            public void startActivityForResult(@NonNull Intent intent, int requestCode, @Nullable Bundle options) {
                fragment.startActivityForResult(intent, requestCode, options);
            }
        };
    }

    @NonNull
    private static ActivityStartable makeStartableForSupportFragment(@NonNull final android.support.v4.app.Fragment fragment) {
        return new ActivityStartable() {
            @NonNull
            @Override
            public PackageManager getPackageManager() {
                return fragment.getContext().getPackageManager();
            }

            @Override
            public void startActivityForResult(@NonNull Intent intent, int requestCode) {
                fragment.startActivityForResult(intent, requestCode);
            }

            @TargetApi(16)
            @RequiresApi(16)
            @Override
            public void startActivityForResult(@NonNull Intent intent, int requestCode, @Nullable Bundle options) {
                fragment.startActivityForResult(intent, requestCode, options);
            }
        };
    }

    @NonNull
    private static <T> Observable<T> createStarter(@NonNull final Class<T> cls, @NonNull final ActivityStartable startable, @NonNull final Intent intent, @Nullable final Bundle options) {
        return Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<T> e) throws Exception {
                EmitterWrapper<T> wrapper = new EmitterWrapper<>(cls, e);
                if (intent.resolveActivity(startable.getPackageManager()) == null) {
                    wrapper.emitter.onError(new ActivityNotFoundException());
                } else {
                    int requestCode = generateRequestCode();
                    REQUESTS.append(requestCode, wrapper);
                    if (Build.VERSION.SDK_INT >= 16) {
                        startable.startActivityForResult(intent, requestCode, options);
                    } else {
                        startable.startActivityForResult(intent, requestCode);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUESTS.indexOfKey(requestCode) > -1) {
            EmitterWrapper wrapper = REQUESTS.get(requestCode);
            if (!wrapper.emitter.isDisposed()) {
                if (wrapper.cls == ActivityResult.class) {
                    wrapper.emitter.onNext(new ActivityResult(requestCode, resultCode, data));
                } else {
                    if (resultCode == Activity.RESULT_OK) {
                        wrapper.emitter.onNext(data);
                    } else {
                        wrapper.emitter.onError(new ActivityCanceledException());
                    }
                }
                wrapper.emitter.onComplete();
            }
            REQUESTS.remove(requestCode);
        }
    }

    static int generateRequestCode() {
        if (RANDOM_REQUEST_CODE == null) {
            RANDOM_REQUEST_CODE = new WeakReference<>(new Random());
        }
        Random random = RANDOM_REQUEST_CODE.get();
        if (random == null) {
            random = new Random();
            RANDOM_REQUEST_CODE = new WeakReference<>(random);
        }
        int requestCode = random.nextInt(MAX_REQUEST_CODE);
        if (REQUESTS.indexOfKey(requestCode) < 0) {
            return requestCode;
        }
        return generateRequestCode();
    }
}