package com.nexus.kernel.manager;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.topjohnwu.superuser.Shell;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建一个简单的文字显示，不需要额外的 XML 布局
        TextView tv = new TextView(this);
        tv.setTextSize(20f);
        tv.setPadding(50, 50, 50, 50);
        setContentView(tv);

        // 使用 libsu 异步检查 Root 权限
        Shell.getShell(shell -> {
            if (shell.isRoot()) {
                tv.setText("一加平板 Pro：已获取 Root 权限\n内核管理环境：就绪");
            } else {
                tv.setText("未检测到 Root 权限\n请在 Magisk/KernelSU 中授权");
            }
        });
    }
}