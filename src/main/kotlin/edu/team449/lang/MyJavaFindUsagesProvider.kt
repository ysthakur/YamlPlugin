package edu.team449.lang

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.java.JavaFindUsagesProvider
import com.intellij.psi.PsiElement

/**
 * TODO actually implement this
 */
class MyJavaFindUsagesProvider : FindUsagesProvider {

    val javaProvider = JavaFindUsagesProvider()

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        javaProvider.getNodeText(element, useFullName)

    override fun getDescriptiveName(element: PsiElement): String =
        javaProvider.getDescriptiveName(element)

    override fun getType(element: PsiElement): String =
        javaProvider.getType(element)

    override fun getHelpId(psiElement: PsiElement): String? =
        javaProvider.getHelpId(psiElement)

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
        javaProvider.canFindUsagesFor(psiElement)
}