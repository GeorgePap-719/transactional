package com.george.transactionalAwareDemo

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <T, R> TransactionalScope.transactional(
    receiver: TransactionalAwareObject<T>,
    block: TransactionalScope.() -> TransactionalAction<T>
): R {
    var job: TransactionalJob<T>?
    try {
        val action = block(this)
        job = TransactionalJob(action)
        receiver.addTransaction(job)
        // block
    } catch (e: Throwable) {
        // rollback
        receiver.rollback(job.key)
        throw e
    }
    // commit() & retrieve result
    val commit = receiver.commit(job.key)
    if (commit is TransactionState && commit == ROLLEDBACK) {
        error("transaction has rolled back")
    } else {
        @Suppress("UNCHECKED_CAST")
        return commit as R
    }
}

@OptIn(ExperimentalContracts::class)
fun <R> transactionalScope(
    context: TransactionContext = EmptyTransactionContext,
    block: TransactionalScope.() -> R
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val scope = TransactionalScopeImpl(context)
    try {
        block(scope)
    } catch (e: Throwable) {
        // signal rollback
    }
    // signal commit
}


fun main() {
    val map = TransactionalConcurrentMap<String, String>()
    transactionalScope {
        transactional(map) {
            map.action { this.put() }
        }
    }


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


