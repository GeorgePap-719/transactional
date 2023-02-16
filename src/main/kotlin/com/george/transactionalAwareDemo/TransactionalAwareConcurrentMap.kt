package com.george.transactionalAwareDemo

import java.util.concurrent.ConcurrentHashMap

typealias TransactionalContextAction<K, V> =
        ConcurrentHashMap<TransactionContext.Key<*>, ConcurrentHashMap<K, V>.() -> Unit>

class TransactionalConcurrentMap<K, V>(concurrentHashMap: ConcurrentHashMap<K, V> = ConcurrentHashMap()) {
    private var current: ConcurrentHashMap<K, V> = concurrentHashMap
    private var context: TransactionalContextAction<K, V> = ConcurrentHashMap()

    // -- TransactionalAwareObject

    fun commit(key: TransactionContext.Key<*>) {
        val action = context[key] ?: error("not possible")
        action(current)
    }

    fun rollback(key: TransactionContext.Key<*>) {
        context.remove(key)
    }
}

fun <R> transactional(
    context: TransactionContext, // todo empty
    block: TransactionScope.() -> R
): R {
    try {
        // block
    } catch (e: Throwable) {
        // rollback
        throw e
    }
    // commit()
}

abstract class TransactionalAwareObject<T> {
    private val contextStore = ConcurrentHashMap<TransactionContext.Key<*>, TransactionalAction<T>>()
    private val contextStore2 = setOf<TransactionalJob<TransactionalAwareObject<T>>>()

    fun commit(key: TransactionContext.Key<*>) {
        val transactionalAction = contextStore[key]
        if (transactionalAction == null) {
            // action is rollbacked already
            rollback(key)
        } else {
            try {
                commitSafely(transactionalAction.value)
            } catch (e: Throwable) {
                rollback(key)
                throw e
            }
            finalizeCommit(key)
        }
    }

    fun rollback(key: TransactionContext.Key<*>) {
        contextStore.remove(key)
    }

    abstract fun commitSafely(action: T.() -> Any?)

    private fun finalizeCommit(key: TransactionContext.Key<*>) {
        contextStore.remove(key)
    }
}

interface TransactionalAction<in T> {
    val value: T.() -> Any?
}

typealias Action<T> = T.() -> Any?

class ConcurrentHashMapTransactionalAction<K, V>(
    override val value: ConcurrentHashMap<K, V>.() -> Any?
) : TransactionalAction<ConcurrentHashMap<K, V>>

open class TransactionalJob<T>(
    action: TransactionalAction<T>
) : TransactionContext.TransactionalElement() {
    private val jobId = 0L + this.hashCode()

    private var state = ActionState.WAITING
    private val actionValue = action.value

    override val key: Key<*> get() = Job

    val isCommitted = state == ActionState.COMMITTED
    val isROLLBACKED = state == ActionState.ROLLBACKED

    private fun getActionOrNull(): Action<T>? = if (state == ActionState.WAITING) actionValue else null

    fun getActionAndCommitOrReturn(perform: (action: Action<T>) -> Any?) {
        val canDoAction = getActionOrNull() ?: return
        try {
            perform(canDoAction)
        } catch (e: Throwable) {
            tryRollback()
            throw e
        }
        tryCommit()
    }

    private fun tryCommit() {
        if (state != ActionState.WAITING) error("forbidden state:$state, should have been waiting")
        state = ActionState.COMMITTED
    }

    private fun tryRollback() {
        if (state != ActionState.WAITING) error("forbidden state:$state, should have been waiting")
        state = ActionState.ROLLBACKED
    }

    companion object Job : Key<TransactionalJob<*>>

    private enum class ActionState {
        WAITING,
        COMMITTED,
        ROLLBACKED,
    }
}


fun main() {
    val map = TransactionalConcurrentMap<String, String>()


    // goal
    /*
     * transactional {
     * .. transactionalAware code here
     * ..
     *  }
     *
     * commit if everything is ok else rollback
     *
     */

}


