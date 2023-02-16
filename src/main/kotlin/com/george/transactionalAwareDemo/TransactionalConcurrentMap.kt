package com.george.transactionalAwareDemo

import java.util.concurrent.ConcurrentHashMap

class TransactionalConcurrentMap<K, V>(
    concurrentHashMap: ConcurrentHashMap<K, V> = ConcurrentHashMap()
) : TransactionalAwareObject<ConcurrentHashMap<K, V>>() {
    private var current: ConcurrentHashMap<K, V> = concurrentHashMap

    override fun <R> commitSafely(action: ConcurrentHashMap<K, V>.() -> R): R {
        return action(current)
    }

    override fun <R> transactionalAction(action: Action<ConcurrentHashMap<K, V>, R>): TransactionalAction<ConcurrentHashMap<K, V>, R> {
        val transactionalAction = ConcurrentHashMapTransactionalAction(action)
        val job = TransactionalJob(transactionalAction)
        addTransaction(job)
        return transactionalAction
    }
}

class ConcurrentHashMapTransactionalAction<K, V, R>(
    override val value: ConcurrentHashMap<K, V>.() -> R
) : TransactionalAction<ConcurrentHashMap<K, V>, R>