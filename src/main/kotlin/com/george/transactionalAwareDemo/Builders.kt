package com.george.transactionalAwareDemo

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <T, R> TransactionalScope.transactional(
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
    when (transactionChannel.tryReceive().getOrThrow()) {
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
fun <R> transactionalScope(
    context: TransactionContext = EmptyTransactionContext,
    block: TransactionalScope.() -> R
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


fun main() {
    val map = TransactionalConcurrentMap<String, String>()
        transactionalScope {
            transactional(map) {
              transactionalAction { put("hi", "hi23") }
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


