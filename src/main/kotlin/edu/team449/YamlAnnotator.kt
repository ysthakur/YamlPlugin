package edu.team449

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import java.util.concurrent.ConcurrentHashMap

//todo check that all the arguments' types are correct
class YamlAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    //Remove all pointers to deleted elements
    elementsToAnnotate.keys.removeAll { it.element == null }
    val pointer = elementsToAnnotate.keys.find { it.element == element }
    if (pointer != null) {
      val prevMsg = elementsToAnnotate.remove(pointer)!!
      holder.newAnnotation(prevMsg.first, prevMsg.second).create()
    } else {
      if (element is YAMLKeyValue && element.key?.text?.matches(classNameRegex) == true) {
        val value = element.value
        //This means that it is merely a reference to another object
        if (value is YAMLPlainTextImpl) checkReference(value)
        else
          resolveToClass(element)?.let { cls -> //If it's not null, it's a constructor call
            findConstructor(cls)?.let { checkCtorCall(it, element, cls) } ?: addErrorAnnotation(
              element.key!!,
              "Could not find a constructor for class ${cls.name}"
            )
          } ?: addErrorAnnotation(
            element.key!!,
            if (resolveToPackage(element.key!!.text, element.project) != null)
              "${element.key!!.text} is a package, not a class"
            else
              "Could not find ${element.key!!.text}"
          )
      }
    }
  }

  private fun checkCtorCall(ctorDef: PsiMethod, ctorCall: YAMLKeyValue, cls: PsiClass) {
    val params = ctorDef.parameterList.parameters

    val (requiredParams, otherParams) = params.partition(::isRequiredParam)
    val idName = getIdName(cls)

    checkArgs(
      ctorCall,
      ctorCall.key,
      idName,
      idName == null || cls.qualifiedName!!.startsWith(wpiPackage),
      requiredParams.map { it.name },
      otherParams.map { it.name },
      getAllArgs(ctorCall)
    )

    /*val params = ctorDef.parameterList.parameters

    val args = getAllArgs(ctorCall)
    val idName = getIdName(cls)

    val givenParams = mutableSetOf<PsiParameter>()

    //initialised to true if idName is null, i.e., there is no id
    var foundId: Boolean = idName == null

    for (argKey in args) {
      val argName = removeQuotes(argKey.keyText)
      if (argName == idName) foundId = true
      else params.find { it.name == argName }?.let { param ->
        if (param in givenParams) addErrorAnnotation(argKey, "Duplicate argument for property ${param.name}")
        else givenParams += param
      } ?: addErrorAnnotation(
        argKey.key!!,
        "Could not find a parameter named $argName"
      )
    }

    if (!foundId) addErrorAnnotation(ctorCall.key!!, "Id property $idName not given")

    //Check that all the parameters have been entered in
    for (param in params.filter { isRequiredParam(it) && it !in givenParams }) addErrorAnnotation(
      ctorCall.key!!,
      "No argument given for required property ${param.name}"
    )*/
  }

  /**
   * Check all arguments for a constructor call.
   *
   * @param parent The parent (constructor/file?) to highlight with errors
   * @param idName The id parameter (null if there is none)
   * @param requiredParams The names of the required parameters
   * @param otherParams The names of the non-required parameters
   * @param args The actual arguments given that need to be checked
   */
  private fun checkArgs(
    ctorCall: YAMLKeyValue,
    parent: PsiElement?,
    idName: String?,
    isIdRequired: Boolean,
    requiredParams: List<String>,
    otherParams: List<String>,
    args: List<Pair<String, YAMLKeyValue>>
  ) {
    val givenParams = mutableSetOf<String>()
    var foundId = isIdRequired || idName == null

    for ((keyText, keyVal) in args) {
      val argName = removeQuotes(keyText)
      if (argName == idName) {
        foundId = true
        ids[keyVal.valueText] = SmartPointerManager.createPointer(ctorCall)
      } else if (argName in requiredParams || argName in otherParams) {
        if (argName in givenParams) addErrorAnnotation(keyVal, "Duplicate argument for property $argName")
        else givenParams += argName
      } else {
        addErrorAnnotation(
          keyVal.key!!,
          "Could not find a parameter named $argName"
        )
      }
    }

    if (parent != null) {
      if (!foundId) addErrorAnnotation(parent, "No argument given for id parameter $idName")
      //Check that all the parameters have been entered in
      for (param in requiredParams) if (param !in givenParams) addErrorAnnotation(
        parent,
        "No argument given for required property $param"
      )
    }
  }

  /**
   * Check if a reference is invalid
   */
  private fun checkReference(value: YAMLPlainTextImpl) {
    val pointer = ids[value.text]
    val ref = pointer?.element
    if (ref == null) {
      addErrorAnnotation(value, "Could not find previous object with id ${value.text}, ${pointer==null}")
      //If the pointer is there but not the element, the element was deleted
      if (pointer != null) ids.remove(value.text)
    } else if (ref.startOffset > value.startOffset) {
      addErrorAnnotation(value, "Forward reference")
    }
  }

  private fun addAnnotation(element: PsiElement, severity: HighlightSeverity, msg: String) {
    elementsToAnnotate[SmartPointerManager.createPointer(element)] = Pair(severity, msg)
  }

  private fun addErrorAnnotation(element: PsiElement, msg: String) =
    addAnnotation(element, HighlightSeverity.ERROR, msg)

  companion object {
    val LOG: Logger = Logger.getInstance(this::class.java)

    //TODO Make the keys smart pointers to avoid memory leaks
    private val elementsToAnnotate: MutableMap<SmartPsiElementPointer<PsiElement>, Pair<HighlightSeverity, String>> =
      ConcurrentHashMap()
    internal val ids: MutableMap<String, SmartPsiElementPointer<YAMLKeyValue>> = ConcurrentHashMap()
  }
}