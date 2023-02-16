package com.george.transactionalAwareDemo.planb

//typealias TransactionalAction<T, R> = T.() -> R

/*
 * Represents a single action.
 */
open class TransactionalAction<T, R>(val value: T.() -> R)

typealias Action<T, R> = T.() -> R

open class Key

abstract class TransactionalAwareObject<T> : TransactionalAwareObjectSupport {
    private val actionStore = hashMapOf<Key, TransactionalAction<*, *>>()

    fun <R> action(input: Action<T, R>): Key {
        val action = TransactionalAction(input)
        val newKey = Key()
        actionStore.plus(newKey to action)
        return newKey
    }

    final override fun <R> commit(key: Key): R {
        try {
            val action = actionStore[key]?.value ?: error("transaction is not retrievable")
            @Suppress("UNCHECKED_CAST")
            action as T.() -> R
            return commitSafely(action)
        } catch (e: Throwable) {
            rollback(key) // in case operation has an error, remove action.
            throw e
        }
    }

    final override fun rollback(key: Key) {
        actionStore.remove(key)
    }

    abstract fun <R> commitSafely(input: Action<T, R>): R
}

interface TransactionalAwareObjectSupport {
    fun <R> commit(key: Key): R
    fun rollback(key: Key)
}

open class TransactionalAwareList<T>(private val list: MutableList<T>) : TransactionalAwareObject<List<T>>() {
    override fun <R> commitSafely(input: Action<List<T>, R>): R = input(list)

    override fun toString(): String = list.toString()
}

fun <T, R> transactional(
    receiver: TransactionalAwareObject<T>,
    block: TransactionalAwareObject<T>.() -> R
): R {
    try {
        block(receiver)
    } catch (e: Throwable) {
        receiver.rollback()
    }
}

fun main() {
    val list = TransactionalAwareList(mutableListOf("hi"))
    transactional(list) {
      action { this.plus("hi2") }
    }

    println(list)
}