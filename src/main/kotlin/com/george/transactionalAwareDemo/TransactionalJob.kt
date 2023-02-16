package com.george.transactionalAwareDemo

open class TransactionalJob<T, R>(
    action: TransactionalAction<T, R>
) : TransactionContext.TransactionalElement() {
    private var state = ActionState.WAITING
    private val actionValue = action.value

    override val key: Key<*> get() = Job

    val isCommitted = state == ActionState.COMMITTED
    val isROLLBACKED = state == ActionState.ROLLBACKED

    private fun getActionOrNull(): Action<T, R>? = if (state == ActionState.WAITING) actionValue else null

    fun getActionAndCommitOrNull(perform: (action: Action<T, R>) -> Any?): Any? { // null || Unit
        val canDoAction = getActionOrNull() ?: return null
        try {
            perform(canDoAction)
        } catch (e: Throwable) {
            tryMarkRollback()
            throw e
        }
        tryMarkCommitted()
        return Unit
    }

    private fun tryMarkCommitted() {
        if (state != ActionState.WAITING) error("forbidden state:$state, should have been waiting")
        state = ActionState.COMMITTED
    }

    private fun tryMarkRollback() {
        if (state != ActionState.WAITING) error("forbidden state:$state, should have been waiting")
        state = ActionState.ROLLBACKED
    }

    companion object Job : Key<TransactionalJob<*, *>>

    private enum class ActionState {
        WAITING,
        COMMITTED,
        ROLLBACKED,
    }
}