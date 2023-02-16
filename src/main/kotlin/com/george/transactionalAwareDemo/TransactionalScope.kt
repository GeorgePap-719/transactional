package com.george.transactionalAwareDemo

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

interface TransactionalScope {
    val transactionContext: TransactionContext

    val transactionChannel: SharedFlow<TransactionSignal>

    suspend fun commitTransaction()

    suspend fun rollbackTransaction()

    fun completeTransaction()
}

class TransactionalScopeImpl(override val transactionContext: TransactionContext) : TransactionalScope {
    private val channel = MutableSharedFlow<TransactionSignal>()

    override val transactionChannel: SharedFlow<TransactionSignal> get() = channel

    override suspend fun commitTransaction() {
        channel.emit(Commit)
    }

    override suspend fun rollbackTransaction() {
        channel.emit(RolledBack)
    }

    override fun completeTransaction() {
        channel
    }
}

sealed class TransactionSignal
object Commit : TransactionSignal()
object RolledBack : TransactionSignal()