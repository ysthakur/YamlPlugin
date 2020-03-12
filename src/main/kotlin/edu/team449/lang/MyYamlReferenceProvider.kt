package edu.team449.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

/**
 * Returns references
 * TODO make a reference type for objects referred to using the '@id' property
 */
object MyYamlReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        //todo implement this
        //Logger.getInstance(this::class.java).warn("Got to getReferencesByElement")
        return when (element) {
            is YAMLKeyValue -> {
                if (isConstructorCall(element)) {
                    arrayOf<PsiReference>(YamlJavaClassOrConstructorReference(element))
                } else {
                    arrayOf<PsiReference>(YamlJavaParameterReference(element))
                }
            }
            is YAMLPlainTextImpl -> {
                arrayOf<PsiReference>(IdReference(element))
            }
            else -> {
                emptyArray()
            }
        }

        //return emptyArray()
    }
}