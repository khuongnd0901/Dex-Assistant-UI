package com.khuongnd.dexassistant.ui.interaction

/** A reply belongs to one prompt of one parent action, and is consumed before dispatch. */
sealed interface SpokenReply {
    data object Confirm : SpokenReply
    data object Next : SpokenReply
    data object Previous : SpokenReply
    data object Cancel : SpokenReply
    data class Select(val index: Int) : SpokenReply
}
sealed interface InteractionResult<out T> {
    data class Selected<T>(val value: T) : InteractionResult<T>
    data class Confirmed<T>(val value: T) : InteractionResult<T>
    data object Cancelled : InteractionResult<Nothing>
    data class PageChanged(val page: Int) : InteractionResult<Nothing>
    data object Invalid : InteractionResult<Nothing>
    data object NewCommand : InteractionResult<Nothing>
}
class PendingInteraction<T>(val parentId: String, val actionId: String, choices: List<T>) {
    private val choices = choices.toList()
    private var selected: T? = this.choices.singleOrNull()
    private var deadline: Long? = null
    private var consumed = false
    var promptVersion = 1
        private set
    var page = 0; private set
    val pageChoices get() = choices.drop(page * 5).take(5)
    val choiceCount get() = choices.size
    fun reprompt() { deadline = null; promptVersion++ }
    val selecting get() = selected == null
    val selectedValue get() = selected
    val waitingForTts get() = !consumed && deadline == null
    init { require(choices.isNotEmpty()) }

    fun ttsFinished(version: Int, success: Boolean, now: Long): Boolean {
        if (consumed || version != promptVersion || deadline != null) return false
        if (!success) { consumed = true; return false }
        deadline = now + 10_000
        return true
    }
    fun expired(now: Long) = !consumed && deadline?.let { now >= it } == true
    fun cancel() { consumed = true; deadline = null }
    fun reply(reply: SpokenReply?, now: Long): InteractionResult<T> {
        if (consumed || deadline == null) return InteractionResult.Invalid
        if (expired(now)) { cancel(); return InteractionResult.Cancelled }
        if (reply == null) { cancel(); return InteractionResult.NewCommand }
        if (reply == SpokenReply.Cancel) { cancel(); return InteractionResult.Cancelled }
        if (selecting) {
            if (reply == SpokenReply.Next || reply == SpokenReply.Previous) {
                val next = page + if (reply == SpokenReply.Next) 1 else -1
                if (next !in 0..(choices.size - 1) / 5) return InteractionResult.Invalid
                page = next; reprompt()
                return InteractionResult.PageChanged(page)
            }
            val candidate = (reply as? SpokenReply.Select)?.index?.let { choices.getOrNull(it - 1) }
                ?: return InteractionResult.Invalid
            selected = candidate; deadline = null; promptVersion++
            return InteractionResult.Selected(candidate)
        }
        if (reply != SpokenReply.Confirm) return InteractionResult.Invalid
        val value = selected ?: return InteractionResult.Invalid
        cancel() // duplicate confirmations cannot dispatch even if revalidation/execution is async.
        return InteractionResult.Confirmed(value)
    }
}
