package com.nexus.kernel.manager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("kernel_manager");
    }

    // 调用 C++ 层的修补函数
    private native int patchInitBoot(String inputPath, String outputPath);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建简单的 UI 布局 (也可以使用之前的 XML)
        setContentView(R.layout.activity_main); 

        Button patchBtn = findViewById(R.id.btn_patch); // 假设 XML 中 ID 为此
        TextView infoTxt = findViewById(R.id.txt_info);

        patchBtn.setOnClickListener(v -> {
            String input = "/sdcard/init_boot.img";
            String output = "/sdcard/init_boot_patched.img";

            if (!new File(input).exists()) {
                Toast.makeText(this, "未找到 init_boot.img，请放在存储根目录", Toast.LENGTH_LONG).show();
                return;
            }

            infoTxt.setText("状态: 正在修补内核镜像 (ARMv9)...");
            patchBtn.setEnabled(false);

            // 开启新线程运行，防止 UI 卡死
            new Thread(() -> {
                int result = patchInitBoot(input, output);
                runOnUiThread(() -> {
                    if (result == 0) {
                        infoTxt.setText("修补完成！\n请将 init_boot_patched.img 传至电脑刷入。");
                    } else {
                        infoTxt.setText("错误: 修补失败，请检查镜像格式。");
                    }
                    patchBtn.setEnabled(true);
                });
            }).start();
        });
    }
}