package com.george.transactionalAwareDemo

import java.util.concurrent.ConcurrentHashMap

abstract class TransactionalAwareObject<T> {
    private val jobs = mutableSetOf<TransactionalJob<T>>()

    fun contains(key: TransactionContext.Key<*>): Boolean = jobs.contains(key.asJob())

    fun addTransaction(job: TransactionalJob<T>) {
        jobs.add(job)
    }

    fun commit(key: TransactionContext.Key<*>) {
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

    fun rollback(key: TransactionContext.Key<*>) {
        jobs.remove(key.asJob())
    }

    abstract fun commitSafely(action: T.() -> Any?)

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

class ConcurrentHashMapTransactionalAction<K, V>(
    override val value: ConcurrentHashMap<K, V>.() -> Any?
) : TransactionalAction<ConcurrentHashMap<K, V>>