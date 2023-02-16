package com.george.transactionalAwareDemo

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <R> TransactionalScope.transactional(
    block: TransactionalScope.() -> R
): R {
    try {
        // block
    } catch (e: Throwable) {
        // rollback
        throw e
    }
    // commit()
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
        transactional {
            with(map) {
                this@transactionalScope.action { this.put("hi", "test") }
            }
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


