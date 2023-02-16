package com.george.transactionalAwareDemo

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <T, R> TransactionalScope.transactional(
    receiver: TransactionalAwareObject<T>,
    block: TransactionalScope.() -> TransactionalAction<T>
): R {
    var job: TransactionalJob<T>? = null
    try {
        val action = block(this)
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


