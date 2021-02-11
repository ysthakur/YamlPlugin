package edu.team449

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.yaml.psi.*
import org.jetbrains.yaml.psi.impl.YAMLAliasImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import java.util.concurrent.ConcurrentHashMap

//todo check that all the arguments' types are correct
class YamlAnnotator : Annotator {

  override fun annotate(elem: PsiElement, holder: AnnotationHolder) {
    //TODO figure out why this was causing a memory leak
//    val project = elem.project
//    if (project !in allIds.keys) {
//      allIds[project] = ConcurrentHashMap()
//      val listener = ChangeListener(project)
//      PsiManager.getInstance(project)
//        .addPsiTreeChangeListener(listener) { PsiManager.getInstance(project).removePsiTreeChangeListener(listener) }
//    }

    val parent = elem.parent
    val element = if (elem is YAMLAliasImpl) anchorValue(elem) else elem
    if (parent is YAMLKeyValue) {
      if (element is YAMLPlainTextImpl && element == parent.value) {
        //It's a value, not a key
        checkPlaintext(element, parent, classOf(parent), holder)
      } else if (element is LeafPsiElement && element == parent.key) {
        if (element.text.matches(classNameWithDotMaybeIncomplete)) {
          //element.text is the name of a class, not a parameter
          checkQualifiedCtorCall(parent, element.text, holder)
        } else if (element.text != "<<") {
          checkArgument(parent, element.text, holder)
        }
      }
    }
  }

  /**
   * Check an argument where the name of a parameter has been given
   */
  private fun checkArgument(arg: YAMLKeyValue, keyText: String, holder: AnnotationHolder) {
    val argType = typeOf(arg)

    val parentIsWPI = (arg.parent?.parent as? YAMLKeyValue?)?.keyText?.let(::isWPIClass) != true

    //Check if such a parameter exists only if it's not part of a Map and it's not an argument to the constructor
    //of a class from wpilib
    if (arg.parent?.firstChild !is LeafPsiElement && arg.parent?.firstChild?.text != "!!map" && parentIsWPI) {
      checkArg(arg, holder)
    }

    if (argType == null) {
      val upperCtor = getUpperConstructorCall(arg)
      val clazz = upperCtor?.let(::classOf)
      val idKey = clazz?.let(::getIdName)

      if (idKey == removeQuotes(keyText)) {
        val ids = getIds(arg.project)
        ids[arg.valueText] = SmartPointerManager.createPointer(arg.value!!)
      }
//      else
//        addWarning(holder, "Type of $keyText unknown")
    } else {
      /*Make sure it doesn't look like this (constructor call under parameter)
      foo:
        org.foo.blah:
          key: val
          key2: val2
      */
      val isCtorCall =
        when (val value = arg.value) {
          is YAMLMapping -> {
            val keyVals = value.keyValues
            !(keyVals.size == 1 && keyVals.iterator().next().keyText.matches(classNameWithDotMaybeIncomplete))
          }
          else -> false
        }
      if (isCtorCall) {
        val argClass = PsiTypesUtil.getPsiClass(argType)!!
        //It's a constructor call, so check it
        val args = getAllArgs(arg)
        val ctor = findConstructor(argClass)

        //Check that all parameters have been given
        if (ctor != null) {
          checkParams(args, ctor.parameterList.parameters.filter(::isRequiredParam).map { it.name }, holder)
        }

        //Check that the id is given
        val (idName, needsId) = getAndNeedsId(argClass)
        if (idName != null) {
          //If the id isn't found but it's required, mark an error
          if (needsId && args.all { removeQuotes(it.first) != idName }) {
            addWarning(holder, "Id $idName not given")
          }
        }
      }
    }
  }

  /**
   * Check if such a parameter exists. If it's the id parameter, add it to the map of ids
   */
  private fun checkArg(arg: YAMLKeyValue, holder: AnnotationHolder) {
    val argName = removeQuotes(arg.keyText)
    if (argName != "<<" && resolveToParameter(arg) == null) {
      val idName = getIdName(getUpperConstructorCall(arg)?.let(::classOf) ?: RobotStuff.robotClass(arg.project))
      //If it's not one of the parameters, and it's not the id, mark a warning
      if (idName != argName) {
        //TODO should this be an error?
        addWarning(holder, "No such parameter: ${argName}")
      }
//      else {
//        ids[arg.valueText] = SmartPointerManager.createPointer(arg.parent.parent as YAMLKeyValue)
//      }
    }
  }

