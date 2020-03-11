package edu.team449.lang

import com.intellij.psi.PsiMethod
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * This is some random placeholder that someone at Jetbrains
 * put in
 */
val annoyingDummyCode = "IntellijIdeaRulezzz"

fun String.withoutQuotes() = this.removeSurrounding("\"")

fun PsiMethod.findAnnotation(annotName: String) =
    this.annotations.find { a -> (a.qualifiedName ?: "").afterDot() == annotName }

fun PsiMethod.findParam(paramName: String) = this.parameterList.parameters.find { p -> p.name == paramName }

fun hasAnnotation(method: PsiMethod, annotName: String) = method.findAnnotation(annotName) != null

fun isConstructorCall(constructorCall: YAMLKeyValue) =
    incompleteCodeFormatted(constructorCall.keyText).matches(classNameRegex)

fun incompleteCodeFormatted(text: String) = text.replace(annoyingDummyCode, "")

fun String.afterDot() = Regex("""\.[^.]*$""").find(this)?.value?.removePrefix(".") ?: this

fun getUpperConstructor(constructorCall: YAMLKeyValue): YAMLKeyValue {
    return constructorCall.parent.parent as YAMLKeyValue
}