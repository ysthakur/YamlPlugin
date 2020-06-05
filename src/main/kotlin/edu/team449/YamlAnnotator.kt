package edu.team449

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.util.containers.toMutableSmartList
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

//todo check that all the arguments' types are correct
class YamlAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {

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
        holder.createErrorAnnotation(value, "Could not find previous object with id ${value.text}")
      } else if (ref.second) {
        holder.createErrorAnnotation(value, "Illegal forward reference")
      }
      return
    }

    val cls = resolveToClass(element)
    if (cls == null) {
      holder.createErrorAnnotation(
        element.key!!,
        if (resolveToPackage(element.key!!.text, element.project) != null)
          "${element.key!!.text} is a package, not a class"
        else
          "Could not find ${element.key!!.text}"
      )
    } else {
      val constructor = resolveToConstructor(cls)
      if (constructor == null) {
        holder.createErrorAnnotation(element.key!!, "Could not find a constructor for class ${cls.name}")
        return
      }
      val params = constructor.parameterList.parameters.toMutableList()
      //Parameters get removed as arguments for them are found
      val requiredParams =
        params.filter { p ->
          p.annotations.find { a ->
            a.text.contains(Regex("""@JsonProperty\(.*required( )?=( )?true"""))
          } != null
        }.toMutableSmartList()

      val idName = getIdName(cls)

      //val args = element.value!! as YAMLMappingImpl
      val args = try {
        getAllArgs(element)
      } catch (nspe: NoSuchParameterException) {
        holder.createErrorAnnotation(nspe.elem, "Could not resolve parameter")
        return
      }

      val paramsToArgs = mutableMapOf<PsiParameter, YAMLKeyValue>()
      for (argKey in args) {
        val keyText = argKey.keyText.withoutQuotes()
        val param = params.find { p -> p.name == keyText }
        if (param != null) {
          paramsToArgs[param] = argKey
          requiredParams.remove(param)
        } else if (keyText != idName) holder.createErrorAnnotation(
          argKey.key!!,
          "Could not find a parameter named $keyText"
        )
      }

      //Check that all the parameters have been entered in
      for (param in requiredParams) {
        //Required by Jackson but not given
        holder.createErrorAnnotation(element.key!!, "No argument given for required property ${param.name}")
      }
      if (needsIdAnnotation(cls) && findIdArg(args, cls) == null) {
        holder.createErrorAnnotation(element.key!!, "Id property $idName not given")
      }
    }
  }
}