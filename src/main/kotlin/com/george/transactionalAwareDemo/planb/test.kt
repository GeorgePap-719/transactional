package com.george.transactionalAwareDemo.planb

fun test() {
    val list = listOf<String>()
}

open class TransactionalList<T>(private val array: Array<T>) : AbstractList<T>() {
    override val size: Int get() = array.size

    override operator fun get(index: Int): T = array[index]
}