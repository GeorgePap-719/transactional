package com.george.transactionalAwareDemo

import java.util.concurrent.ConcurrentHashMap

class TransactionalConcurrentMap<K, V>(
    concurrentHashMap: ConcurrentHashMap<K, V> = ConcurrentHashMap()
) : TransactionalAwareObject<ConcurrentHashMap<K, V>>() {
    private var current: ConcurrentHashMap<K, V> = concurrentHashMap

    override fun commitSafely(action: ConcurrentHashMap<K, V>.() -> Any?) {
        action(current)
    }

    fun TransactionalScope.action(action: ConcurrentHashMap<K, V>.() -> Any?) {
        val transactionalAction = ConcurrentHashMapTransactionalAction(action)
        val job = TransactionalJob(transactionalAction)
        addTransaction(job)
    }
}