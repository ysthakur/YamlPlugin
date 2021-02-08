package edu.team449

import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.yaml.psi.*
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
val VALID_IDENTIFIER_REGEX = Regex(VALID_IDENTIFIER_REGEX_STR)
val TYPE_ACCEPTING_SINGLE_TYPE_PARAMETER_REGEX =
  Regex("$VALID_IDENTIFIER_REGEX_STR(\\W*\\.\\W*$VALID_IDENTIFIER_REGEX_STR)*\\W*<\\W*$VALID_IDENTIFIER_REGEX_STR\\W*>")
//TODO find an alternative to recursive regex
//val GENERIC_TYPE_REGEX = Regex("""[A-Za-z_][A-Za-z0-9_$]*(<(?R)(,(?R))*>)?""")

fun removeQuotes(s: String) = s.removeSurrounding("\"").removeSurrounding("'")

fun PsiMethod.findAnnotation(annotName: String) =
  this.annotations.find { a -> (a.qualifiedName ?: "").afterDot() == annotName }

fun PsiMethod.findParam(paramName: String) = this.parameterList.parameters.find { p -> p.name == paramName }

fun psiTypeToClass(type: PsiType): PsiClass? = PsiTypesUtil.getPsiClass(type)

fun psiClassToType(clazz: PsiClass): PsiType =
  JavaPsiFacade.getInstance(clazz.project).elementFactory.createType(clazz)

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
 * Whether or not an element could be a reference to some other object using that object's id
 */
fun couldBeIdReference(element: YAMLPlainTextImpl): Boolean {
  val text = element.text
  return element.firstChild !is YAMLAnchor && text.matches(VALID_IDENTIFIER_REGEX) && text != "true" && text != "false"
}

/**
 * TODO generalize this maybe?
 * Whether or not it's a list or other collection
 */
fun isCollectionClass(className: String) = className.contains(LIST_CLASS_SIMPLE_NAME)

fun hasAnnotation(method: PsiMethod, annotName: String) = method.findAnnotation(annotName) != null

/**
 * Whether or not it looks like this:
 * org.foo.blah:
 *  key: val
 */
fun isQualifiedConstructorCall(constructorCall: YAMLKeyValue): Boolean =
  getRealKeyText(constructorCall)?.let { incompleteCodeFormatted(it).matches(classNameRegex) } ?: false

fun incompleteCodeFormatted(text: String) = text.replace(ANNOYING_AND_NON_USEFUL_DUMMY_CODE_BY_JETBRAINS, "")

fun String.afterDot() = Regex("""\.[^.]*$""").find(this)?.value?.removePrefix(".") ?: this

/**
 * Whether or not the constructor that Jackson calls needs
 * a JsonCreator annotation right there.
 * Currently just checks if it's from WPI
 * TODO this is a terrible solution, fix this
 */
fun needsJsonAnnot(cls: PsiClass): Boolean =
  !(cls.qualifiedName?.startsWith("edu.wpi") ?: true)

/**
 * TODO figure out why I made this in the first place
 * Whether or not it needs a `JsonIdentityInfo` annotation
 * to set an id property (Default id is "`@id`")
 */
fun needsIdAnnotation(cls: PsiClass): Boolean =
  if (cls.qualifiedName!!.startsWith(wpiPackage)) false
  else getIdName(cls) == null

/**
 * TODO Don't just check if it's a WPI class
 * A pair where the first element is the name of the id for the given class
 * and the second parameter tells you whether or not the id is required
 */
fun getAndNeedsId(cls: PsiClass): Pair<String?, Boolean> =
  getIdName(cls)?.let { id ->
    Pair(id, !(cls.qualifiedName?.startsWith(wpiPackage) ?: true))
  } ?: Pair(null, false)


//TODO work on this
fun isTopLevel(keyVal: YAMLKeyValue): Boolean = keyVal.parent?.parent is YAMLDocument

/**
 * Get the constructor call that this is an argument to, but if
 * there isn't one, returns `null`
 */
fun getUpperConstructorCall(ctorCall: YAMLKeyValue): YAMLKeyValue? = when (val upperCtor = ctorCall.parent?.parent) {
  is YAMLKeyValue -> upperCtor
  else -> null
}

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
inline fun <reified T : PsiElement> anchorIfAliasNullIfWrongTypeElseSame(value: PsiElement): T? =
  when (value) {
    is YAMLAliasImpl -> givenTypeOrNull<T>(anchorValue(value))
    is T -> value
    else -> null
  }

/**
 * Gets the value referred to by the alias, if there is one
 */
fun anchorValue(alias: YAMLAliasImpl): YAMLPsiElement? {
  val anchor = YAMLAliasReference(alias).resolve()
  return anchor?.markedValue ?: (anchor?.parent as? YAMLKeyValue?)
}

/**
 * Get all the arguments that this constructor has, including ones
 * copied using aliases/anchors.
 * Does not guarantee that all the `YAMLKeyValue`'s will have
 * `constructorCall` as their parent
 */
fun getAllArgs(constructorCall: YAMLKeyValue): List<Pair<String, YAMLKeyValue>> {
  val value = constructorCall.value
  val default = listOf<Pair<String, YAMLKeyValue>>()
  return when (value) {
    is YAMLMapping -> {
      val keyValues = value.keyValues
      val arg1 = keyValues.firstOrNull()
      val (realKeyVals, inherited) =
        if (arg1?.keyText == "<<") {
          when (val alias = arg1.value) {
            is YAMLAliasImpl ->
              Pair(keyValues.drop(1), getAllArgs(anchorValue(alias)!!.parent as YAMLKeyValue))
            else -> TODO("Element at textRange ${alias?.textRange} is not an alias")
          }
        } else {
          Pair(keyValues, default)
        }
      val args = realKeyVals.map { keyVal ->
        when (val key = keyVal.key) {
          is YAMLAliasImpl -> Pair(anchorValue(key)!!.lastChild.text, keyVal)
          else -> Pair(key!!.text, keyVal)
        }
      }
      args + inherited.filter { (name, _) -> !args.any { it.first == name } }
    }
    //todo check if this is ever even needed, since it's a stupid thing to do.
    is YAMLPlainTextImpl -> YamlAnnotator.findById(value.text, value.project)?.let(::getAllArgs) ?: default
    is YAMLSequence -> default //TODO implement sequences
    else -> default
  }
}