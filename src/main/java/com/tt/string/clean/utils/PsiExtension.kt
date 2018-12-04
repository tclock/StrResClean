package com.tt.string.clean.utils

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache


const val XML_FILE_EOF = "xml"
const val JAVA_FILE_EOF = "java"
const val KOTLIN_FILE_EOF = "kotlin"


fun PsiFile?.getPsiClass(): PsiClass?{

    this?.let {
        val globalSearchScope = GlobalSearchScope.fileScope(it)
        val fullName = it.name
        val className = fullName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        return PsiShortNamesCache.getInstance(it.project).getClassesByName(className, globalSearchScope)[0]
    }
    return null
}

fun PsiFile?.isXmlFile(): Boolean{
    return this?.fileType?.name?.toLowerCase() == XML_FILE_EOF
}

fun PsiFile?.isJavaFile(): Boolean{
    return this?.fileType?.name?.toLowerCase() == JAVA_FILE_EOF
}

fun PsiFile?.isKotlinFile(): Boolean{
    return this?.fileType?.name?.toLowerCase() == KOTLIN_FILE_EOF
}

fun PsiClass?.isInheritFragment(): Boolean{

    this?.let {
        val scope = GlobalSearchScope.allScope(it.project)
        val fragmentClass = JavaPsiFacade.getInstance(it.project).findClass(
                "android.app.Fragment", scope)
        val supportFragmentClass = JavaPsiFacade.getInstance(it.project).findClass(
                "android.support.v4.app.Fragment", scope)
        return (fragmentClass != null && it.isInheritor(fragmentClass, true) ) || (supportFragmentClass != null && it.isInheritor(supportFragmentClass,true))
    }

    return false
}

fun PsiClass?.isInheritActivity(): Boolean{
    this?.let {
        val scope = GlobalSearchScope.allScope(it.project)
        val activityClass = JavaPsiFacade.getInstance(it.project).findClass(
                "android.app.Activity", scope)
        return activityClass != null && it.isInheritor(activityClass, true)
    }

    return false
}

