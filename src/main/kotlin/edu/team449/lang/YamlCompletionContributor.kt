package edu.team449.lang

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionType.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes


class YamlCompletionContributor : CompletionContributor() {
    init {
        extend(
            BASIC,
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withLanguage(YAMLLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    @NotNull parameters: CompletionParameters,
                    context: ProcessingContext,
                    @NotNull resultSet: CompletionResultSet
                ) {
                    resultSet.addElement(LookupElementBuilder.create("org.usfirst.frc.team449.robot"))
                }
            }
        )
    }
}