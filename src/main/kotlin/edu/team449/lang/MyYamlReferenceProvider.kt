package edu.team449.lang

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue

object MyYamlReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        //todo implement this
        when (element) {
            is YAMLKeyValue -> {
                if (element.key?.text?.matches(Regex("""([A-Z_][A-Z_0-9]*\.)+[A-Z_][A-Z_0-9]*""")) == true) {
                    return arrayOf(YamlJavaClassReference(element))
                }
            }
        }

        return emptyArray()
    }

    /*
     @NotNull
                fun getReferencesByElement(
                    @NotNull element: PsiElement,
                    @NotNull context: ProcessingContext?
                ): Array<PsiReference>? {
                    val literalExpression: PsiLiteralExpression = element as PsiLiteralExpression
                    val value =
                        if (literalExpression.getValue() is String) literalExpression.getValue() else null
                    if (value != null && value.startsWith(SIMPLE_PREFIX_STR + SIMPLE_SEPARATOR_STR)) {
                        val property = TextRange(
                            SIMPLE_PREFIX_STR.length() + SIMPLE_SEPARATOR_STR.length() + 1,
                            value.length + 1
                        )
                        return arrayOf<PsiReference>(SimpleReference(element, property))
                    }
                    return PsiReference.EMPTY_ARRAY
                }
     */
}