package com.nexus.kernel.manager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.topjohnwu.superuser.Shell;
import java.io.*;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 1. 加载驱动
    static {
        System.loadLibrary("nexus-native-lib");
    }

    // 声明 C++ 原生检测方法
    public native String getKernelVersion();
    public native boolean checkNativePrivilege(); // 新增：内核级权限校验

    private TextView txtStatus;
    private ActivityResultLauncher<Intent> pickerLauncher;
    private static final String ENGINE_NAME = "lib_jni_sys_core.so"; // 伪装工具名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 绑定
        txtStatus = findViewById(R.id.txt_status);
        TextView txtKernel = findViewById(R.id.txt_kernel);
        MaterialButton btnPatch = findViewById(R.id.btn_patch_file);
        MaterialButton btnModules = findViewById(R.id.btn_modules);
        MaterialButton btnReboot = findViewById(R.id.btn_direct_install);

        // 2. 初始化环境显示
        txtKernel.setText("内核层: " + getKernelVersion());
        validatePrivilege();

        // 3. 注册镜像选择器（用于修补功能）
        pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    executeGhostPatch(result.getData().getData());
                }
            }
        );

        // 4. 核心功能绑定
        btnPatch.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("*/*");
            pickerLauncher.launch(intent);
        });

        btnModules.setOnClickListener(v -> detectModulesStealthily());

        btnReboot.setOnClickListener(v -> {
            Shell.cmd("rm -rf " + getFilesDir() + "/*", "reboot bootloader").submit();
        });
    }

    /**
     * 核心：双重验证授权系统
     */
    private void validatePrivilege() {
        boolean isNativeRoot = checkNativePrivilege();
        Shell.getShell(shell -> {
            boolean isShellRoot = shell.isRoot();
            runOnUiThread(() -> {
                if (isNativeRoot && isShellRoot) {
                    txtStatus.setText("认证状态: 已通过 (System Mode)");
                    txtStatus.setTextColor(0xFF00C853); // 绿色
                } else {
                    txtStatus.setText("认证状态: 待授权");
                    txtStatus.setTextColor(0xFFD50000); // 红色
                }
            });
        });
    }

    /**
     * 核心：去特征化模块探测
     */
    private void detectModulesStealthily() {
        String modPath = "/data/adb/modules";
        Shell.cmd("[ -d " + modPath + " ] && ls -1 " + modPath).submit(result -> {
            if (result.isSuccess()) {
                List<String> modules = result.getOut();
                runOnUiThread(() -> Toast.makeText(this, "检测到环境项: " + modules.size(), Toast.LENGTH_SHORT).show());
            } else {
                runOnUiThread(() -> Toast.makeText(this, "读取受限，请确认 Root 授权", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * 核心：幽灵修补流程 (释放 -> 修补 -> 自毁)
     */
    private void executeGhostPatch(Uri uri) {
        Toast.makeText(this, "正在建立隔离修补环境...", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            File engine = new File(getFilesDir(), ENGINE_NAME);
            try {
                // A. 瞬时释放引擎 (从 assets 里的 .so 伪装文件读取)
                try (InputStream is = getAssets().open("lib_patch_engine.so");
                     OutputStream os = new FileOutputStream(engine)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
                }
                engine.setExecutable(true, false);

                // B. 拷贝待修补镜像到私有目录
                File bootImg = new File(getFilesDir(), "temp_boot.img");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(bootImg)) {
                    byte[] buf = new byte[8192]; int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }

                // C. 指令链执行：解包 -> 修补 -> 清理 -> 物理擦除引擎
                String workDir = getFilesDir().getPath();
                String outPath = "/sdcard/Download/nexus_patched_" + System.currentTimeMillis() + ".img";
                String cmd = "cd " + workDir + 
                             " && ./" + ENGINE_NAME + " unpack temp_boot.img" +
                             " && ./" + ENGINE_NAME + " patch temp_boot.img " + outPath + 
                             " && ./" + ENGINE_NAME + " cleanup" +
                             " && rm -f " + ENGINE_NAME + " temp_boot.img";

                Shell.cmd(cmd).submit(result -> {
                    runOnUiThread(() -> {
                        if (result.isSuccess()) {
                            Toast.makeText(this, "修补成功！保存至 Download 目录", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "修补流程异常，环境已重置", Toast.LENGTH_LONG).show();
                        }
                    });
                });

            } catch (Exception e) {
                if (engine.exists()) engine.delete();
                runOnUiThread(() -> Toast.makeText(this, "错误: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}