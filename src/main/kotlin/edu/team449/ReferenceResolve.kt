package edu.team449.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.impl.YAMLAliasImpl
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import org.jetbrains.yaml.resolve.YAMLAliasReference
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

val classNameRegexStr = """([A-Za-z_][A-Za-z_0-9]*\.)+[A-Za-z_][A-Za-z_0-9]*"""
val classNameDotRegex = Regex("$classNameRegexStr\\.")
val classNameRegex = Regex(classNameRegexStr)
val robotPkgName = "org.usfirst.frc.team449.robot"
val ROBOT_MAP_QUALIFIED_NAME = "$robotPkgName.RobotMap"
//var ROBOT_CLASS_CACHED: PsiClass? = null
fun Project.robotClass() = resolveToClass(ROBOT_MAP_QUALIFIED_NAME, this)

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
        //val cls = elem.javaClass
        //val text = elem.text
        if (elem == idArg) forwardRef = true
        if (elem !is YAMLKeyValue || elem.value !is YAMLMappingImpl) {
            false
        } else {
            val otherId = getIdArg(elem) ?: return@findElementLinearly false
            //Logger.getInstance(MyYamlAnnotator::class.java).warn("Value is ${otherId.valueText}")
            otherId.valueText == idArg.text
        }
    } as YAMLKeyValue? to forwardRef
}

fun getIdArg(constructorCall: YAMLKeyValue): YAMLKeyValue? {
    val cls = resolveToClass(constructorCall) ?: return null
    val idStr = getIdName(cls) ?: return null
    return findArg(constructorCall, idStr)
}

fun findArg(constructorCall: YAMLKeyValue, argName: String) =
    (constructorCall.value as YAMLMapping).keyValues.find { keyVal -> keyVal.keyText.withoutQuotes() == argName }

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
    if (arg.key is YAMLAliasImpl)
        return resolveToParameter(
            (YAMLAliasReference(arg.key as YAMLAliasImpl).resolve()?.markedValue ?: return null) as YAMLKeyValue
        )
    val upperConst = getUpperConstructor(arg)
    val cls = if (upperConst == null) {
        arg.project.robotClass()
    } else {
        typeOf(upperConst)
    } ?: return null
    return resolveToConstructor(cls)?.findParam(arg.keyText)
}

/**
 * Find ALL the constructors in the given class usable by
 * YAML files (whether they're actual constructors or methods
 * annotated with `JsonCreator`)
 */
fun allYAMLConstructors(cls: PsiClass): List<PsiMethod> {
    val needsJsonAnnot = needsJsonAnnot(cls)
    val pred =
        { method: PsiMethod -> if (needsJsonAnnot) hasAnnotation(method, "JsonCreator") else method.isConstructor }
    return cls.methods.filter(pred)
}

/**
 * Find a constructor in the class by the name `className`, assuming there's
 * only one constructor used
 */
fun resolveToConstructor(cls: PsiClass): PsiMethod? {
    val cs = allYAMLConstructors(cls)
    return if (cs.isEmpty()) null else cs[0]
}

/**
 * Return the Java class that the key constructs
 * If the class is not explicitly stated, uses the
 * type of the parameter of the parent with the same
 * name
 */
fun typeOf(keyValue: YAMLKeyValue): PsiClass? {
    if (isConstructorCall(keyValue)) {
        return resolveToClass(keyValue)
    } else {
        val param = resolveToParameter(keyValue) ?: return null
        return PsiTypesUtil.getPsiClass(param.type)
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
fun getIdName(cls: PsiClass): String? {
    return if (!needsIdAnnotation(cls)) defaultId
           else (cls.annotations.find { annot ->
               annot.text.matches(Regex("""@JsonIdentityInfo\(.*""")) } ?: return null)
           .findAttributeValue("property")?.text?.withoutQuotes()
           ?: defaultId
}

/**
 * Find a package by name
 */
fun resolveToPackage(pkgName: String, project: Project): PsiPackage? =
    JavaPsiFacade.getInstance(project).findPackage(pkgName)