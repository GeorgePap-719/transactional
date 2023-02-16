package com.george.transactionalAwareDemo

import kotlinx.coroutines.flow.first
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

suspend fun <T, R> TransactionalScope.transactional(
    receiver: TransactionalAwareObject<T>,
    block: TransactionalAwareObject<T>.() -> TransactionalAction<T, R>
): R {
    var job: TransactionalJob<T, R>? = null
    try {
        val action = block(receiver)
        job = TransactionalJob(action)
        receiver.addTransaction(job)
        // block
    } catch (e: Throwable) {
        // rollback
        if (job != null) receiver.rollback(job.key)
        throw e
    }
    // wait for signal
    when (transactionChannel.first()) {
        Commit -> {
            // commit() & retrieve result
            val commit = receiver.commit(job.key)
            if (commit is TransactionState && commit == ROLLEDBACK) {
                error("transaction has rolled back")
            } else {
                @Suppress("UNCHECKED_CAST")
                return commit as R
            }
        }

        RolledBack -> {
            receiver.rollback(job.key)
            @Suppress("UNCHECKED_CAST")
            return ROLLEDBACK as R // does not matter what we return here, since scope will throw exception after
            // all rollbacks are completed
        }
    }
}

@OptIn(ExperimentalContracts::class)
suspend fun <R> transactionalScope(
    context: TransactionContext = EmptyTransactionContext,
    block: suspend TransactionalScope.() -> R
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val scope = TransactionalScopeImpl(context)
    var throwable: Throwable? = null
    try {
        try {
            block(scope)
        } catch (e: Throwable) {
            // signal rollback
            scope.rollbackTransaction()
            throwable = e
        }
        scope.commitTransaction() // signal commit
    } finally {
        scope.completeTransaction() // close channel
        if (throwable != null) throw throwable
    }
}


suspend fun main() {
    val map = TransactionalConcurrentMap<String, String>()
    transactionalScope {
        val result = transactional(map) {
            transactionalAction { put("hi", "hi23") }
        }
        println("result:$result")
    }
    println("lets print map:$map")
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


