package edu.team449

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLValue
import org.jetbrains.yaml.psi.impl.YAMLAliasImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import org.jetbrains.yaml.resolve.YAMLAliasReference

/**
 * This is some random placeholder that someone at Jetbrains put in
 */
const val ANNOYING_AND_NON_USEFUL_DUMMY_CODE_BY_JETBRAINS = "IntellijIdeaRulezzz"
const val DEFAULT_ID = "@id"
const val LIST_CLASS_SIMPLE_NAME = "List"
const val VALID_IDENTIFIER_REGEX_STR = """[A-Za-z_$][A-Za-z0-9_$]*"""
val TYPE_ACCEPTING_SINGLE_TYPE_PARAMETER_REGEX =
  Regex("$VALID_IDENTIFIER_REGEX_STR(\\W*\\.\\W*$VALID_IDENTIFIER_REGEX_STR)*\\W*<\\W*$VALID_IDENTIFIER_REGEX_STR\\W*>")

//typealias ArgKey = Pair<String, YAMLPsiElement>
//typealias ArgumentList = Map<ArgKey, YAMLValue>

//val ArgKey.argName
//  get() = this.first
//val ArgKey.elem
//  get() = this.second

fun removeQuotes(s: String) = s.removeSurrounding("\"")

fun PsiMethod.findAnnotation(annotName: String) =
  this.annotations.find { a -> (a.qualifiedName ?: "").afterDot() == annotName }

fun PsiMethod.findParam(paramName: String) = this.parameterList.parameters.find { p -> p.name == paramName }

/**
 * Whether or not this type accepts only one type parameter. `typeText` is what
 * someone typed in as the type of a parameter in some Java file
 */
fun hasSingleTypeParameter(typeText: String) = typeText.matches(TYPE_ACCEPTING_SINGLE_TYPE_PARAMETER_REGEX)

/**
 * Get the type argument to this type.
 * Assumes the class involved only has one type parameter
 * @see hasSingleTypeParameter
 */
fun extractTypeArgument(typeText: String) = typeText.substringAfter('<').removeSuffix(">")

/**
 * Whether or not it's a list
 */
//fun PsiClass.isCollectionClass(): Boolean =
//  TODO("Figure out how collections work")
//this.name?.startsWith(LIST_CLASS_SIMPLE_NAME) ?: false

/**
 * TODO generalize this maybe?
 * Whether or not it's a list or other collection
 */
fun isCollectionClass(className: String) = className.contains(LIST_CLASS_SIMPLE_NAME)

fun hasAnnotation(method: PsiMethod, annotName: String) = method.findAnnotation(annotName) != null

fun isConstructorCall(constructorCall: YAMLKeyValue) =
  incompleteCodeFormatted(constructorCall.keyText).matches(classNameRegex)

fun incompleteCodeFormatted(text: String) = text.replace(ANNOYING_AND_NON_USEFUL_DUMMY_CODE_BY_JETBRAINS, "")

fun String.afterDot() = Regex("""\.[^.]*$""").find(this)?.value?.removePrefix(".") ?: this

/**
 * Whether or not the constructor that Jackson calls needs
 * a JsonCreator annotation right there.
 * Currently just checks if it's from WPI
 * TODO this is a terrible solution, fix this
 */
fun needsJsonAnnot(cls: PsiClass): Boolean =
  !cls.qualifiedName!!.startsWith("edu.wpi")

/**
 * TODO figure out why I made this in the first place
 * Whether or not it needs a `JsonIdentityInfo` annotation
 * to set an id property (Default id is "`@id`")
 */
fun needsIdAnnotation(cls: PsiClass): Boolean =
  if (cls.qualifiedName!!.startsWith("edu.wpi")) false
  else getIdName(cls) == null

fun getUpperConstructor(constructorCall: YAMLKeyValue): YAMLKeyValue? =
  givenTypeOrNull<YAMLKeyValue>(constructorCall.parent.parent)

/**
 * Whether or not this parameter/property is required.
 * Does this by looking for `required=true` in its
 * `JsonProperty` annotation
 */
fun isRequiredParam(param: PsiParameter): Boolean =
  param.annotations.any {
    it.text.contains(Regex("""@JsonProperty\(.*required( )?=( )?true"""))
  }

/**
 * If `a` is of type `T`, return `a`, otherwise return `null`
 */
inline fun <reified T> givenTypeOrNull(a: Any?): T? =
  if (a is T) a else null

/**
 * If value is of type `T`, returns the value itself. If value is an alias
 * to something of type T, returns that anchor's marked value. If value is
 * not of type T, returns null
 *
 * @param value the value that may or may not be an alias
 * @param T the desired type of the result
 */
inline fun <reified T : YAMLPsiElement> anchorIfAliasNullIfWrongTypeElseSame(value: YAMLPsiElement): T? =
  when (value) {
    is YAMLAliasImpl -> givenTypeOrNull<T>(YAMLAliasReference(value).resolve()?.markedValue)
    is T -> value
    else -> null
  }

/**
 * Get all the arguments that this constructor has, including ones
 * copied using aliases/anchors.
 * Does not guarantee that all the `YAMLKeyValue`'s will have
 * `constructorCall` as their parent
 */
fun getAllArgs(constructorCall: YAMLKeyValue): List<YAMLKeyValue> {
  val value = constructorCall.value
  val default = emptyList<YAMLKeyValue>()
  return when (value) {
    is YAMLMapping -> {
      val args = mutableListOf<YAMLKeyValue>()
      fun add(kv: YAMLKeyValue) {
        val ind = args.indexOfFirst { it.keyText == kv.keyText }
        if (ind == -1) args.add(kv)
        else args[ind] = kv
      }
      for (keyVal in value.keyValues) {
        if (keyVal.keyText == "<<") {
          if (keyVal is YAMLAliasImpl) {
            Logger.getInstance(object {}.javaClass).debug("Yes! $keyVal is YAMLAlias!")
            val ref = YAMLAliasReference(keyVal)
            val other = ref.resolve()?.markedValue?.parent
            if (other is YAMLKeyValue) {
              val otherArgs = getAllArgs(other)
              for (kv2 in otherArgs) add(kv2)
            } else return default
          } else {
            Logger.getInstance(object {}.javaClass).debug("No!!! $keyVal is not an alias!")
          }
        } else add(keyVal)
      }

      args
    }
    is YAMLPlainTextImpl -> resolveToObjectDef(value).first?.let(::getAllArgs) ?: default
    else -> default
  }
}