package com.nexus.kernel.manager;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.topjohnwu.superuser.Shell;

public class MainActivity extends AppCompatActivity {

    // 1. 加载 C++ 库
    static {
        System.loadLibrary("nexus-kernel-lib");
    }

    // 2. 声明 C++ 方法
    public native String getKernelStatusFromCpp();
    public native boolean checkModuleExists(String moduleName);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusTxt = findViewById(R.id.txt_status);
        TextView kernelTxt = findViewById(R.id.txt_kernel_info);
        MaterialButton btnGrant = findViewById(R.id.btn_grant_root);
        MaterialButton btnModules = findViewById(R.id.btn_modules);

        // 调用 C++ 获取内核信息
        kernelTxt.setText("内核(Cpp): " + getKernelStatusFromCpp());

        // 3. 授权管理 (Magisk/KSU 兼容逻辑)
        btnGrant.setOnClickListener(v -> {
            Shell.cmd("magisk --grant com.nexus.kernel.manager").submit(res -> {
                if (res.isSuccess()) {
                    Toast.makeText(this, "自授权成功 (Root 已确认)", Toast.LENGTH_SHORT).show();
                    statusTxt.setText("🛡️ Nexus 环境已授权");
                }
            });
        });

        // 4. 模块系统逻辑
        btnModules.setOnClickListener(v -> {
            // 通过 Java 调用 C++ 检查特定模块是否存在
            boolean hasZygisk = checkModuleExists("zygisk_next");
            Toast.makeText(this, "Zygisk 模块状态: " + (hasZygisk ? "已安装" : "未找到"), Toast.LENGTH_LONG).show();
        });
    }
}