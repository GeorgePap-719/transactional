package com.george.transactionalAwareDemo

import com.george.transactionalAwareDemo.TransactionContext.*
import java.util.concurrent.ConcurrentHashMap

typealias TransactionalContextAction<K, V> =
        ConcurrentHashMap<Key<*>, ConcurrentHashMap<K, V>.() -> Unit>

class TransactionalConcurrentMap<K, V>(concurrentHashMap: ConcurrentHashMap<K, V> = ConcurrentHashMap()) {
    private var current: ConcurrentHashMap<K, V> = concurrentHashMap
    private var context: TransactionalContextAction<K, V> = ConcurrentHashMap()

    // -- TransactionalAwareObject

    fun commit(key: Key<*>) {
        val action = context[key] ?: error("not possible")
        action(current)
    }

    fun rollback(key: Key<*>) {
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
    private val jobs = mutableSetOf<TransactionalJob<T>>()

    fun contains(key: Key<*>): Boolean = jobs.contains(key.asJob())

    fun addTransaction(job: TransactionalJob<T>) {
        jobs.add(job)
    }

    fun commit(key: Key<*>) {
        val transactionalJob = jobs[key]
        if (transactionalJob == null) {
            // action is rollbacked already.
            // There is nothing we can do at this point
        } else {
            val action = transactionalJob.getActionAndCommitOrNull {
                try {
                    commitSafely(it)
                } catch (e: Throwable) {
                    rollback(key)
                    throw e
                }
                finalizeCommit(key)
            }
            if (action == null) {
                rollback(key)
            }
        }
    }

    fun rollback(key: Key<*>) {
        jobs.remove(key.asJob())
    }

    abstract fun commitSafely(action: T.() -> Any?)

    private fun finalizeCommit(key: Key<*>) {
        jobs.remove(key.asJob())
    }

    @Suppress("UNCHECKED_CAST")
    private fun Key<*>.asJob(): TransactionalJob<T> =
        this as TransactionalJob<T>
}

private operator fun <E : TransactionalElement> Collection<E>.get(key: Key<*>): E? = find { it.key == key }

interface TransactionalAction<in T> {
    val value: T.() -> Any?
}

typealias Action<T> = T.() -> Any?

class ConcurrentHashMapTransactionalAction<K, V>(
    override val value: ConcurrentHashMap<K, V>.() -> Any?
) : TransactionalAction<ConcurrentHashMap<K, V>>

open class TransactionalJob<T>(
    action: TransactionalAction<T>
) : TransactionalElement() {
    private var state = ActionState.WAITING
    private val actionValue = action.value

    override val key: Key<*> get() = Job

    val isCommitted = state == ActionState.COMMITTED
    val isROLLBACKED = state == ActionState.ROLLBACKED

    private fun getActionOrNull(): Action<T>? = if (state == ActionState.WAITING) actionValue else null

    fun getActionAndCommitOrNull(perform: (action: Action<T>) -> Any?): Any? { // null || Unit
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


