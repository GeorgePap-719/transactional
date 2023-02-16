package com.george.transactionalAwareDemo

abstract class TransactionalAwareObject<T> {
    private val jobs = mutableSetOf<TransactionalJob<T>>()

    fun contains(key: TransactionContext.Key<*>): Boolean = jobs.contains(key.asJob())

    fun addTransaction(job: TransactionalJob<T>) {
        jobs.add(job)
    }

    fun commit(key: TransactionContext.Key<*>): Any? { // result || TransactionState
        val transactionalJob = jobs[key]
        if (transactionalJob == null) {
            // action is rollbacked already.
            // There is nothing we can do at this point
            return ROLLEDBACK
        } else {
            var commitResult: Any? = null
            val action = transactionalJob.getActionAndCommitOrNull {
                try {
                    commitResult = commitSafely(it)
                } catch (e: Throwable) {
                    rollback(key)
                    throw e
                }
                finalizeCommit(key)
            }
            return if (action == null) {
                rollback(key)
                error("job:$transactionalJob has commenced rollback")
            } else {
                commitResult
            }
        }
    }

    fun rollback(key: TransactionContext.Key<*>) {
        jobs.remove(key.asJob())
    }

    abstract fun commitSafely(action: Action<T>): Any?
    abstract fun transactionalAction(action: Action<T>): TransactionalAction<T>

    private fun finalizeCommit(key: TransactionContext.Key<*>) {
        jobs.remove(key.asJob())
    }

    @Suppress("UNCHECKED_CAST")
    private fun TransactionContext.Key<*>.asJob(): TransactionalJob<T> =
        this as TransactionalJob<T>
}

private operator fun <E : TransactionContext.TransactionalElement> Collection<E>.get(key: TransactionContext.Key<*>): E? =
    find { it.key == key }

interface TransactionalAction<in T> {
    val value: T.() -> Any?
}
typealias Action<T> = T.() -> Any?

data class TransactionState(val value: String)

val ROLLEDBACK = TransactionState("ROLLEDBACK")