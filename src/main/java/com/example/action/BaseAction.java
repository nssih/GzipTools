package com.example.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.actionSystem.CommonDataKeys;

public abstract class BaseAction extends AnAction {

    protected String getSelectedText(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return null;

        return editor.getSelectionModel().getSelectedText();
    }
}
