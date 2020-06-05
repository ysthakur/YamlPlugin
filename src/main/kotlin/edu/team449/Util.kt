package edu.team449

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
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
val ANNOYING_AND_NON_USEFUL_DUMMY_CODE_BY_JETBRAINS = "IntellijIdeaRulezzz"
val DEFAULT_ID = "@id"
val LIST_CLASS_SIMPLE_NAME = "List"
val VALID_IDENTIFIER_REGEX_STR = """[A-Za-z_$][A-Za-z0-9_${'$'}]*"""
val TYPE_ACCEPTING_SINGLE_TYPE_PARAMETER_REGEX =
  Regex("$VALID_IDENTIFIER_REGEX_STR(\\W*\\.\\W*$VALID_IDENTIFIER_REGEX_STR)*\\W*<\\W*$VALID_IDENTIFIER_REGEX_STR\\W*>")

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
fun PsiClass.isCollectionClass() = this.name?.startsWith(LIST_CLASS_SIMPLE_NAME) ?: false

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
fun getAllArgs(constructorCall: YAMLKeyValue): List<YAMLKeyValue> {
  val value = constructorCall.value
  val default = { emptyList<YAMLKeyValue>() }
  val exit = { elem: YAMLPsiElement ->
    throw NoSuchParameterException(
      "(Unresolved)",
      elem
    )
  }
  return when (value) {
    is YAMLMapping -> {
      val args = mutableListOf<YAMLKeyValue>()
      val add = { kv: YAMLKeyValue ->
        val ind = args.indexOfFirst { kv2 -> kv2.keyText == kv.keyText }
        if (ind == -1) args.add(kv)
        else args[ind] = kv
      }
      for (keyVal in value.keyValues) {
        if (keyVal.keyText == "<<") {
          val ref = YAMLAliasReference(keyVal as YAMLAliasImpl)
          val other = ref.resolve()?.markedValue?.parent as YAMLKeyValue? ?: return default()
          val otherArgs = getAllArgs(other)
          for (kv2 in otherArgs) add(kv2)
        } else add(keyVal)
      }

      /*val params: MutableMap<ArgKey, YAMLValue> =
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
      }*/
      args
    }
    is YAMLPlainTextImpl -> {
      getAllArgs(resolveToObjectDef(value).first ?: return default())
    }
    else -> default()
  }
}

class NoSuchParameterException(paramName: String, val elem: PsiElement) :
  Exception("Could not find a parameter by the name of $paramName")