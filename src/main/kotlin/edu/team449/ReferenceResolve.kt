package edu.team449

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import edu.team449.RobotStuff.robotClass
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.impl.YAMLAliasImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

const val identifierRegexStr = """[A-Za-z_][A-Za-z_0-9]*"""
const val classNameRegexStr = """(${identifierRegexStr}\.)+$identifierRegexStr"""
const val robotPkgName = "org.usfirst.frc.team449.robot"
const val wpiPackage = "edu.wpi"
const val ROBOT_MAP_QUALIFIED_NAME = "$robotPkgName.RobotMap"
val classNameWithDotMaybeIncomplete =
  Regex("""($identifierRegexStr\.)+($identifierRegexStr)?""")//Regex("""[A-Za-z_][A-Za-z_0-9]*(\.[A-Za-z_][A-Za-z_0-9]*)*?(\.[A-Za-z_]?)?""")
val classNameRegex = Regex(classNameRegexStr)

object RobotStuff {
  private var robotClasses: MutableMap<Project, PsiClass> = mutableMapOf()
  private var robotCtors: MutableMap<Project, PsiMethod> = mutableMapOf()

  fun robotClass(project: Project): PsiClass? =
    if (project in robotClasses) robotClasses[project]
    else resolveToClass(ROBOT_MAP_QUALIFIED_NAME, project)?.let { robotClasses.put(project, it) }

  fun robotConstructor(project: Project): PsiMethod? =
    if (project in robotCtors) robotCtors[project]
    else robotClass(project)?.let(::findConstructor)?.let { robotCtors.put(project, it) }
}

/**
 * Resolve reference to a single element
 */
fun resolveRef(element: PsiElement): PsiElement? =
  when (element) {
    is YAMLPlainTextImpl -> resolveToIdDecl(element)
    is YAMLKeyValue -> resolveToClass(element) ?: resolveToParameter(element)
    else -> element
  }

fun resolveToIdDecl(idRef: YAMLPlainTextImpl): YAMLKeyValue? =
  YamlAnnotator.findById(idRef.text, idRef.project)

/**
 * Resolves a plaintext value to an object of that id, and also gives
 * whether or not it is a forward reference
 *
 * @return The object/constructor call this refers to (null if not found)
 *         and a boolean (true if it is a forward reference, false otherwise)
 */
//fun resolveToObjectDef(idArg: YAMLPlainTextImpl): Pair<YAMLKeyValue?, Boolean> {
//  val file = idArg.containingFile
//  var forwardRef = false
//  return findElementLinearly(file) { elem ->
//    if (elem == idArg) forwardRef = true
//
//    elem is YAMLKeyValue && getIdValue(elem)?.let { it == idArg.text } ?: false
//  } as YAMLKeyValue? to forwardRef
//}

fun getIdValue(constructorCall: YAMLKeyValue): String? {
  val cls = resolveToClass(constructorCall) ?: return null
  val idStr = getIdName(cls) ?: return null
  return getAllArgs(constructorCall).find { (name, _) -> removeQuotes(name) == idStr }?.first
}

/*fun getIdArg(args: Iterable<YAMLKeyValue>, constructorCall: YAMLKeyValue): YAMLKeyValue? {
    return findIdArg(args, resolveToClass(constructorCall) ?: return null)
}*/

fun findIdArg(args: Iterable<YAMLKeyValue>, cls: PsiClass): YAMLKeyValue? {
  return args.find { keyVal -> removeQuotes(keyVal.keyText) == getIdName(cls) ?: return null }
}

fun isIdArg(arg: YAMLKeyValue, idName: String): Boolean =
  removeQuotes(arg.keyText) == idName

/**
 * Find the class whose constructor/JsonCreator is being called
 */
fun resolveToClass(constructorCall: YAMLKeyValue): PsiClass? =
  getRealKeyText(constructorCall)?.let { resolveToClass(it, constructorCall.project) }

/**
 * If the input is an alias, it keeps trying to find the
 * real value referred to by it. If not, it just gives it back.
 */
fun getRealValue(element: PsiElement): PsiElement? =
  if (element is YAMLAliasImpl) anchorValue(element)?.let(::getRealValue)
  else element

fun getRealKeyText(element: YAMLKeyValue): String? =
  when (val key = getRealValue(element.key!!)) {
    is YAMLKeyValue -> key.keyText
    else -> key?.text
  }

fun resolveToParameter(arg: YAMLKeyValue): PsiParameter? {
  val clazz =
    if (isTopLevel(arg))
      robotClass(arg.project)
    else
      when (val seq = arg.parent?.parent?.parent) {
        is YAMLSequence ->
          typeOfItems(seq)?.let(::psiTypeToClass)
        else ->
          getUpperConstructorCall(arg)?.let(::classOf)
      }
  return clazz?.let { resolveToParameter(arg, it) }
}

fun getTheClassWhoseCtorThisIsAnArgTo(arg: YAMLKeyValue): PsiClass? {
  return if (isTopLevel(arg))
    robotClass(arg.project)
  else
    when (val seq = arg.parent?.parent?.parent) {
      is YAMLSequence ->
        typeOfItems(seq)?.let(::psiTypeToClass)
      else ->
        getUpperConstructorCall(arg)?.let(::classOf)
    }
}

/**
 * Whether or not a parameter exists. Works with WPI classes.
 * @param arg The parameter name-argument pair in YAML
 * @param clazz The Java class to which this is an argument
 */
