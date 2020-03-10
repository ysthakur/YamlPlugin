package edu.team449.lang

import org.jetbrains.yaml.psi.YAMLKeyValue

fun String.withoutQuotes() = this.removeSurrounding("\"")

fun isConstructorCall(constructorCall: YAMLKeyValue) = constructorCall.keyText.replace("IntellijIdeaRulezzz", "").matches(classNameRegex)
