package edu.team449

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLAliasImpl
import org.jetbrains.yaml.psi.impl.YAMLBlockSequenceImpl
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

val classNameRegexStr = """([A-Za-z_][A-Za-z_0-9]*\.)+[A-Za-z_][A-Za-z_0-9]*"""
val classNameMaybeDot = Regex("""[A-Za-z_][A-Za-z_0-9]*(\.[A-Za-z_][A-Za-z_0-9]*)*?(\.[A-Za-z_]?)?""")
val classNameDotRegex = Regex("$classNameRegexStr\\.")
val classNameRegex = Regex(classNameRegexStr)
val robotPkgName = "org.usfirst.frc.team449.robot"
val ROBOT_MAP_QUALIFIED_NAME = "$robotPkgName.RobotMap"

val LOG = Logger.getInstance(object {}.javaClass)

//var ROBOT_CLASS_CACHED: PsiClass? = null

fun Project.robotClass() =
  resolveToClass(ROBOT_MAP_QUALIFIED_NAME, this)

/**
 * Resolve reference to a single element
 */
fun resolveRef(element: PsiElement): PsiElement? =
  when (element) {
    is YAMLPlainTextImpl -> resolveToIdDecl(element) ?: resolveToObjectDef(element).first?.let(::resolveRef)
    is YAMLKeyValue -> resolveToClass(element) ?: resolveToParameter(element)
    else -> element
  }

fun resolveToIdDecl(idRef: YAMLPlainTextImpl): YAMLKeyValue? {
  val file = idRef.containingFile
  return findElementLinearly(file) { elem ->
    //val cls = elem.javaClass
    //val text = elem.text
    if (elem.parent !is YAMLKeyValue || elem !is YAMLMappingImpl || elem !is YAMLKeyValue) {
      false
    } else {
      //Logger.getInstance(MyYamlAnnotator::class.java).warn("Value is ${elem.valueText}")
      elem.valueText == idRef.text
    }
  } as YAMLKeyValue?
}

/**
 * Resolves a plaintext value to an object of that id, and also gives
 * whether or not it is a forward reference
 *
 * @return The object/constructor call this refers to (null if not found)
 *         and a boolean (true if it is a forward reference, false otherwise)
 */
fun resolveToObjectDef(idArg: YAMLPlainTextImpl): Pair<YAMLKeyValue?, Boolean> {
  val file = idArg.containingFile
  var forwardRef = false
  return findElementLinearly(file) { elem ->
    if (elem == idArg) forwardRef = true

    elem is YAMLKeyValue && getIdArg(elem)?.let { it.valueText == idArg.text } ?: false
  } as YAMLKeyValue? to forwardRef
}

fun getIdArg(constructorCall: YAMLKeyValue): YAMLKeyValue? {
  val cls = resolveToClass(constructorCall) ?: return null
  val idStr = getIdName(cls) ?: return null
  return findArg(constructorCall, idStr)
}

/*fun getIdArg(args: Iterable<YAMLKeyValue>, constructorCall: YAMLKeyValue): YAMLKeyValue? {
    return findIdArg(args, resolveToClass(constructorCall) ?: return null)
}*/

fun findIdArg(args: Iterable<YAMLKeyValue>, cls: PsiClass): YAMLKeyValue? {
  return args.find { keyVal -> removeQuotes(keyVal.keyText) == getIdName(cls) ?: return null }
}

fun isIdArg(arg: YAMLKeyValue, idName: String): Boolean =
  removeQuotes(arg.keyText) == idName

fun findArg(constructorCall: YAMLKeyValue, argName: String) =
  getAllArgs(constructorCall).find { keyVal -> removeQuotes(keyVal.keyText) == argName }

/**
 * Find a child PSI element inside this element recursively
 * using the predicate given
 * @param parent The parent that may or may not contain a child matching
 * pred
 * @param pred the predicate used to determine if the element has
 * been found
 */
fun findElementLinearly(parent: PsiElement, pred: (PsiElement) -> Boolean): PsiElement? {
  for (child in parent.children) {
    return if (pred(child)) child
    else findElementLinearly(child, pred) ?: continue
  }
  return null
}

/**
 * This apparently doesn't work because it's in a different thread
 */
