package com.bgmi;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import static com.bgmi.server.ApiServer.mainURL;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.bgmi.server.DownloadZip;
import com.bgmi.utils.AppManager;
import com.bgmi.utils.Downtwo;
import com.bgmi.utils.Prefs;
import com.bgmi.floating.FloatService;
import com.bgmi.floating.Overlay;
import com.bgmi.floating.ToggleAim;
import com.bgmi.floating.ToggleBullet;
import com.bgmi.floating.ToggleSimulation;
import com.topjohnwu.superuser.Shell;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.zcore.core.env.BEnvironment;
import com.zcore.ZCoreCore;
import com.zcore.entity.pm.InstallResult;

@Obfuscate
public class MAct extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("THUNDER");
        } catch (UnsatisfiedLinkError ignored) {}
    }

    private static final String PKG_BGMI = "com.pubg.imobile";
    private static final int USER_ID = 0;
    public static String modeselect;
    private ProgressBar progressBar;
    private TextView progressText;
    private static final String OBB_LOG_TAG = "LoaderObb";
    private final ActivityResultLauncher<String[]> obbPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::importObb);
    private AppManager appManager;
    private Prefs prefs;
    public static boolean Ischeck = false;
    public static int gameint = 1;
    public static int bitversi = 64;
    public static boolean modestatus = false;

    private LinearLayout mainContainer;
    private Button startBgmi;
    private LinearLayout progressCard;
    private LinearLayout countdownLayout;
    public static String game = "com.pubg.imobile";
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    public static String socket;
    public static String daemonPath;
    
    private static MAct instance;
    
    public static Context get() {
        return instance != null ? instance : null;
    }
    
    public static native String pp();
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        instance = this;
        
        prefs = new Prefs(this);
        Loadssets();
        if (!ischicken()) {
            Toast.makeText(this, "THUNDER PAPA HI KEHDE 😂😂", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!isExpiryDateValid()) {
            showToast("Invalid or expired license!");
            prefs.setSt("USER", "");
            startActivity(new Intent(this, LogAct.class));
            finish();
            return;
        }
        
        createUI();
        appManager = new AppManager(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MAct.this, FloatService.class));
        stopService(new Intent(MAct.this, Overlay.class));
        stopService(new Intent(MAct.this, ToggleBullet.class));
        stopService(new Intent(MAct.this, ToggleAim.class));
        stopService(new Intent(MAct.this, ToggleSimulation.class));
        finishAffinity();
        instance = null;
    }
    
    private boolean isExpiryDateValid() {
        try {
            String expiryDateStr = pp();
            if (expiryDateStr == null || expiryDateStr.isEmpty() || !expiryDateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return false;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date expiryDate = dateFormat.parse(expiryDateStr);
            long now = System.currentTimeMillis();
            return expiryDate.getTime() > now;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void createUI() {
        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setGravity(Gravity.CENTER);
        mainContainer.setBackgroundColor(Color.parseColor("#000000")); // BLACK background

        LinearLayout loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dpToPx(50));
        loadingLayout.setLayoutParams(layoutParams);

        android.graphics.Typeface pixelFont = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.pixel);

        // App Logo Image
        android.widget.ImageView appLogoInfo = new android.widget.ImageView(this);
        appLogoInfo.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dpToPx(100), dpToPx(100));
        logoParams.gravity = Gravity.CENTER;
        logoParams.setMargins(0, 0, 0, dpToPx(20));
        appLogoInfo.setLayoutParams(logoParams);

        // App Name - ORANGE color
        TextView appLogoText = new TextView(this);
        appLogoText.setText(getString(R.string.app_name));
        appLogoText.setTextColor(Color.parseColor("#FF6600")); // ORANGE
        appLogoText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        appLogoText.setTypeface(pixelFont, android.graphics.Typeface.BOLD);
        appLogoText.setGravity(Gravity.CENTER);
        appLogoText.setPadding(0, 0, 0, dpToPx(10)); 
        
        // Subtitle - WHITE with alpha
        TextView subtitleText = new TextView(this);
        subtitleText.setText("ESP VISUAL SYSTEM v3.0");
        subtitleText.setTextColor(Color.parseColor("#FFFFFF"));
        subtitleText.setAlpha(0.6f);
        subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subtitleText.setTypeface(pixelFont);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setPadding(0, 0, 0, dpToPx(30));
        
        loadingLayout.addView(appLogoInfo);
        loadingLayout.addView(appLogoText);
        loadingLayout.addView(subtitleText);

        // Progress Text - WHITE
        progressText = new TextView(this);
        progressText.setTextColor(Color.parseColor("#FFFFFF"));
        progressText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        progressText.setGravity(Gravity.CENTER);
        progressText.setTypeface(pixelFont);
        progressText.setPadding(0, 0, 0, dpToPx(10));
        progressText.setText("Initializing...");

        // Progress Bar - ORANGE
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
            dpToPx(250),
            dpToPx(8)
        ));
        progressBar.setProgressDrawable(createRoundRectDrawable(Color.parseColor("#FF6600"), dpToPx(4)));
        progressBar.setProgress(0);
        progressBar.setMax(100);
        
        loadingLayout.addView(progressText);
        loadingLayout.addView(progressBar);

        startBgmi = new Button(this);
        startBgmi.setText("Import OBB");
        startBgmi.setVisibility(View.GONE);
        startBgmi.setOnClickListener(view -> obbPicker.launch(new String[]{"*/*"}));
        loadingLayout.addView(startBgmi);

        mainContainer.addView(loadingLayout);
        setContentView(mainContainer);

        new Handler(Looper.getMainLooper()).postDelayed(this::startAutoSequence, 1000);
    }
    
    private GradientDrawable createRoundRectDrawable(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(radius);
        shape.setColor(color);
        return shape;
    }
    
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            dp, 
            getResources().getDisplayMetrics()
        );
    }
    
    private void startAutoSequence() {
        if (!ZCoreCore.get().isInstalled(PKG_BGMI, USER_ID)) {
            progressText.setText("Installing Game Engine...");
            new Thread(() -> {
                InstallResult result = ZCoreCore.get().installPackageAsUser(PKG_BGMI, USER_ID);
                runOnUiThread(() -> {
                    if (result.success) {
                        checkAndCopyObb();
                    } else {
                        progressText.setText("Install Failed: " + result.msg);
                    }
                });
            }).start();
        } else {
            checkAndCopyObb();
        }
    }

    private void checkAndCopyObb() {
        progressText.setText("Checking OBB...");
        new Thread(() -> {
            try {
                File destination = getExpectedObb();
                try (FileInputStream input = new FileInputStream(destination)) {
                    if (!destination.isFile() || input.read() == -1) {
                        throw new IOException("Expected a nonempty OBB file");
                    }
                } catch (IOException | SecurityException e) {
                    Log.w(OBB_LOG_TAG, "Cannot use OBB at " + destination, e);
                    showObbImport("Select " + destination.getName() + " from Downloads or another accessible folder.");
                    return;
                }
                runOnUiThread(this::launchGameAuto);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(OBB_LOG_TAG, "Cannot determine the OBB filename for " + PKG_BGMI, e);
                runOnUiThread(() -> progressText.setText("Could not read the installed BGMI version."));
            }
        }).start();
    }

    private void launchGameAuto() {
        progressText.setText("Status: Launching Game...");
        progressBar.setIndeterminate(true);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            launchGame();
        }, 500);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private File getExpectedObb() throws PackageManager.NameNotFoundException {
        PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
        String filename = "main." + info.versionCode + "." + info.packageName + ".obb";
        return new File(BEnvironment.getExternalObbDir(PKG_BGMI), filename);
    }

    private void showObbImport(String message) {
        runOnUiThread(() -> {
            progressBar.setIndeterminate(false);
            progressText.setText(message);
            startBgmi.setVisibility(View.VISIBLE);
        });
    }

    private void importObb(Uri uri) {
        if (uri == null) {
            return;
        }

        startBgmi.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        progressText.setText("Importing OBB...");
        new Thread(() -> {
            File destination = null;
            File partial = null;
            try {
                destination = getExpectedObb();
                String filename = destination.getName();
                try (Cursor cursor = getContentResolver().query(uri,
                        new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor == null || !cursor.moveToFirst() || !filename.equals(cursor.getString(0))) {
                        showObbImport("Select the OBB named " + filename + ".");
                        return;
                    }
                }

                File directory = destination.getParentFile();
                if (directory == null || (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory())) {
                    throw new IOException("Cannot create " + directory);
                }

                partial = File.createTempFile("obb-", ".part", directory);
                try (InputStream input = getContentResolver().openInputStream(uri);
                     FileOutputStream output = new FileOutputStream(partial)) {
                    if (input == null) {
                        throw new IOException("The document provider could not open the selected file");
                    }
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }

                if (partial.length() == 0) {
                    throw new IOException("The selected OBB is empty");
                }
                if (!partial.renameTo(destination)) {
                    throw new IOException("Cannot publish the imported OBB");
                }

                runOnUiThread(this::launchGameAuto);
            } catch (IOException | RuntimeException | PackageManager.NameNotFoundException e) {
                Log.e(OBB_LOG_TAG, "Could not import OBB to " + destination, e);
                showObbImport("OBB import failed. Check available space or select another copy.");
            } finally {
                if (partial != null && partial.exists() && !partial.delete()) {
                    Log.w(OBB_LOG_TAG, "Could not remove incomplete OBB at " + partial);
                }
            }
        }).start();
    }

    public void Checking(){
        new DownloadZip(this).execute("1", mainURL());
    }

    private boolean ischicken() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Signature signature : signatures) {
                byte[] digest = md.digest(signature.toByteArray());
                StringBuilder hash = new StringBuilder();
                for (byte b : digest) {
                    hash.append(String.format("%02X", b));
                }
                if (hash.toString().equals("E545B00E31AF8FC008566DE0B87C0B17FC467A90AEED71B00755B98698AE58BC")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    void RunShell(String cmd) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void runant(final String nf){
        excpp("/"+nf);
    }

    private void ExecuteElf(String shell) {
        try {
            Runtime.getRuntime().exec(shell);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void excpp(String path) {
        try {
            ExecuteElf("chmod 777 " + getFilesDir() + path);
            ExecuteElf(getFilesDir() + path);
            ExecuteElf("su -c chmod 777 " + getFilesDir() + path);
            ExecuteElf("su -c " + getFilesDir() + path);
        } catch (Exception e) {
        }
    }

    private void Loadssets() {
        MoveAssets(getFilesDir() + "/", "sock64");
        MoveAssets(getFilesDir() + "/", "socu64");
        MoveAssets(getFilesDir() + "/", "socs32");
        MoveAssets(getFilesDir() + "/", "socu32");
        MoveAssets(getFilesDir() + "/", "TW");
        MoveAssets(getFilesDir() + "/", "VNG");
        MoveAssets(getFilesDir() + "/", "HakmeDev");
        MoveAssets(getFilesDir() + "/", "kernels64");
    }

    private boolean MoveAssets(String outPath, String fileName) {
        File file = new File(outPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("--Method--", "copyAssetsSingleFile: cannot create directory.");
                return false;
            }
        }
        try {
            InputStream inputStream = getAssets().open(fileName);
            File outFile = new File(file, fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = inputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            inputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (FloatService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startPatcher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 123);
            } else {
                startFloater();
            }
        }
    }

    private void startFloater() {
        startService(new Intent(MAct.this, FloatService.class));
        loadAssets();
    } 
        
    private void stopPatcher() {
        stopService(new Intent(MAct.this, FloatService.class));
        stopService(new Intent(MAct.this, Overlay.class));
        stopService(new Intent(MAct.this, ToggleAim.class));
        stopService(new Intent(MAct.this, ToggleBullet.class));
        stopService(new Intent(MAct.this, ToggleSimulation.class));
    }

    public void loadAssets() {
        String filepath = Environment.getExternalStorageDirectory() + "/Android/data/.tyb";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filepath);
            byte[] buffer = "DO NOT DELETE".getBytes();
            fos.write(buffer, 0, buffer.length);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        daemonPath = getFilesDir().toString() + "/sock64";
        if (Shell.rootAccess()) {
            socket = "su -c " + daemonPath;
        } else {
            socket = daemonPath;
        }
        try {
            Runtime.getRuntime().exec("chmod 777 " + daemonPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void launchGame() {
        try {
            ZCoreCore.get().launchApk(PKG_BGMI, USER_ID);
            startPatcher();
            showToast("Launching BGMI...");
        } catch (Exception e) {
            showToast("Launch failed: " + e.getMessage());
        }
    }
}