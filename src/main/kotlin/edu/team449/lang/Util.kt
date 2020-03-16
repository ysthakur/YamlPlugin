package edu.team449.lang

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLValue
import org.jetbrains.yaml.psi.impl.YAMLAliasImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import org.jetbrains.yaml.resolve.YAMLAliasReference

/**
 * This is some random placeholder that someone at Jetbrains
 * put in
 */
val annoyingDummyCode = "IntellijIdeaRulezzz"
val defaultId = "@id"

typealias ArgKey = Pair<String, YAMLPsiElement>
typealias ArgumentList = Map<ArgKey, YAMLValue>

val ArgKey.argName
    get() = this.first
val ArgKey.elem
    get() = this.second

fun String.withoutQuotes() = this.removeSurrounding("\"")

fun PsiMethod.findAnnotation(annotName: String) =
    this.annotations.find { a -> (a.qualifiedName ?: "").afterDot() == annotName }

fun PsiMethod.findParam(paramName: String) = this.parameterList.parameters.find { p -> p.name == paramName }

fun hasAnnotation(method: PsiMethod, annotName: String) = method.findAnnotation(annotName) != null

fun isConstructorCall(constructorCall: YAMLKeyValue) =
    incompleteCodeFormatted(constructorCall.keyText).matches(classNameRegex)

fun incompleteCodeFormatted(text: String) = text.replace(annoyingDummyCode, "")

fun String.afterDot() = Regex("""\.[^.]*$""").find(this)?.value?.removePrefix(".") ?: this

/**
 * Whether or not the constructor that Jackson calls needs
 * a JsonCreator annotation right there.
 * Currently just checks if it's from WPI
 * TODO this is a terrible solution, fix this
 */
fun needsJsonAnnot(cls: PsiClass): Boolean {
    val name = cls.qualifiedName!!
    return !name.startsWith("edu.wpi")
}

/**
 * Whether or not it needs a `JsonIdentityInfo` annotation
 * to set an id property (Default id is "`@id`")
 */
fun needsIdAnnotation(cls: PsiClass): Boolean {
    return !cls.qualifiedName!!.startsWith("edu.wpi")
}

fun getUpperConstructor(constructorCall: YAMLKeyValue) =
    try {
        constructorCall.parent.parent as YAMLKeyValue
    } catch (e: ClassCastException) {
        null
    }

/**
 * If value is of type `T`, returns the value itself. If value is an alias
 * to something of type T, returns that anchor's marked value. If value is
 * not of type T, returns null
 *
 * @param value the value that may or may not be an alias
 * @param T the desired type of the result
 */
inline fun <reified T : YAMLPsiElement> anchorIfAliasNullIfWrongTypeElseSame(value: YAMLPsiElement): T? = when (value) {
    is YAMLAliasImpl ->
        try {
            YAMLAliasReference(value).resolve()?.markedValue as T?
        } catch (cce: ClassCastException) {
            null
        }
    is T -> value
    else -> null
}

/**
 * Get all the arguments that this constructor has, including ones
 * copied using aliases/anchors.
 * Does not guarantee that all the `YAMLKeyValue`'s will have
 * `constructorCall` as their parent
 */
fun getAllArgs(constructorCall: YAMLKeyValue): ArgumentList {
    val value = constructorCall.value
    val default = { emptyMap<ArgKey, YAMLValue>() }
    val exit = { elem: YAMLPsiElement -> throw NoSuchParameterException("(Unresolved)", elem) }
    return when (value) {
        is YAMLMapping -> {
            //val res = LinkedList<YAMLKeyValue>()
            val params: MutableMap<ArgKey, YAMLValue> =
                value.keyValues.map<YAMLKeyValue, Pair<ArgKey, YAMLValue>> { keyVal ->
                    (Pair(
                        (anchorIfAliasNullIfWrongTypeElseSame<YAMLValue>(
                            keyVal.key as YAMLPsiElement? ?: exit(keyVal)
                        )?.text ?: exit(keyVal.key as YAMLPsiElement)), (keyVal.key ?: exit(keyVal)) as YAMLPsiElement
                    ) to keyVal.value!!)
                }.toMap(mutableMapOf())
            for (key in params.keys) {
                if (key.argName == "<<") {
                    val ref = YAMLAliasReference(params[key] as YAMLAliasImpl)
                    val other = ref.resolve()?.markedValue?.parent as YAMLKeyValue? ?: return default()
                    val otherArgs = getAllArgs(other)
                    for (k2 in otherArgs.keys) {
                        if (params[k2] == null) {
                            params[k2] = otherArgs[k2]!!
                        }
                    }
                }
            }
            /*for (keyVal in value.keyValues) {
                val text = keyVal.keyText
                if (text == "<<") {
                    val ref = YAMLAliasReference(keyVal.value as YAMLAliasImpl)
                    val other = ref.resolve()?.markedValue?.parent as YAMLKeyValue? ?: return emptyList()
                    res.addAll(getAllArgs(other))
                } else {
                    val ind = res.indexOfFirst { kv -> kv.keyText == text }
                    if (ind == -1) res.add(keyVal)
                    else res[ind] = keyVal
                }
            }*/
            params
        }
        is YAMLPlainTextImpl -> {
            getAllArgs(resolveToObjectDef(value).first ?: return default())
        }
        else -> default()
    }
}