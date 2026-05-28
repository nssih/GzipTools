package com.tools.handler;

import com.tools.action.BaseAction;
import com.tools.constant.Constants;
import com.tools.core.GzipOpertion;
import com.tools.show.Show;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.util.Base64;

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
            // 从原始 GZIP bytes 的 offset 4-7 读 mtime（小端序）
            long mtime = 0;
            try {
                String cleaned = text.replaceAll("\\s", "");
                int mod = cleaned.length() % 4;
                if (mod != 0) cleaned += "====".substring(mod);
                byte[] raw = Base64.getDecoder().decode(cleaned);
                if (raw.length >= 8) {
                    mtime = (raw[4] & 0xFFL)
                          | ((raw[5] & 0xFFL) << 8)
                          | ((raw[6] & 0xFFL) << 16)
                          | ((raw[7] & 0xFFL) << 24);
                }
            } catch (Exception ignored) {}
            Show.showResult(result, mtime);
        }
    }

}
