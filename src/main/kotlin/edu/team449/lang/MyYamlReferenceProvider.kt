package edu.team449.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Returns references
 * TODO make a reference type for objects referred to using the '@id' property
 */
object MyYamlReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        //todo implement this
        //Logger.getInstance(this::class.java).warn("Got to getReferencesByElement")
        when (element) {
            is YAMLKeyValue -> {
                if (isConstructorCall(element)) {
                    return arrayOf(YamlJavaClassOrConstructorReference(element))
                }
            }
            else -> {
                if (element.text.contains(classNameRegex))
                    Logger.getInstance(this.javaClass)
                        .warn("Could not find element of type $element with text ${element.text}")
            }
        }

        return emptyArray()
    }
}