  /**
   * Check if all parameters are given
   */
  private fun checkParams(
    args: List<Pair<String, YAMLKeyValue>>,
    requiredParams: List<String>,
    holder: AnnotationHolder
  ) {
    for (param in requiredParams) {
      if (!args.any { it.first == param }) {
        addError(holder, "Parameter $param not given")
      }
    }
  }

  //TODO implement this properly?
  private fun checkPlaintext(
    element: YAMLPlainTextImpl,
    keyValue: YAMLKeyValue,
    clazz: PsiClass?,
    holder: AnnotationHolder
  ) {
    if (element.firstChild !is YAMLAnchor) {
      val text = element.text
      if (keyValue.keyText.matches(classNameWithDotMaybeIncomplete)) {
        val ref = findById(text, element.project)
        if (ref == null) {
          addError(
            holder,
            "Could not find previous object with id ${text}"
          )
        } else if (ref.startOffset > element.startOffset) {
          addWarning(holder, "Forward reference")
        }
      } else if (text.matches(VALID_IDENTIFIER_REGEX)) {
        if (text == "true" || text == "false") {

        } else if (clazz?.isEnum == true) {

        } else {

        }
      } else { //has to be a number (or string?)

      }
    }
  }

  /**
   * Check a constructor call where the class's name has been given
   */
  private fun checkQualifiedCtorCall(parent: YAMLKeyValue, keyText: String, holder: AnnotationHolder) {
    val argClass = resolveToClass(keyText, parent.project)
    if (argClass == null) {
      addError(holder, "No such class $keyText")
      return
    }

    if (!isWPIClass(keyText)) {
      val isCtorCall = parent.value is YAMLMapping
      if (isCtorCall) {
        //It's a constructor call, so check it
        val args = getAllArgs(parent)
        val ctor = findConstructor(argClass)

        //Check that all parameters have been given
        if (ctor != null)
          checkParams(args, ctor.parameterList.parameters.filter(::isRequiredParam).map { it.name }, holder)
        else
          addWarning(holder, "No constructor found for class $keyText")

        //Check that the id is given
        val (idName, needsId) = getAndNeedsId(argClass)
        if (idName != null) {
          //If the id isn't found but it's required, mark an error
          if (needsId && args.all { removeQuotes(it.first) != idName }) {
            addError(holder, "Id $idName not given")
          }
        }
      }
    } else { //wpilib classes get special treatment
      argClass.constructors.find { ctor ->
        val modifiers = ctor.modifierList
        if (!modifiers.hasModifierProperty("public")) return@find false

        val params = ctor.parameterList.parameters.map { it.name }
        val args = getAllArgs(parent).map { it.first }.filter { it != "'@id'" && it != "requiredSubsystems" }
        params.containsAll(args) && args.containsAll(params)
      } ?: addError(holder, "No suitable constructor found for class $keyText")
    }
  }

  private fun addAnnotation(holder: AnnotationHolder, severity: HighlightSeverity, msg: String) =
    holder.newAnnotation(severity, msg).create()

  private fun addError(holder: AnnotationHolder, msg: String) =
    addAnnotation(holder, HighlightSeverity.ERROR, msg)

  private fun addWarning(holder: AnnotationHolder, msg: String) =
    addAnnotation(holder, HighlightSeverity.WARNING, msg)

  companion object {
    val LOG: Logger = Logger.getInstance(this::class.java)

    private val allIds: MutableMap<Project, MutableMap<String, SmartPsiElementPointer<PsiElement>>> =
      ConcurrentHashMap()

    fun getIds(project: Project): MutableMap<String, SmartPsiElementPointer<PsiElement>> =
      allIds[project] ?: run {
        val newIds = ConcurrentHashMap<String, SmartPsiElementPointer<PsiElement>>()
        allIds[project] = newIds
        newIds
      }

    fun findById(id: String, project: Project): YAMLKeyValue? {
      val ids = getIds(project)
      return ids[id]?.let { ptr ->
        val ref = ptr.element
        if (ref == null || ref.text != id) {
          ids.remove(id)
          LOG.warn("Yay, removed id $id!")
        }
        ref?.parent?.parent?.parent as YAMLKeyValue?
      }
    }
  }

  class ChangeListener(project: Project) : PsiTreeChangeListener {
    override fun beforeChildAddition(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun beforeChildMovement(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun beforePropertyChange(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun childrenChanged(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun childMoved(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

    override fun propertyChanged(event: PsiTreeChangeEvent) {
      //TODO("Not yet implemented")
    }

  }
}