fun findElement(parent: PsiElement, pred: (PsiElement) -> Boolean): PsiElement? {
  val childThreads = AtomicReference<MutableSet<Thread>>(mutableSetOf())
  val mainRes = AtomicReference<PsiElement?>()
  for (child in parent.children) {
    if (mainRes.get() != null) break
    if (pred(child)) return child
    else {
      val thr = thread {
        val res = findElement(child, pred) ?: return@thread
        mainRes.set(res)
        val me = Thread.currentThread()
        for (t in childThreads.acquire) {
          if (t != me) t.interrupt()
        }
      }
      childThreads.get().add(thr)
    }
  }
  return mainRes.get()
}

/**
 * Find the class whose constructor/JsonCreator is being called
 */
fun resolveToClass(constructorCall: YAMLKeyValue): PsiClass? =
  resolveToClass(constructorCall.keyText, constructorCall.project)

fun resolveToParameter(arg: YAMLKeyValue): PsiParameter? {
  if (arg.key is YAMLAliasImpl) {
    val anchoredValue = anchorIfAliasNullIfWrongTypeElseSame<YAMLKeyValue>(arg.key as YAMLAliasImpl)
    anchoredValue?.let {
      val param = resolveToParameter(anchoredValue)
      param?.let { return@resolveToParameter param }
      LOG.debug("${arg.key} cannot be resolved to a parameter")
    }
    if (anchoredValue == null) LOG.debug("anchoredValue is null?")
  }
  val upperConst = getUpperConstructor(arg)
  val cls = if (upperConst == null) {
    arg.project.robotClass()
  } else {
    typeOf(upperConst)
  }
  return cls?.let { findConstructor(it)?.findParam(arg.keyText) }
}

/**
 * Find ALL the constructors in the given class usable by
 * YAML files (whether they're actual constructors or methods
 * annotated with `JsonCreator`)
 */
fun allYAMLConstructors(cls: PsiClass): List<PsiMethod> {
  val needsJsonAnnot = needsJsonAnnot(cls)
  return cls.methods.filter { method ->
    if (needsJsonAnnot) hasAnnotation(
      method,
      "JsonCreator"
    ) else method.isConstructor
  }
}

/**
 * Find a constructor in the class by the name `className`, assuming there's
 * only one constructor used
 */
fun findConstructor(cls: PsiClass): PsiMethod? = allYAMLConstructors(cls).firstOrNull()

/**
 * Return the Java class that the key constructs
 * If the class is not explicitly stated, uses the
 * type of the parameter of the parent with the same
 * name
 */
fun typeOf(keyValue: YAMLKeyValue): PsiClass? =
  if (isConstructorCall(keyValue))
    resolveToClass(keyValue)
  else
    resolveToParameter(keyValue)?.let { PsiTypesUtil.getPsiClass(it.type) }

/**
 * Whether or not the argument in the YAML file matches
 * the parameter's type
 */
fun matchesType(arg: YAMLKeyValue, param: PsiParameter) = matchesType(arg, param.type.canonicalText)

/**
 * Whether or not the argument in a YAML file is of the given type
 */
fun matchesType(arg: YAMLKeyValue, className: String): Boolean {
  if (isCollectionClass(className)) {
    val list = arg.value
    //This means it's a list
    if (list is YAMLBlockSequenceImpl && hasSingleTypeParameter(className)) {
      val innerType = extractTypeArgument(className)
      for (item in list.items) {
        val value =
          anchorIfAliasNullIfWrongTypeElseSame<YAMLKeyValue>(item.value ?: return false) ?: return false
        if (!matchesType(value, innerType)) return false
      }
      return true
    } else return false //Because it should be a list
  } else {
    return typeOf(arg)?.let {
      it.qualifiedName == className.replaceAfter('<', "")
    } ?: false
  }
}

/**
 * Helpful method to find a class by name
 */
fun resolveToClass(className: String, project: Project): PsiClass? =
  JavaPsiFacade.getInstance(project).findClass(
    className,
    GlobalSearchScope.allScope(project)
  )

/**
 * Get the name of the property representing the
 * id for this class ("@id" by default) if there
 * is a JsonIdentityInfo annotation
 */
fun getIdName(cls: PsiClass): String? =
  cls.annotations.find { annot ->
    annot.qualifiedName?.endsWith("JsonIdentityInfo") ?: false
  }?.let { idAnnot ->
    idAnnot.findAttributeValue("property")?.text?.let { removeQuotes(it) } ?: DEFAULT_ID
  }

/**
 * Find a package by name
 */
fun resolveToPackage(pkgName: String, project: Project): PsiPackage? =
  JavaPsiFacade.getInstance(project).findPackage(pkgName)