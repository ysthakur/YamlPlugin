package edu.team449

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

//todo check that all the arguments' types are correct
class YamlAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val prevMsg = elementsToAnnotate.remove(element)
    if (prevMsg != null) {
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

    checkArgs(
      ctorCall.key,
      getIdName(cls),
      params.filter(::isRequiredParam).map { it.name },
      params.filterNot(::isRequiredParam).map { it.name },
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
    parent: PsiElement?,
    idName: String?,
    requiredParams: List<String>,
    otherParams: List<String>,
    args: List<YAMLKeyValue>
  ) {
    val givenParams = mutableSetOf<String>()

    //initialised to true if idName is null, i.e., there is no id
    var foundId: Boolean = idName == null

    for (argKey in args) {
      val argName = removeQuotes(argKey.keyText)
      if (argName == idName) foundId = true
      else {
        if (argName in requiredParams || argName in otherParams) {
          if (argName in givenParams) addErrorAnnotation(argKey, "Duplicate argument for property $argName")
          else givenParams += argName
        } else addErrorAnnotation(
          argKey.key!!,
          "Could not find a parameter named $argName"
        )
      }
    }

    if (parent != null) {
      if (!foundId) addErrorAnnotation(parent, "Id property $idName not given")

      //Check that all the parameters have been entered in
      for (param in requiredParams) if (param !in givenParams) addErrorAnnotation(
        parent,
        "No argument given for required property $param"
      )
    }
  }

  private fun checkReference(value: YAMLPlainTextImpl) {
    //Invalid reference
    val ref = resolveToObjectDef(value)
    if (ref.first == null) {
      addErrorAnnotation(value, "Could not find previous object with id ${value.text}")
    } else if (ref.second) {
      addErrorAnnotation(value, "Illegal forward reference")
    } else {
      addAnnotation(value, HighlightSeverity.INFORMATION, "Resolved to ${ref.first?.text}")
    }
  }

  private fun addAnnotation(element: PsiElement, severity: HighlightSeverity, msg: String) {
    elementsToAnnotate[element] = Pair(severity, msg)
  }

  private fun addErrorAnnotation(element: PsiElement, msg: String) =
    addAnnotation(element, HighlightSeverity.ERROR, msg)

  companion object {
    private val elementsToAnnotate: MutableMap<PsiElement, Pair<HighlightSeverity, String>> = HashMap()
  }
}