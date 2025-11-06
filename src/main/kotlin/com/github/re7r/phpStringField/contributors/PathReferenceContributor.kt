package com.github.re7r.phpStringField.contributors

import com.github.re7r.phpNavigator.references.MethodPathReference
import com.github.re7r.phpStringField.annotators.ParameterAnnotator
import com.github.re7r.phpStringField.references.PropertyPathReference
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.intellij.openapi.util.Key

class PathReferenceContributor : PsiReferenceContributor() {
    private val classStringResolveKey = Key.create<Any>("@string-field:class-string-resolve")
    private object Failed

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java), object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement, context: ProcessingContext
                ): Array<PsiReference> {
                    if (element !is StringLiteralExpression) {
                        return emptyArray()
                    }

                    val paramList = PsiTreeUtil.getParentOfType(
                        element, ParameterList::class.java
                    ) ?: return emptyArray()

                    val paramIndex = paramList.parameters.indexOf(element)
                    val methodRef = paramList.parent as? MethodReference ?: return emptyArray()
                    val method = resolveMethod(methodRef) ?: return emptyArray()
                    val param = method.parameters.getOrNull(paramIndex) ?: return emptyArray()

                    val isCallable = when (param.getUserData(ParameterAnnotator.Shared.IS_PATH_PARAM)) {
                        true -> param.getUserData(ParameterAnnotator.Shared.IS_CALLABLE) == true
                        null -> ParameterAnnotator.Shared.visit(param)
                        false -> null
                    } ?: return emptyArray()

                    val references = mutableListOf<PsiReference>()
                    val segments = parseElementPath(element) ?: return emptyArray()

                    var currentClass = resolveClassFromMethodRef(methodRef) ?: return emptyArray()
                    var currentOffset = 1

                    for (segment in segments) {
                        if (segment.isEmpty()) {
                            currentOffset += 1
                            continue
                        }

                        references.add(
                            PropertyPathReference(
                                element, TextRange(
                                    currentOffset, currentOffset + segment.length
                                ), currentClass, segment
                            )
                        )

                        if (isCallable) {
                            references.add(
                                MethodPathReference(
                                    element,
                                    TextRange(currentOffset, currentOffset + segment.length),
                                    currentClass,
                                    segment
                                )
                            )

                            val method = currentClass.methods
                                .filterIsInstance<Method>()
                                .firstOrNull { it.name == segment }

                            if (method != null) {
                                break
                            }
                        }

                        val field = currentClass.fields.filterIsInstance<Field>().firstOrNull { it.name == segment }

                        if (field != null) {
                            val fieldType = field.type
                            val nextClass = resolveClassFromType(fieldType, element.project)
                            if (nextClass != null) {
                                currentClass = nextClass
                                currentOffset += segment.length + 1
                                continue
                            }
                        }

                        break
                    }

                    return references.toTypedArray()
                }
            })
    }

    private fun parseElementPath(element: PsiElement): List<String>? {
        val contents = (element as? StringLiteralExpression)?.contents ?: return emptyList()
        return contents.split('.').takeIf { it.any { sublist -> sublist.isNotEmpty() } }
    }

    private fun resolveMethod(methodRef: MethodReference): Method? {
        return methodRef.resolve() as? Method ?: methodRef.resolveLocal().filterIsInstance<Method>().firstOrNull()
    }

    private fun resolveClassFromMethodRef(methodRef: MethodReference): PhpClass? {
        val phpType = (methodRef.classReference?.type ?: PhpType.EMPTY).global(methodRef.project)
        if (phpType == PhpType.EMPTY) return null

        if (phpType.toString().contains("\\class-string")) {
            return resolveClassFromClassStringVariable(methodRef.classReference as PhpTypedElement)
        }

        return resolveClassFromType(phpType, methodRef.project)
    }

    fun resolveClassFromClassStringVariable(element: PhpTypedElement): PhpClass? {
        val cached = element.getUserData(classStringResolveKey)

        when (cached) {
            is PhpClass -> return cached
            Failed -> return null
        }

        val file = element.containingFile
        val assignments = PsiTreeUtil.findChildrenOfType(file, AssignmentExpression::class.java)
            .filter { it.textOffset < element.textOffset }

        fun findLastAssignmentTo(target: PsiElement, beforeOffset: Int): AssignmentExpression? = when (target) {
            is Variable -> assignments.lastOrNull {
                (it.variable as? Variable)?.name == target.name && it.textOffset < beforeOffset
            }
            is FieldReference -> assignments.lastOrNull {
                val assigned = it.variable as? FieldReference ?: return@lastOrNull false
                assigned.name == target.name &&
                        assigned.classReference?.text == target.classReference?.text &&
                        it.textOffset < beforeOffset
            }
            else -> null
        }

        fun resolveFromExpression(expr: PhpPsiElement?, beforeOffset: Int): PhpClass? = when (expr) {
            is ClassConstantReference -> (expr.classReference as? ClassReference)?.resolve() as? PhpClass
            is Variable -> findLastAssignmentTo(expr, beforeOffset)
                ?.let { resolveFromExpression(it.value, it.textOffset) }
            is FieldReference -> findLastAssignmentTo(expr, beforeOffset)
                ?.let { resolveFromExpression(it.value, it.textOffset) }
            else -> null
        }

        val resolved = when (element) {
            is Variable -> findLastAssignmentTo(element, element.textOffset)
                ?.let { resolveFromExpression(it.value, it.textOffset) }
            is FieldReference -> findLastAssignmentTo(element, element.textOffset)
                ?.let { resolveFromExpression(it.value, it.textOffset) }
            else -> null
        }

        element.putUserData(classStringResolveKey, resolved ?: Failed)
        return resolved
    }

    private fun resolveClassFromType(phpType: PhpType, project: Project): PhpClass? {
        return phpType.types.asSequence().map { it.removePrefix("\\") }.mapNotNull { fqn ->
            PhpIndex.getInstance(project).getClassesByFQN(fqn).firstOrNull()
        }.firstOrNull()
    }
}


