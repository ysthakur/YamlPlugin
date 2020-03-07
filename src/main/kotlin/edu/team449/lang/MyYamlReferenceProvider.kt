package edu.team449.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Returns references
 * TODO make a reference type for objects referred to using the '@id' property
 */
object MyYamlReferenceProvider : PsiReferenceProvider() {
    val classNameRegexStr = """([A-Za-z_][A-Za-z_0-9]*\.)+[A-Za-z_][A-Za-z_0-9]*"""
    val classNameDotRegex = Regex("$classNameRegexStr\\.")
    val classNameRegex = Regex(classNameRegexStr)
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        //todo implement this
        //Logger.getInstance(this::class.java).warn("Got to getReferencesByElement")
        when (element) {
            is YAMLKeyValue -> {
                if (element.key?.text?.matches(classNameRegex) == true) {
                    return arrayOf(YamlJavaClassReference(element))
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