package com.george.transactionalAwareDemo

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

interface TransactionalScope {
    val transactionContext: TransactionContext

    val transactionChannel: ReceiveChannel<TransactionSignal>

    fun commitTransaction()

    fun rollbackTransaction()

    fun completeTransaction()
}

class TransactionalScopeImpl(override val transactionContext: TransactionContext) : TransactionalScope {
    private val channel = Channel<TransactionSignal>()

    override val transactionChannel: ReceiveChannel<TransactionSignal> get() = channel

    override fun commitTransaction() {
        channel.trySend(Commit)
    }

    override fun rollbackTransaction() {
        channel.trySend(RolledBack)
    }

    override fun completeTransaction() {
        channel.close()
    }
}

sealed class TransactionSignal
object Commit : TransactionSignal()
object RolledBack : TransactionSignal()