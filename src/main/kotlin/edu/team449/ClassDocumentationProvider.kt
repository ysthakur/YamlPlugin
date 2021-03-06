package edu.team449

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class ClassDocumentationProvider : DocumentationProvider {

  private val jDocProvider: JavaDocumentationProvider = JavaDocumentationProvider()

  override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): MutableList<String>? {
    return jDocProvider.getUrlFor(element?.let(::resolveRef), originalElement)
  }

  override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
    return jDocProvider.getQuickNavigateInfo(element?.let(::resolveRef), originalElement)
  }

  override fun getDocumentationElementForLookupItem(
    psiManager: PsiManager?,
    `object`: Any?,
    element: PsiElement?
  ): PsiElement? {
    return jDocProvider.getDocumentationElementForLookupItem(psiManager, `object`, element)
  }

  override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
    return jDocProvider.generateDoc(resolveRef(element) ?: element, originalElement)
  }

  override fun getCustomDocumentationElement(
    editor: Editor,
    file: PsiFile,
    contextElement: PsiElement?,
    targetOffset: Int
  ): PsiElement? {
    return jDocProvider.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
  }

  override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
    return jDocProvider.generateHoverDoc(resolveRef(element) ?: element, originalElement)
  }

  override fun getDocumentationElementForLink(
    psiManager: PsiManager?,
    link: String?,
    context: PsiElement?
  ): PsiElement? {
    return jDocProvider.getDocumentationElementForLink(psiManager, link, context)
  }
}