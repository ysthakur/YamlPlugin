package edu.team449.lang

import com.intellij.psi.PsiMethod
import org.jetbrains.yaml.YAMLSyntaxHighlighter
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.lexer.YAMLFlexLexer
import org.jetbrains.yaml.psi.YAMLAnchor
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLValue
import org.jetbrains.yaml.psi.impl.YAMLAliasImpl
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import org.jetbrains.yaml.resolve.YAMLAliasReference
import java.util.*

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

/**
 * Get all the arguments that this constructor has, including ones
 * copied using aliases/anchors.
 * Does not guarantee that all the `YAMLKeyValue`'s will have
 * `constructorCall` as their parent
 */
fun getAllArgs(constructorCall: YAMLKeyValue): Iterable<YAMLKeyValue> {
    val value = constructorCall.value
    val default = {emptyList<YAMLKeyValue>()}
    when (value) {
        is YAMLMapping -> {
            val res = LinkedList<YAMLKeyValue>()
            //todo convert back to flatmap
            for (keyVal in value.keyValues) {
                val text = keyVal.keyText
                if (text == "<<") {
                    val ref = YAMLAliasReference(keyVal.value as YAMLAliasImpl)
                    val other = ref.resolve()?.markedValue?.parent as YAMLKeyValue ?: return emptyList()
                    res.addAll(getAllArgs(other))
                } else {
                    val ind = res.indexOfFirst { kv -> kv.keyText == text }
                    if (ind == -1) res.add(keyVal)
                    else res[ind] = keyVal
                }
            }
            return res
        }
        is YAMLPlainTextImpl -> {
            return getAllArgs(resolveToObjectDef(value) ?: return default())
        }
        else -> return default()
    }
}