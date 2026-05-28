package com.example.handler;

import com.example.action.BaseAction;
import com.example.core.GzipOpertion;
import com.example.show.Show;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * 不带时间戳的压缩方法
 */
public class GzipCompressNoTimeAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String text = getSelectedText(e);
        if (text == null) {
            return;
        }

        String result = GzipOpertion.compressSafe(text, false);

        Show.showResult(result, 0L);
    }
}

