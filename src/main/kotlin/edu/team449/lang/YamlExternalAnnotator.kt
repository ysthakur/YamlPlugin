package edu.team449.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlExternalAnnotator : ExternalAnnotator<YAMLKeyValue, YamlJavaClassOrConstructorReference>() {

    override fun collectInformation(file: PsiFile): YAMLKeyValue? {
        for (element in file.children) {
            val res = getFirstClassRef(element)
            if (res != null) {
                return res
            }
        }
        return null
    }

    override fun doAnnotate(collectedInfo: YAMLKeyValue): YamlJavaClassOrConstructorReference? {
        return YamlJavaClassOrConstructorReference(collectedInfo)
    }

    override fun apply(file: PsiFile, annotationResult: YamlJavaClassOrConstructorReference?, holder: AnnotationHolder) {
        if (annotationResult != null)
            holder.createErrorAnnotation(annotationResult.element, "Something")
        else {
            Logger.getInstance(this::class.java).warn("annotation result == null")
        }
    }

    private fun getFirstClassRef(element: PsiElement): YAMLKeyValue? {
        if (isElemClassRef(element)) return element as YAMLKeyValue
        else {
            for (child in element.children) {
                val res = getFirstClassRef(child)
                if (res != null) return res
            }
            return null
        }
    }

    private fun isOrContainsClassRef(element: PsiElement): Boolean {
        if (isElemClassRef(element)) return true
        else {
            for (child in element.children) {
                if (isElemClassRef(child)) return true
            }
            return false
        }
    }

    private fun isElemClassRef(element: PsiElement) =
        element is YAMLKeyValue &&
                element.key!!.text.matches(
                    Regex("""([A-Z_][A-Z_0-9]*\.)+[A-Z_][A-Z_0-9]*""")
                )


}