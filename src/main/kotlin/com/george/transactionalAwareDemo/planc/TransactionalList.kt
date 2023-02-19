package com.george.transactionalAwareDemo.planc

import java.util.concurrent.ConcurrentHashMap

fun main() {
    val emptyList = mutableListOf<String>()
    val list = TransactionalList(emptyList)
    transactional {
        getAndExecute(list) { transactionalAdd(this@transactional, 1, "") }

    }
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
class TransactionalContext {
    private val actions = ConcurrentHashMap<Key, Action<*, *>>()

    operator fun <T, R> get(key: Key): Action<T, R>? {
        @Suppress("UNCHECKED_CAST")
        return actions[key] as Action<T, R>?
    }

    operator fun <T, R> set(key: Key, action: Action<T, R>) {
        actions[key] = action
    }

    operator fun <T, R> plus(action: Action<T, R>): Key {
        val key = Key()
        actions[key] = action
        return key
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
        val action = transactionalContext.get<MutableList<T>, R>(key)
    }

    fun TransactionalScope.rollback(key: TransactionalContext.Key) {} // no need since it up to transactional {} now

    fun transactionalAdd(scope: TransactionalScope, index: Int, element: T) {
        scope.transactionalContext.plus<MutableList<T>, Unit> { add(index, element) }
    }

    fun transactionalRemoveAt(scope: TransactionalScope, index: Int) {
        scope.transactionalContext.plus<MutableList<T>, T> { removeAt(index) }
    }
}


class TransactionalScope {
    val transactionalContext: TransactionalContext = TransactionalContext()
}

/*
 * Not sure how this should behave
 */
fun <T> transactional(block: TransactionalScope.() -> T) { //:R
    val scope = TransactionalScope()
    try {

    } catch (e: Throwable) {

    }
}

fun <T> TransactionalScope.getAndExecute(
    txAwareObject: TransactionalList<T>,
    block: TransactionalList<T>.() -> Any?
): T {

}