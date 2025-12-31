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

    // 加载底层 NDK 库
    static {
        System.loadLibrary("nexus-kernel-lib");
    }

    public native String getKernelStatusFromCpp();

    private TextView statusTxt;
    private ActivityResultLauncher<Intent> pickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化：释放并赋权 magiskboot 工具
        setupMagiskPath();

        statusTxt = findViewById(R.id.txt_status);
        TextView kernelTxt = findViewById(R.id.txt_kernel_info);
        MaterialButton btnReboot = findViewById(R.id.btn_direct_install);
        MaterialButton btnPatch = findViewById(R.id.btn_patch_file);
        MaterialButton btnGrant = findViewById(R.id.btn_grant_root);
        MaterialButton btnModules = findViewById(R.id.btn_modules);

        // 显示内核信息
        kernelTxt.setText("内核(NDK): " + getKernelStatusFromCpp());
        checkRootStatus();

        // 2. 注册文件选择器（修补镜像用）
        pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleImagePatch(result.getData().getData());
                }
            }
        );

        // 3. 按钮功能绑定
        btnReboot.setText("重启至 Bootloader");
        btnReboot.setOnClickListener(v -> Shell.cmd("reboot bootloader").submit());

        btnPatch.setText("选择并修补 Boot.img");
        btnPatch.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            pickerLauncher.launch(intent);
        });

        btnGrant.setOnClickListener(v -> checkRootStatus());

        btnModules.setOnClickListener(v -> {
            Shell.cmd("ls /data/adb/modules").submit(res -> {
                if (res.isSuccess()) {
                    List<String> modules = res.getOut();
                    String msg = modules.isEmpty() ? "未发现已安装模块" : "发现 " + modules.size() + " 个模块";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "读取失败，请检查 Root 授权", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // 将 Assets 里的 magiskboot 释放到 /data/data/ 下并执行 chmod +x
    private void setupMagiskPath() {
        File toolFile = new File(getFilesDir(), "magiskboot");
        try (InputStream in = getAssets().open("magiskboot");
             OutputStream out = new FileOutputStream(toolFile)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            // 关键：必须赋予执行权限
            toolFile.setExecutable(true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkRootStatus() {
        Shell.getShell(shell -> {
            if (shell.isRoot()) {
                statusTxt.setText("🛡️ Nexus 环境已激活 (Root)");
                statusTxt.setTextColor(getColor(android.R.color.holo_blue_dark));
            } else {
                statusTxt.setText("❌ 未检测到 Root 权限");
                statusTxt.setTextColor(getColor(android.R.color.holo_red_dark));
            }
        });
    }

    // 核心修补逻辑：拷贝 -> unpack -> patch -> cleanup
    private void handleImagePatch(Uri uri) {
        Toast.makeText(this, "正在修补内核镜像...", Toast.LENGTH_LONG).show();
        
        new Thread(() -> {
            try {
                String workDir = getFilesDir().getPath();
                String magiskPath = workDir + "/magiskboot";
                File inputFile = new File(workDir, "boot.img");
                
                // 将文件拷贝到应用私有目录，防止权限问题
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(inputFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }

                // 构造修补指令：解包 -> 修补 -> 清理
                String outPath = "/sdcard/Download/patched_boot.img";
                String cmd = "cd " + workDir + 
                             " && " + magiskPath + " unpack boot.img" +
                             " && " + magiskPath + " patch boot.img " + outPath + 
                             " && " + magiskPath + " cleanup";

                Shell.cmd(cmd).submit(result -> {
                    runOnUiThread(() -> {
                        if (result.isSuccess()) {
                            Toast.makeText(this, "修补成功！保存至: " + outPath, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "修补失败，请确认镜像格式是否正确", Toast.LENGTH_LONG).show();
                        }
                    });
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}