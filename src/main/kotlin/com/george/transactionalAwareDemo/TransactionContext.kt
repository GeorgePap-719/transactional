package com.george.transactionalAwareDemo

import java.util.concurrent.ConcurrentHashMap

typealias ContextValue = ConcurrentHashMap<TransactionContext.Key<*>, TransactionContext.TransactionalElement>

open class TransactionContext(private val hashMap: ContextValue) {
    private val entries: Set<MutableMap.MutableEntry<Key<*>, TransactionalElement>> = hashMap.entries.toSet()

    open operator fun <E : TransactionalElement> get(key: Key<E>): E? {
        return if (hashMap.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            hashMap[key] as E
        } else {
            null
        }
    }

    operator fun plus(context: TransactionContext): TransactionContext {
        val newContext = ConcurrentHashMap<Key<*>, TransactionalElement>()
        newContext.putAll(hashMap)
        context.entries.forEach { newContext[it.key] = it.value }
        return TransactionContext(newContext)
    }

    operator fun minus(key: Key<*>): TransactionContext {
        val newContext = ConcurrentHashMap<Key<*>, TransactionalElement>()
        newContext.putAll(hashMap)
        newContext.remove(key)
        return TransactionContext(newContext)
    }

    interface Key<E : TransactionalElement>

    abstract class TransactionalElement : TransactionContext(ConcurrentHashMap()) {
        abstract val key: Key<*>

        override fun <E : TransactionalElement> get(key: Key<E>): E? =
            @Suppress("UNCHECKED_CAST")
            if (this.key == key) this as E else null
    }
}

fun newTransactionContext(context: ContextValue): TransactionContext = if (context.isEmpty()) {
    TransactionContext(ConcurrentHashMap())
} else {
    TransactionContext(context)
}

//

interface TransactionScope {
    val transactionContext: TransactionContext
}