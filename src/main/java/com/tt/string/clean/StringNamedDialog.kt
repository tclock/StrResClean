package com.tt.string.clean

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.ui.JBColor
import com.tt.string.clean.utils.*
import org.apache.http.util.TextUtils
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*


class StringNamedDialog(private val actionEvent: AnActionEvent) : JDialog() {

    private lateinit var contentPane: JPanel
    private lateinit var buttonOK: JButton
    private lateinit var buttonCancel: JButton
    private lateinit var stringNameTextField: JTextField
    private lateinit var stringValueTextField: JTextField
    private lateinit var  replaceSelectedTextCheckBox: JCheckBox

    private var selectedText: String? = null

    private val psiFile: PsiFile? by lazy {
        actionEvent.getData(PlatformDataKeys.PSI_FILE)
    }

    init {

        setContentPane(contentPane)
        isModal = true
        getRootPane().defaultButton = buttonOK
        setDialogLocation()

        initListener()
        initView()
        setReplaceSelectedTextCheckBoxVisible()

        // call onCancel() when cross is clicked
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE

    }

    private fun initView(){
        val mEditor = actionEvent.getData(PlatformDataKeys.EDITOR) ?: return
        val model = mEditor.selectionModel
        selectedText = model.selectedText
        stringValueTextField.text = selectedText
    }

    private fun setDialogLocation(){
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        setLocation((screenSize.width - contentPane.preferredSize.width)/2, (screenSize.height - contentPane.preferredSize.height)/2 - 50)
    }

    private fun initListener() {
        buttonOK.addActionListener { onOK() }

        buttonCancel.addActionListener { onCancel() }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                onCancel()
            }
        })

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction({ onCancel() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)

    }

    private fun setReplaceSelectedTextCheckBoxVisible() {
        psiFile?.let {
            if (it.isJavaFile() || it.isKotlinFile() || it.isXmlFile()) {
                replaceSelectedTextCheckBox.isVisible = !TextUtils.isEmpty(selectedText)
            } else {
                replaceSelectedTextCheckBox.isVisible = false
            }

        }

    }


    private fun onOK() {
        // add your code here
        selectedText = stringValueTextField.text
        if (TextUtils.isEmpty(selectedText)) {
            stringValueTextField.border = BorderFactory.createLineBorder(JBColor.RED)
            stringValueTextField.requestFocus()
            return
        }

        val text = stringNameTextField.text
        if (TextUtils.isEmpty(text)) {
            stringNameTextField.border = BorderFactory.createLineBorder(JBColor.RED)
            stringNameTextField.requestFocus()
            return
        }

        actionEvent.getData(PlatformDataKeys.PROJECT)?.let {

            val module = ModuleUtil.findModuleForFile(psiFile)

            val virtualFiles = if (module != null){
                ModuleRootManager.getInstance(module).contentRoots
            }else {
                ModuleRootManager.getInstance(ModuleManager.getInstance(it).modules[0]).contentRoots
            }
            try {
                val result = StringXmlFileUtils.addStringTag(this, it, virtualFiles, text, stringValueTextField.text)

                if(result){
                    if (replaceSelectedTextCheckBox.isVisible && replaceSelectedTextCheckBox.isSelected) {
                        replaceSelectedText(it, text)
                    }
                    dispose()
                }
            }catch (e: Exception){

            }

        }


    }



    private fun replaceSelectedText(project: Project, stringName: String) {

        val editor = actionEvent.getRequiredData(CommonDataKeys.EDITOR)
        val selectionModel = editor.selectionModel

        var start = selectionModel.selectionStart
        var end = selectionModel.selectionEnd

        if(isReplaceQuotationMark(editor.document, start, end)){
            start -= 1
            end += 1
        }

        //Making the replacement
        WriteCommandAction.runWriteCommandAction(project
        ) { editor.document.replaceString(start, end, getReplaceTextByFileType(stringName)) }
        selectionModel.removeSelection()
    }


    /**
     * Returns replacement text by file type to replace the selected text.
     * if the file type is "xml", return "@string/{@param stringName}"
     * or is "kotlin", return "resources.getString(R.string.{@param stringName})"
     * or is "java", return "getResources().getString(R.string.{@param stringName})"
     * otherwise,  return "R.string.{@param stringName}".
     */
    private fun getReplaceTextByFileType(stringName: String): String{

        return if (psiFile.isXmlFile()) {
            "@string/$stringName"
        } else {

            val strId = "R.string.$stringName"
            psiFile.getPsiClass()?.let {
                if (it.isInheritActivity() || it.isInheritFragment()){
                    return if (psiFile.isKotlinFile()){
                        "resources.getString($strId)"
                    }else {
                        "getResources().getString($strId)"
                    }
                }
            }
            strId
        }
    }

    /**
     * Return true if the file type is not "xml" and the selected text is surrounded by quotation mark.
     */
    private fun isReplaceQuotationMark(document: Document, selectedStart: Int, selectedEnd: Int): Boolean{
        if(psiFile.isXmlFile()){
            return false
        }else {

            if(selectedStart in 1..(selectedEnd - 1)){
                val text = document.getText(TextRange.create(selectedStart - 1, selectedEnd + 1))
                if (text.startsWith("\"") && text.endsWith("\"")){
                    return true
                }
            }
            return false
        }
    }


    private fun onCancel() {
        // add your code here if necessary
        dispose()
    }

}