fun existsParameter(arg: YAMLKeyValue, clazz: PsiClass): Boolean =
  //Make sure it's not an alias, resolve to the real value
  getRealKeyText(arg)?.let { argName ->
    if (clazz.qualifiedName !in wpiCtors)
      findConstructor(clazz)?.let { findParam(it, argName) } != null
    else
      wpiCtors[clazz.qualifiedName]?.let {
        it.keys.any { paramName -> stripQMark(paramName) == argName }
      }
  } ?: false

/**
 * Resolve to a parameter in a Java method. Does not work with WPI classes
 */
fun resolveToParameter(arg: YAMLKeyValue, clazz: PsiClass): PsiParameter? =
  //Make sure it's not an alias, resolve to the real value
  getRealKeyText(arg)?.let { argName ->
    findConstructor(clazz)?.let { findParam(it, argName) }
  }

fun typeOfItems(seq: YAMLSequence): PsiType? =
  when (val arg = seq.parent) {
    is YAMLKeyValue -> {
      val paramType = typeOf(arg)
      if (paramType is PsiClassType && paramType.className == "List")
        paramType.parameters[0]
      else {
//        LOG.error("keyvalue failed ${arg.keyText}")
//        TODO()
        null
      }
    }
    else -> {
//      LOG.error("Not a keyvalue ${arg.text}")
//      TODO()
      null
    }
  }

/**
 * Find a constructor in the given class usable by
 * YAML files (whether they're actual constructors or methods
 * annotated with `JsonCreator`)
 */
fun findConstructor(cls: PsiClass): PsiMethod? =
  cls.methods.find(
    if (needsJsonAnnot(cls)) { method -> hasAnnotation(method, "JsonCreator") }
    else { method -> method.isConstructor })

/**
 * Return the Java class that the key constructs
 * If the class is not explicitly stated, uses the
 * type of the parameter of the parent with the same
 * name
 */
fun classOf(keyValue: YAMLKeyValue): PsiClass? =
  if (isQualifiedConstructorCall(keyValue)) {
    resolveToClass(keyValue)
  } else {
    typeOf(keyValue)?.let(::psiTypeToClass)
  }

/**
 * Return the Java class that the key constructs
 * If the class is not explicitly stated, uses the
 * type of the parameter of the parent with the same
 * name
 */
fun typeOf(keyValue: YAMLKeyValue): PsiType? =
  if (isQualifiedConstructorCall(keyValue))
    resolveToClass(keyValue)?.let(::psiClassToType)
  else
    when (val greatGrandParent = keyValue.parent?.parent?.parent) {
      is YAMLSequence ->
        typeOfItems(greatGrandParent)?.let { outer ->
          psiTypeToClass(outer)?.let {
            resolveToParameter(
              keyValue,
              it
            )?.type
          }
        }
      else -> resolveToParameter(keyValue)?.type
    }

/**
 * TODO work on this
 * Whether or not the argument in the YAML file matches
 * the parameter's type
 */
//fun matchesType(arg: YAMLKeyValue, param: PsiParameter) = matchesType(arg, param.type.canonicalText)

/**
 * Whether or not the argument in a YAML file is of the given type
 */
//fun matchesType(arg: YAMLKeyValue, className: String): Boolean {
//  if (isCollectionClass(className)) {
//    val list = arg.value
//    //This means it's a list
//    if (list is YAMLBlockSequenceImpl && hasSingleTypeParameter(className)) {
//      val innerType = extractTypeArgument(className)
//      for (item in list.items) {
//        if (item.value != null) {
//          val value =
//            anchorIfAliasNullIfWrongTypeElseSame<YAMLKeyValue>(item.value!!)
//          if (value == null || !matchesType(value, innerType)) return false
//        }
//      }
//      return true
//    } else return false //Because it should be a list
//  } else {
//    return classOf(arg)?.let {
//      it.qualifiedName == className.replaceAfter('<', "")
//    } ?: false
//  }
//}

/**
 * Helpful method to find a class by name
 */
fun resolveToClass(className: String, project: Project): PsiClass? =
  JavaPsiFacade.getInstance(project).findClass(
    className,
    GlobalSearchScope.allScope(project)
  )

/**
 * Get the class whose constructor this is an argument to.
 */
fun getUpperClass(arg: YAMLKeyValue): PsiClass? {
  val upperCtor = getUpperConstructorCall(arg)
  return if (upperCtor == null) {
    robotClass(arg.project)
  } else {
    classOf(upperCtor)
  }
}

/**
 * Get the name of the property representing the
 * id for this class if there's a JsonIdentityInfo annotation.
 * @return `null` if there's no `JsonIdentityInfo` annotation,
 *         '@id' if the name hasn't been set or if it's a WPI
 *         class (identified by package name), and the custom id otherwise.
 */
fun getIdName(cls: PsiClass): String? =
  cls.annotations.find { annot ->
    annot.qualifiedName?.endsWith("JsonIdentityInfo") ?: false
  }?.let { idAnnot ->
    idAnnot.findAttributeValue("property")?.text?.let { removeQuotes(it) } ?: DEFAULT_ID
  } ?: (if (cls.qualifiedName?.startsWith(wpiPackage) == true) DEFAULT_ID else null)

/**
 * Find a package by name
 */
fun resolveToPackage(pkgName: String, project: Project): PsiPackage? =
  JavaPsiFacade.getInstance(project).findPackage(pkgName)