package com.github.re7r.phpStringField.references

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass

class PropertyPathReference(
    element: PsiElement,
    range: TextRange,
    private val contextClass: PhpClass,
    private val propertyName: String
) : PsiReferenceBase<PsiElement>(element, range) {
    override fun resolve(): PsiElement? {
        return contextClass.fields.firstOrNull {
            it.name == propertyName && it is Field && it.modifier.isPublic
        } as? Field
    }

    override fun getVariants(): Array<Any> {
        return contextClass.fields
            .asSequence()
            .filterIsInstance<Field>()
            .filter { it.modifier.isPublic }
            .map { field ->
                LookupElementBuilder
                    .create(field.name)
                    .withIcon(field.getIcon(0))
                    .withTypeText(field.type.toString(), true)
                    .withTailText(" in ${field.containingClass?.name}", true)
            }
            .toList()
            .toTypedArray()
    }
}