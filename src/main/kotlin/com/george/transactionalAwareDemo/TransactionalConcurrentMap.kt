package com.george.transactionalAwareDemo

import java.util.concurrent.ConcurrentHashMap

class TransactionalConcurrentMap<K, V>(
    concurrentHashMap: ConcurrentHashMap<K, V> = ConcurrentHashMap()
) : TransactionalAwareObject<ConcurrentHashMap<K, V>>() {
    private var current: ConcurrentHashMap<K, V> = concurrentHashMap

    override fun commitSafely(action: ConcurrentHashMap<K, V>.() -> Any?): Any? {
        return action(current)
    }

    override fun transactionalAction(action: Action<ConcurrentHashMap<K, V>>): TransactionalAction<ConcurrentHashMap<K, V>> {
        val transactionalAction = ConcurrentHashMapTransactionalAction(action)
        val job = TransactionalJob(transactionalAction)
        addTransaction(job)
        return transactionalAction
    }
}

class ConcurrentHashMapTransactionalAction<K, V>(
    override val value: ConcurrentHashMap<K, V>.() -> Any?
) : TransactionalAction<ConcurrentHashMap<K, V>>

fun <T> TransactionalAwareObject<T>.action(action: Action<T>): TransactionalAction<T> {
    val _action = transactionalAction { action }
    _action.value(this)
}