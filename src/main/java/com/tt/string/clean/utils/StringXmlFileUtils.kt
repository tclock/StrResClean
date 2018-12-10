package com.tt.string.clean.utils

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import org.jdom.JDOMException
import java.awt.Component
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.swing.JOptionPane


object StringXmlFileUtils {

    private const val STRINGS_FILE_NAME = "strings.xml"
    private const val DEFAULT_VALUES_DIR_NAME = "values"
    private const val LOCALIZED_VALUES_DIR_NAME = "values-"
    private const val TAG_STRING = "string"
    private const val ATTRIBUTE_NAME = "name"
    private const val TAG_RESOURCES = "resources"


    fun addStringTag(parentComponent: Component, project: Project, virtualFiles: Array<VirtualFile>, strName: String, value: String): Boolean {

        for (virtualFile in virtualFiles) {
            System.out.println("loop...")
            if (isDefaultStringDirectory(virtualFile.parent) && isStringsFile(virtualFile)) {
                System.out.println("has find...")
                val file = PsiManager.getInstance(project).findFile(virtualFile)
                return if (isExistStringName(file, strName)){
                    if (confirmConflictDialog(parentComponent)){
                        addToStringFileAndOpen(project, virtualFile, file, strName, value)
                        true
                    }else {
                        throw RuntimeException()
                    }
                }else {
                    addToStringFileAndOpen(project, virtualFile, file, strName, value)
                    true
                }
            }

            val childVirtualFile = virtualFile.children
            if (childVirtualFile.isNotEmpty()) {
                if (addStringTag(parentComponent, project, childVirtualFile, strName, value)){
                    return true
                }
            }
        }
        return false
    }

    private fun addToStringFileAndOpen(project: Project, virtualFile: VirtualFile, psiFile: PsiFile?, strName: String, value: String){
        addStringTag(project, psiFile, strName, value)
        FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, virtualFile), true)
    }

    private fun confirmConflictDialog(parentComponent: Component): Boolean{
        val result = JOptionPane.showConfirmDialog(parentComponent, "String name conflict! still add?",
                "An conflict Question",
                JOptionPane.YES_NO_OPTION)
        return result == JOptionPane.YES_OPTION
    }

    private fun isExistStringName(psiFile: PsiFile?, strName: String): Boolean{

        psiFile?.let {
            try {
                val bais = ByteArrayInputStream(it.text.toByteArray())

                val doc = JDOMUtil.loadDocument(bais)
                var root = doc.rootElement
                val resources = root.getChild(TAG_RESOURCES)
                if (resources != null) root = resources

                val elements = root.children
                for (element in elements) {
                    if (TAG_STRING == element.name) {
                        val key = element.getAttributeValue(ATTRIBUTE_NAME)
                        if (strName == key){
                            return true

                        }
                    }
                }
            } catch (e: JDOMException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        return false
    }

    private fun isStringsFile(virtualFile: VirtualFile): Boolean {
        return STRINGS_FILE_NAME == virtualFile.name
    }


    /**
     * add an string tag if the file is xml string file
     * @param project
     * @param file string xml file
     * @param strName string name
     * @param value
     */
    private fun addStringTag(project: Project, file: PsiFile?, strName: String, value: String) {
        if (isStringsXmlFile(file)) {

            val xmlFile = file as XmlFile
            val rootTag = xmlFile.rootTag

            rootTag?.let {
                WriteCommandAction.runWriteCommandAction(project) {

                    val childTag = it.createChildTag(TAG_STRING, it.namespace, null, false)
                    childTag.setAttribute(ATTRIBUTE_NAME, strName)
                    childTag.value.text = value
                    it.addSubTag(childTag, false)

                }
            }

        }
    }

    /**
     * @return true if the given PsiFile is a strings.xml file.
     */
    private fun isStringsXmlFile(file: PsiFile?): Boolean {
        return file is XmlFile && file.name == STRINGS_FILE_NAME
    }

    private fun isDefaultStringDirectory(file: VirtualFile): Boolean {
        return file.name == DEFAULT_VALUES_DIR_NAME
    }

    /**
     * Returns true if the given file is the default strings.xml file
     */
    fun isLocalizedStringsFile(file: PsiFile): Boolean {
        if (!isStringsXmlFile(file))
            return false

        val dir = file.containingDirectory
        return dir != null && dir.name.startsWith(LOCALIZED_VALUES_DIR_NAME)
    }

    /**
     * Returns default strings.xml file.
     * @param currentFile One of localized strings files.
     */
    fun getDefaultStringsFile(currentFile: PsiFile): PsiFile? {
        val currentDir = currentFile.containingDirectory ?: return null
        // If the specified file is from the "values" directory, it's already our default strings file
        if (currentDir.name == DEFAULT_VALUES_DIR_NAME)
            return currentFile

        val defaultDir: PsiDirectory? = currentDir.parentDirectory?.findSubdirectory(DEFAULT_VALUES_DIR_NAME)
                ?: return null

        return defaultDir?.findFile(STRINGS_FILE_NAME)
    }


}