package com.george.transactionalAwareDemo.planc

import java.util.concurrent.ConcurrentHashMap

fun test() {
    val list = listOf<String>()
}

/*
 * Problems to solve:
 * 1) How transactional {} block should behave.
 * 2) how operations should behave if they try to add the same on the same index.
 */

typealias Action<T, R> = T.() -> R

/**
 * Will act as a store for actions.
 */
class TransactionalContext<T> {
    private val actions = ConcurrentHashMap<Key, Action<T, *>>()

    operator fun <R> get(key: Key): Action<T, R>? {
        @Suppress("UNCHECKED_CAST")
        return actions[key] as Action<T, R>?
    }

    operator fun <R> set(key: Key, action: Action<T, R>) {
        actions[key] = action
    }

    fun removeAt(key: Key) {
        actions.remove(key)
    }

    class Key {
        private val value: Int = this.hashCode()

        override fun toString(): String = "Key($value)"
    }
}

/**
 * This implementation does not solve concurrency issues.
 */
class TransactionalList<T>(private val list: MutableList<T>) : AbstractMutableList<T>() {
    private val transactions = TransactionalContext<MutableList<T>>()

    override val size: Int get() = list.size

    override fun add(index: Int, element: T) {
        list.add(index, element)
    }

    override operator fun get(index: Int): T = list[index]

    override fun removeAt(index: Int): T = removeAt(index)

    override fun set(index: Int, element: T): T {
        val elementAt = list[index]
        list[index] = element
        return elementAt
    }


    fun <R> TransactionalScope.commit(key: TransactionalContext.Key) {
        val action = transactions.get<R>(key) ?: error("todo")
        action(list)
        transactions.removeAt(key) // finalize transaction
    }

    fun TransactionalScope.rollback(key: TransactionalContext.Key) {
        transactions.removeAt(key)
    }

}


interface TransactionalScope

/*
 * Not sure how this should behave
 */
fun <T> transactional(block: () -> T) { //:R
    try {

    } catch (e: Throwable) {

    }
}