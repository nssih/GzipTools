package com.example.handler;

import com.example.action.BaseAction;
import com.example.constant.Constants;
import com.example.core.GzipOpertion;
import com.example.show.Show;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * 解开压缩的方法
 */
public class GzipUncompressAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String text = getSelectedText(e);
        if (text == null) {
            return;
        }

        String result = GzipOpertion.uncompress(text);
        if (result.startsWith(Constants.ERR)) {
            Show.notifyError(result);
        } else {
            Show.showResult(result);
        }
    }

}
