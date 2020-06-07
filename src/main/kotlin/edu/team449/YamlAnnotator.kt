package edu.team449

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

//todo check that all the arguments' types are correct
class YamlAnnotator : Annotator {

  private val elementsToAnnotate: MutableMap<PsiElement, Pair<HighlightSeverity, String>> = HashMap()

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val prevMsg = elementsToAnnotate.remove(element)
    if (prevMsg != null) {
      holder.newAnnotation(prevMsg.first, prevMsg.second).create()
      return
    }

    if (element.project.robotClass() == null) {
      Logger.getInstance(javaClass).error("Oh no, robot class is null!!!!!")
    }
    if (element !is YAMLKeyValue) {
      return
    }
    if (!(element.key ?: return).text.matches(classNameRegex)) {
      return
    }

    val value = element.value
    //This means that it is merely a reference to another object
    if (value is YAMLPlainTextImpl) {
      //Invalid reference
      val ref = resolveToObjectDef(value)
      if (ref.first == null) {
        addErrorAnnotation(value, "Could not find previous object with id ${value.text}")
      } else if (ref.second) {
        addErrorAnnotation(value, "Illegal forward reference")
      }
      return
    }

    val cls = resolveToClass(element)
    if (cls == null) {
      addErrorAnnotation(
        element.key!!,
        if (resolveToPackage(element.key!!.text, element.project) != null)
          "${element.key!!.text} is a package, not a class"
        else
          "Could not find ${element.key!!.text}"
      )
    } else {
      val constructor = resolveToConstructor(cls)
      if (constructor == null) {
        addErrorAnnotation(element.key!!, "Could not find a constructor for class ${cls.name}")
        return
      }
      val params = constructor.parameterList.parameters.toMutableList()

      try {
        val args = getAllArgs(element)
        val idName = getIdName(cls)

        // The Boolean value says whether or not the corresponding parameter has been set
        val requiredParams: MutableMap<PsiParameter, Boolean> =
          params.filter(::isRequiredParam).map { Pair(it, false) }.toMap().toMutableMap()

        val paramsToArgs = mutableMapOf<PsiParameter, YAMLKeyValue>()

        //initialised to true if idName is null, i.e., there is no id
        var foundId: Boolean = idName == null

        for (argKey in args) {
          val keyText = argKey.keyText.withoutQuotes()
          params.find { p -> p.name == keyText }?.let {
            paramsToArgs[it] = argKey
            if (requiredParams[it] == true)
              addErrorAnnotation(argKey, "Duplicate argument for property ${it.name}")
            else requiredParams[it] = true
          } ?: if (keyText == idName) foundId = true
          else addErrorAnnotation(
            argKey.key!!,
            "Could not find a parameter named $keyText"
          )
        }

        //Check that all the parameters have been entered in
        for (param in requiredParams) {
          if (param.value == false)
            addErrorAnnotation(element.key!!, "No argument given for required property ${param.key.name}")
        }

        if (!foundId) {
          addErrorAnnotation(element.key!!, "Id property $idName not given")
        }
      } catch (nspe: NoSuchParameterException) {
        addErrorAnnotation(nspe.elem, "Could not resolve parameter")
      }
    }
  }

  fun addAnnotation(element: PsiElement, severity: HighlightSeverity, msg: String) {
    elementsToAnnotate[element] = Pair(severity, msg)
  }

  fun addErrorAnnotation(element: PsiElement, msg: String) = addAnnotation(element, HighlightSeverity.ERROR, msg)
}