package com.george.transactionalAwareDemo

interface TransactionOperations {
    fun commit(context: TransactionContext2)

    fun rollback(context: TransactionContext2)
}