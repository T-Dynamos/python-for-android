package org.kivy.android;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import com.airbnb.lottie.LottieAnimationView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.ActionMode;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.content.res.Configuration;
import android.widget.RelativeLayout;
import android.content.res.Resources.NotFoundException;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
import org.libsdl.app.SDLActivity;
import com.airbnb.lottie.LottieAnimationView;
import android.widget.TextView;

import org.kivy.android.launcher.Project;

import org.renpy.android.ResourceManager;


public class PythonActivity extends SDLActivity {
    private static final String TAG = "PythonActivity";

    public static PythonActivity mActivity = null;

    private ResourceManager resourceManager = null;
    private Bundle mMetaData = null;
    private PowerManager.WakeLock mWakeLock = null;

    public String getAppRoot() {
        String app_root =  getFilesDir().getAbsolutePath() + "/app";
        return app_root;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "PythonActivity onCreate running");
        resourceManager = new ResourceManager(this);

        Log.v(TAG, "About to do super onCreate");
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Did super onCreate");

        this.mActivity = this;
        this.showLoadingScreen(this.getLoadingScreen());

        new UnpackFilesTask().execute(getAppRoot());
    }

    public void loadLibraries() {
        String app_root = new String(getAppRoot());
        File app_root_file = new File(app_root);
        PythonUtil.loadLibraries(app_root_file,
            new File(getApplicationInfo().nativeLibraryDir));
    }

    /**
     * Show an error using a toast. (Only makes sense from non-UI
     * threads.)
     */
    public void toastError(final String msg) {

        final Activity thisActivity = this;

        runOnUiThread(new Runnable () {
            public void run() {
                Toast.makeText(thisActivity, msg, Toast.LENGTH_LONG).show();
            }
        });

        // Wait to show the error.
        synchronized (this) {
            try {
                this.wait(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    private class UnpackFilesTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            File app_root_file = new File(params[0]);
            Log.v(TAG, "Ready to unpack");
            updateLoadingText("Unpacking files...");
            Consumer<Integer> progressUpdate = (inte) -> updateLoadingText(inte.toString());
            boolean should_init = PythonUtil.unpackAsset(
               progressUpdate, 
                mActivity,
                "private", 
                app_root_file, 
                true
            );
            PythonUtil.unpackPyBundle(
                mActivity, 
                getApplicationInfo().nativeLibraryDir + "/" + "libpybundle", 
                app_root_file, 
                false
              );
            if (should_init) {
              updateLoadingText("Initializing...");
    // Create a Timer
    Timer timer = new Timer();

    // Schedule the task to update the loading text after 2 seconds
    timer.schedule(new TimerTask() {
        @Override
        public void run() {
            updateLoadingText("Loading...");
        }
    }, 6000); // 2000 milliseconds = 2 seconds 

            } else {updateLoadingText("");}
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // Figure out the directory where the game is. If the game was
            // given to us via an intent, then we use the scheme-specific
            // part of that intent to determine the file to launch. We
            // also use the android.txt file to determine the orientation.
            //
            // Otherwise, we use the public data, if we have it, or the
            // private data if we do not.
            mActivity.finishLoad();

            // finishLoad called setContentView with the SDL view, which
            // removed the loading screen. However, we still need it to
            // show until the app is ready to render, so pop it back up
            // on top of the SDL view.
            mActivity.showLoadingScreenAgain();

            String app_root_dir = getAppRoot();
            if (getIntent() != null && getIntent().getAction() != null &&
                    getIntent().getAction().equals("org.kivy.LAUNCH")) {
                File path = new File(getIntent().getData().getSchemeSpecificPart());

                Project p = Project.scanDirectory(path);
                String entry_point = getEntryPoint(p.dir);
                SDLActivity.nativeSetenv("ANDROID_ENTRYPOINT", p.dir + "/" + entry_point);
                SDLActivity.nativeSetenv("ANDROID_ARGUMENT", p.dir);
                SDLActivity.nativeSetenv("ANDROID_APP_PATH", p.dir);

                if (p != null) {
                    if (p.landscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                }

                // Let old apps know they started.
                try {
                    FileWriter f = new FileWriter(new File(path, ".launch"));
                    f.write("started");
                    f.close();
                } catch (IOException e) {
                    // pass
                }
            } else {
                String entry_point = getEntryPoint(app_root_dir);
                SDLActivity.nativeSetenv("ANDROID_ENTRYPOINT", entry_point);
                SDLActivity.nativeSetenv("ANDROID_ARGUMENT", app_root_dir);
                SDLActivity.nativeSetenv("ANDROID_APP_PATH", app_root_dir);
            }

            String mFilesDirectory = mActivity.getFilesDir().getAbsolutePath();
            Log.v(TAG, "Setting env vars for start.c and Python to use");
            SDLActivity.nativeSetenv("ANDROID_PRIVATE", mFilesDirectory);
            SDLActivity.nativeSetenv("ANDROID_UNPACK", app_root_dir);
            SDLActivity.nativeSetenv("PYTHONHOME", app_root_dir);
            SDLActivity.nativeSetenv("PYTHONPATH", app_root_dir + ":" + app_root_dir + "/lib");
            SDLActivity.nativeSetenv("PYTHONOPTIMIZE", "2");

            try {
                Log.v(TAG, "Access to our meta-data...");
                mActivity.mMetaData = mActivity.getPackageManager().getApplicationInfo(
                        mActivity.getPackageName(), PackageManager.GET_META_DATA).metaData;

                PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
                if ( mActivity.mMetaData.getInt("wakelock") == 1 ) {
                    mActivity.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Screen On");
                    mActivity.mWakeLock.acquire();
                }
                if ( mActivity.mMetaData.getInt("surface.transparent") != 0 ) {
                    Log.v(TAG, "Surface will be transparent.");
                    getSurface().setZOrderOnTop(true);
                    getSurface().getHolder().setFormat(PixelFormat.TRANSPARENT);
                } else {
                    Log.i(TAG, "Surface will NOT be transparent");
                }
            } catch (PackageManager.NameNotFoundException e) {
            }

            // Launch app if that hasn't been done yet:
            if (mActivity.mHasFocus && (
                    // never went into proper resume state:
                    mActivity.mCurrentNativeState == NativeState.INIT ||
                    (
                    // resumed earlier but wasn't ready yet
                    mActivity.mCurrentNativeState == NativeState.RESUMED &&
                    mActivity.mSDLThread == null
                    ))) {
                // Because sometimes the app will get stuck here and never
                // actually run, ensure that it gets launched if we're active:
                mActivity.resumeNativeThread();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    public static ViewGroup getLayout() {
        return   mLayout;
    }

    public static SurfaceView getSurface() {
        return   mSurface;
    }

    //----------------------------------------------------------------------------
    // Listener interface for onNewIntent
    //

    public interface NewIntentListener {
        void onNewIntent(Intent intent);
    }

    private List<NewIntentListener> newIntentListeners = null;

    public void registerNewIntentListener(NewIntentListener listener) {
        if ( this.newIntentListeners == null )
            this.newIntentListeners = Collections.synchronizedList(new ArrayList<NewIntentListener>());
        this.newIntentListeners.add(listener);
    }

    public void unregisterNewIntentListener(NewIntentListener listener) {
        if ( this.newIntentListeners == null )
            return;
        this.newIntentListeners.remove(listener);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if ( this.newIntentListeners == null )
            return;
        this.onResume();
        synchronized ( this.newIntentListeners ) {
            Iterator<NewIntentListener> iterator = this.newIntentListeners.iterator();
            while ( iterator.hasNext() ) {
                (iterator.next()).onNewIntent(intent);
            }
        }
    }

    //----------------------------------------------------------------------------
    // Listener interface for onActivityResult
    //

    public interface ActivityResultListener {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    private List<ActivityResultListener> activityResultListeners = null;

    public void registerActivityResultListener(ActivityResultListener listener) {
        if ( this.activityResultListeners == null )
            this.activityResultListeners = Collections.synchronizedList(new ArrayList<ActivityResultListener>());
        this.activityResultListeners.add(listener);
    }

    public void unregisterActivityResultListener(ActivityResultListener listener) {
        if ( this.activityResultListeners == null )
            return;
        this.activityResultListeners.remove(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ( this.activityResultListeners == null )
            return;
        this.onResume();
        synchronized ( this.activityResultListeners ) {
            Iterator<ActivityResultListener> iterator = this.activityResultListeners.iterator();
            while ( iterator.hasNext() )
                (iterator.next()).onActivityResult(requestCode, resultCode, intent);
        }
    }

    public static void start_service(
            String serviceTitle,
            String serviceDescription,
            String pythonServiceArgument
            ) {
        _do_start_service(
            serviceTitle, serviceDescription, pythonServiceArgument, true
        );
    }

    public static void start_service_not_as_foreground(
            String serviceTitle,
            String serviceDescription,
            String pythonServiceArgument
            ) {
        _do_start_service(
            serviceTitle, serviceDescription, pythonServiceArgument, false
        );
    }

    public static void _do_start_service(
            String serviceTitle,
            String serviceDescription,
            String pythonServiceArgument,
            boolean showForegroundNotification
            ) {
        Intent serviceIntent = new Intent(PythonActivity.mActivity, PythonService.class);
        String argument = PythonActivity.mActivity.getFilesDir().getAbsolutePath();
        String app_root_dir = PythonActivity.mActivity.getAppRoot();
        String entry_point = PythonActivity.mActivity.getEntryPoint(app_root_dir + "/service");
        serviceIntent.putExtra("androidPrivate", argument);
        serviceIntent.putExtra("androidArgument", app_root_dir);
        serviceIntent.putExtra("serviceEntrypoint", "service/" + entry_point);
        serviceIntent.putExtra("pythonName", "python");
        serviceIntent.putExtra("pythonHome", app_root_dir);
        serviceIntent.putExtra("pythonPath", app_root_dir + ":" + app_root_dir + "/lib");
        serviceIntent.putExtra("serviceStartAsForeground",
            (showForegroundNotification ? "true" : "false")
        );
        serviceIntent.putExtra("serviceTitle", serviceTitle);
        serviceIntent.putExtra("serviceDescription", serviceDescription);
        serviceIntent.putExtra("pythonServiceArgument", pythonServiceArgument);
        PythonActivity.mActivity.startService(serviceIntent);
    }

    public static void stop_service() {
        Intent serviceIntent = new Intent(PythonActivity.mActivity, PythonService.class);
        PythonActivity.mActivity.stopService(serviceIntent);
    }

    /** Loading screen view **/
    public static ImageView mImageView = null;
    public static LinearLayout SplashParent = null;
    public static View mLottieView = null;
    /** Whether main routine/actual app has started yet **/
    protected boolean mAppConfirmedActive = false;
    /** Timer for delayed loading screen removal. **/
    protected Timer loadingScreenRemovalTimer = null; 

    // Overridden since it's called often, to check whether to remove the
    // loading screen:
    @Override
    protected boolean sendCommand(int command, Object data) {
        boolean result = super.sendCommand(command, data);
        considerLoadingScreenRemoval();
        return result;
    }
   
    /** Confirm that the app's main routine has been launched.
     **/
    @Override
    public void appConfirmedActive() {
        if (!mAppConfirmedActive) {
            Log.v(TAG, "appConfirmedActive() -> preparing loading screen removal");
            mAppConfirmedActive = true;
            considerLoadingScreenRemoval();
        }
    }

    /** This is called from various places to check whether the app's main
     *  routine has been launched already, and if it has, then the loading
     *  screen will be removed.
     **/
    public void considerLoadingScreenRemoval() {
        if (loadingScreenRemovalTimer != null)
            return;
        runOnUiThread(new Runnable() {
            public void run() {
                if (((PythonActivity)PythonActivity.mSingleton).mAppConfirmedActive &&
                        loadingScreenRemovalTimer == null) {
                    // Remove loading screen but with a delay.
                    // (app can use p4a's android.loadingscreen module to
                    // do it quicker if it wants to)
                    // get a handler (call from main thread)
                    // this will run when timer elapses
                    TimerTask removalTask = new TimerTask() {
                        @Override
                        public void run() {
                            // post a runnable to the handler
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    PythonActivity activity =
                                        ((PythonActivity)PythonActivity.mSingleton);
                                    if (activity != null)
                                        activity.removeLoadingScreen();
                                }
                            });
                        }
                    };
                }
            }
        });
    }

    public void removeLoadingScreenMain() {
        updateLoadingText("Running");
        runOnUiThread(new Runnable() {
            public void run() {
                SplashParent.removeView(mLottieView);
                SplashParent.setVisibility(View.GONE);
                mLottieView = null;
                mImageView = null;
                }
            });
    }

    public void removeLoadingScreen() {}

    @Override
    public void onActionModeStarted(ActionMode mode) {
        Menu menu = mode.getMenu();
        menu.removeGroup(menu.size() - 1);
        super.onActionModeStarted(mode);
    }

    public String getEntryPoint(String search_dir) {
        /* Get the main file (.pyc|.py) depending on if we
         * have a compiled version or not.
        */
        List<String> entryPoints = new ArrayList<String>();
        entryPoints.add("main.pyc");  // python 3 compiled files
		for (String value : entryPoints) {
            File mainFile = new File(search_dir + "/" + value);
            if (mainFile.exists()) {
                return value;
            }
        }
        return "main.py";
    }

    protected void showLoadingScreenAgain() {
        if (SplashParent != null) {
          mLayout.addView(SplashParent);
        };
    }

    protected void showLoadingScreen(View[] views) {
        try {
            SplashParent = new LinearLayout(this);
            addContentView(SplashParent, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
            setBackgroundColor(SplashParent, "#FF00AD", false);
            SplashParent.addView(views[0]);
            SplashParent.addView(views[1]);
            new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animateStaticImage();
            }
            }, 2500);
        } catch (IllegalStateException e) {
            // The loading screen can be attempted to be applied twice if app
            // is tabbed in/out, quickly.
            // (Gives error "The specified child already has a parent.
            // You must call removeView() on the child's parent first.")
        }
    }

    private String readJson(String folder, String file_name){
        StringBuilder sb = new StringBuilder();
        try {
            File file = new File(folder, file_name);
            if (!file.exists()) {
                return null;
            }
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line = "";
            try {
                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (line != null) {
                sb.append(line).append('\n');
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private Object readSettings(String key){
        Object value = null;
        try {
            Object sb = readJson(getFilesDir().toString(), "settings.json");
            if (sb == null) {
                return sb;
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            if (jsonObject.has(key)) {
                value = jsonObject.get(key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    protected void setBackgroundColor(View view, String color, boolean animate) {
      String backgroundColor;
      
      backgroundColor = "#FF00AD";

      if (backgroundColor != null) {
          try {
              int parsedColor = Color.parseColor(backgroundColor);
              if (animate) {
                  ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), view.getDrawingCacheBackgroundColor(), parsedColor);
                  colorAnimation.setDuration(500);
                  colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                      @Override
                      public void onAnimationUpdate(ValueAnimator animator) {
                          view.setBackgroundColor((int) animator.getAnimatedValue());
                      }
                  });
                  colorAnimation.start();
              } else {
                  view.setBackgroundColor(parsedColor);
              }
          } catch (IllegalArgumentException e) {
              e.printStackTrace();
          }
      }
    }

    private String getFiletheme(String dark_file, String light_file, String fallback) {
        Object gtype = readSettings("gtype");
        if (gtype == null) {
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    return dark_file;
                case Configuration.UI_MODE_NIGHT_NO:
                    return light_file;
                default:
                    return fallback;
            }
        }
        gtype = gtype.toString();
        String grad = readSettings("grad").toString();
        
        if (gtype.equals("Basic")) {
          if (grad.equals("Light")) {
            return light_file;
          } else { return dark_file;}
        }

        else {
            if (Arrays.asList("Light", "Medium").contains(gtype)) {
                return light_file;
            } 
            else {
                return dark_file;
            }
        }
    }

    protected View[] getLoadingScreen() {
        if (mLottieView != null || mImageView != null) {
            View[] _view = {mImageView, mLottieView};
            return _view;

        }
        String filename = "fore"; //  getFiletheme("splash_dark", "splash_light", "splash_dark");
        int presplashId = this.resourceManager.getIdentifier(filename , "drawable");
        InputStream is = this.getResources().openRawResource(presplashId);
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {};
        }
        mImageView = new ImageView(this);
        mImageView.setImageBitmap(bitmap);
        mImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mLottieView = getLayoutInflater().inflate(
        mActivity.resourceManager.getIdentifier("lottie", "layout"),
        mLayout,
        false
        );
        String filename_ = getFiletheme("splash_dark_lottie.json","splash_light_lottie.json","splash_dark_lottie.json");
        LottieAnimationView lottieView = mLottieView.findViewById(mActivity.resourceManager.getIdentifier("progressBar", "id"));
        
        updateLoadingText("");
        TextView loadingText = mLottieView.findViewById(
            mActivity.resourceManager.getIdentifier("loadingText", "id")
        );
        if (filename_.equals("splash_dark_lottie.json")) {loadingText.setTextColor(Color.WHITE);} else {loadingText.setTextColor(Color.BLACK);}

        lottieView.setAnimation(filename_);
        lottieView.loop(true);
        lottieView.playAnimation();
        
        mLottieView.setAlpha(0);
        
        View[] _view = {mImageView, mLottieView};
        return _view;
    }

    public void updateLoadingText(String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLottieView != null) {
                    TextView loadingText = mLottieView.findViewById(
                        mActivity.resourceManager.getIdentifier("loadingText", "id")
                    );
                    if (loadingText != null) {
                        loadingText.setText(text);
                    } else {
                        Log.e("python", "loadingText TextView not found");
                    }
                } else {
                    Log.e("python", "mLottieView is null");
                }
            }
        });
    }


    private void animateStaticImage() {
        setBackgroundColor(SplashParent, "auto", true);
        if (mImageView != null) {
            mImageView.animate()
                    .translationYBy(-500)
                    .alpha(0)
                    .setDuration(500)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            SplashParent.removeView(mImageView);
                            if (mLottieView != null) {
                              mLottieView.animate().alpha(1).setDuration(700);
                            }
                        }
                    })
                    .start();
        } else {
            // Handle the case where mImageView is null
            Log.e("animateStaticImage", "mImageView is null");
        }
    }

    @Override
    protected void onPause() {
        if (this.mWakeLock != null && mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }

        Log.v(TAG, "onPause()");
        try {
            super.onPause();
        } catch (UnsatisfiedLinkError e) {
            // Catch pause while still in loading screen failing to
            // call native function (since it's not yet loaded)
        }
    }

    @Override
    protected void onResume() {
        if (this.mWakeLock != null) {
            this.mWakeLock.acquire();
        }
        Log.v(TAG, "onResume()");
        try {
            super.onResume();
        } catch (UnsatisfiedLinkError e) {
            // Catch resume while still in loading screen failing to
            // call native function (since it's not yet loaded)
        }
        considerLoadingScreenRemoval();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        try {
            super.onWindowFocusChanged(hasFocus);
        } catch (UnsatisfiedLinkError e) {
            // Catch window focus while still in loading screen failing to
            // call native function (since it's not yet loaded)
        }
        considerLoadingScreenRemoval();
    }

    /**
     * Used by android.permissions p4a module to register a call back after
     * requesting runtime permissions
     **/
    public interface PermissionsCallback {
        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
    }

    private PermissionsCallback permissionCallback;
    private boolean havePermissionsCallback = false;

    public void addPermissionsCallback(PermissionsCallback callback) {
        permissionCallback = callback;
        havePermissionsCallback = true;
        Log.v(TAG, "addPermissionsCallback(): Added callback for onRequestPermissionsResult");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult()");
        if (havePermissionsCallback) {
            Log.v(TAG, "onRequestPermissionsResult passed to callback");
            permissionCallback.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Used by android.permissions p4a module to check a permission
     **/
    public boolean checkCurrentPermission(String permission) {
        if (android.os.Build.VERSION.SDK_INT < 23)
            return true;

        try {
            java.lang.reflect.Method methodCheckPermission =
                Activity.class.getMethod("checkSelfPermission", String.class);
            Object resultObj = methodCheckPermission.invoke(this, permission);
            int result = Integer.parseInt(resultObj.toString());
            if (result == PackageManager.PERMISSION_GRANTED) 
                return true;
        } catch (IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
        }
        return false;
    }

    /**
     * Used by android.permissions p4a module to request runtime permissions
     **/
    public void requestPermissionsWithRequestCode(String[] permissions, int requestCode) {
        if (android.os.Build.VERSION.SDK_INT < 23)
            return;
        try {
            java.lang.reflect.Method methodRequestPermission =
                Activity.class.getMethod("requestPermissions",
                String[].class, int.class);
            methodRequestPermission.invoke(this, permissions, requestCode);
        } catch (IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
        }
    }

    public void requestPermissions(String[] permissions) {
        requestPermissionsWithRequestCode(permissions, 1);
    }

    public static void changeKeyboard(int inputType) {
      if (SDLActivity.keyboardInputType != inputType){
          SDLActivity.keyboardInputType = inputType;
          InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.restartInput(mTextEdit);
          }
    }
}
