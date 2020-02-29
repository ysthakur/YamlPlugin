package edu.team449.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.psi.YAMLPsiElement

class MyYamlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        //Logger.getInstance(this::class.java).warn("Got to registerReferenceProviders")
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLPsiElement::class.java) as ElementPattern<out PsiElement>,
            MyYamlReferenceProvider
        )
    }
}