package com.emergetools.relax

import androidx.test.uiautomator.UiSelector

class RelaxSelector(
    private val packageName: String,
    private var uiSelector: UiSelector = UiSelector()
) {

    fun checkable(checkable: Boolean) = apply {
        uiSelector = uiSelector.checkable(checkable)
    }

    fun checked(checked: Boolean) = apply {
        uiSelector = uiSelector.checked(checked)
    }

    fun child(select: RelaxSelector.() -> Unit) = apply {
        val selector = RelaxSelector(packageName).apply(select).toUiSelector()
        uiSelector = uiSelector.childSelector(selector)
    }

    fun className(type: Class<*>) = apply {
        uiSelector = uiSelector.className(type)
    }

    fun className(className: String) = apply {
        uiSelector = uiSelector.className(className)
    }

    fun classNameMatches(regex: String) = apply {
        uiSelector = uiSelector.classNameMatches(regex)
    }

    fun clickable(clickable: Boolean) = apply {
        uiSelector = uiSelector.clickable(clickable)
    }

    fun description(description: String) = apply {
        uiSelector = uiSelector.description(description)
    }

    fun descriptionContains(description: String) = apply {
        uiSelector = uiSelector.descriptionContains(description)
    }

    fun descriptionMatches(regex: String) = apply {
        uiSelector = uiSelector.descriptionMatches(regex)
    }

    fun descriptionStartsWith(description: String) = apply {
        uiSelector = uiSelector.descriptionStartsWith(description)
    }

    fun enabled(enabled: Boolean) = apply {
        uiSelector = uiSelector.enabled(enabled)
    }

    fun focusable(focusable: Boolean) = apply {
        uiSelector = uiSelector.focusable(focusable)
    }

    fun focused(focused: Boolean) = apply {
        uiSelector = uiSelector.focused(focused)
    }

    fun index(index: Int) = apply {
        uiSelector = uiSelector.index(index)
    }

    fun instance(instance: Int) = apply {
        uiSelector = uiSelector.instance(instance)
    }

    fun longClickable(longClickable: Boolean) = apply {
        uiSelector = uiSelector.longClickable(longClickable)
    }

    fun packageName(packageName: String) = apply {
        uiSelector = uiSelector.packageName(packageName)
    }

    fun packageNameMatches(regex: String) = apply {
        uiSelector = uiSelector.packageNameMatches(regex)
    }

    fun parent(select: RelaxSelector.() -> Unit) = apply {
        val selector = RelaxSelector(packageName).apply(select).toUiSelector()
        uiSelector = uiSelector.fromParent(selector)
    }

    fun resId(resId: String) = apply {
        val id = when {
            resId.startsWith("id/") -> "$packageName:$resId"
            resId.contains(":id/") -> resId
            else -> "$packageName:id/$resId"
        }
        uiSelector = uiSelector.resourceId(id)
    }

    fun resIdMatches(regex: String) = apply {
        uiSelector = uiSelector.resourceIdMatches(regex)
    }

    fun scrollable(scrollable: Boolean) = apply {
        uiSelector = uiSelector.scrollable(scrollable)
    }

    fun selected(selected: Boolean) = apply {
        uiSelector = uiSelector.selected(selected)
    }

    fun text(text: String) = apply {
        uiSelector = uiSelector.text(text)
    }

    fun textContains(text: String) = apply {
        uiSelector = uiSelector.textContains(text)
    }

    fun textMatches(regex: String) = apply {
        uiSelector = uiSelector.textMatches(regex)
    }

    fun textOrResId(textOrResId: String) = apply {
        uiSelector = when {
            textOrResId.startsWith("id/") -> {
                uiSelector.resourceId("$packageName:$textOrResId")
            }
            textOrResId.contains(":id/") -> uiSelector.resourceId(textOrResId)
            else -> uiSelector.text(textOrResId)
        }
    }

    fun textStartsWith(text: String) = apply {
        uiSelector = uiSelector.textStartsWith(text)
    }

    fun toUiSelector(): UiSelector {
        return uiSelector
    }

    override fun toString(): String {
        return StringBuilder()
            .append(RelaxSelector::class.java.simpleName)
            .append('[')
            .append(uiSelector.toString())
            .append(']')
            .toString()
    }
}
