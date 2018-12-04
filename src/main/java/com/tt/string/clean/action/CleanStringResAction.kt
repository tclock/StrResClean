package com.tt.string.clean.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.tt.string.clean.StringNamedDialog

class CleanStringResAction : AnAction() {


    override fun actionPerformed(e: AnActionEvent?) {

        e?.getData(PlatformDataKeys.PROJECT)?.let {
            showDialog(e)
        }

    }

    private fun showDialog(e: AnActionEvent) {
        val dialog = StringNamedDialog(e)
        dialog.pack()
        dialog.isVisible = true
    }


    override fun update(e: AnActionEvent?) {
        super.update(e)
        e?.presentation?.isVisible = e?.project != null;
    }

}