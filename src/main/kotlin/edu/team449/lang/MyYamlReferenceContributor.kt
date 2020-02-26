package edu.team449.lang

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.psi.YAMLPsiElement

class MyYamlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        TODO("not implemented")
        /*registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLPsiElement::class.java),
            object : PsiReferenceProvider() {
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
            })*/
    }
}