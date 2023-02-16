package com.george.transactionalAwareDemo

import java.util.concurrent.ConcurrentHashMap

fun <R> transactional(
    context: TransactionContext, // todo empty
    block: TransactionScope.() -> R
): R {
    try {
        // block
    } catch (e: Throwable) {
        // rollback
        throw e
    }
    // commit()
}

class ConcurrentHashMapTransactionalAction<K, V>(
    override val value: ConcurrentHashMap<K, V>.() -> Any?
) : TransactionalAction<ConcurrentHashMap<K, V>>


fun main() {
    val map = TransactionalConcurrentMap<String, String>()


    // goal
    /*
     * transactional {
     * .. transactionalAware code here
     * ..
     *  }
     *
     * commit if everything is ok else rollback
     *
     */

}


