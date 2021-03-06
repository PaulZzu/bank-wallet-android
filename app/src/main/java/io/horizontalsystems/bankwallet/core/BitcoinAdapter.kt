package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

class BitcoinAdapter(val words: List<String>, network: BitcoinKit.NetworkType) : IAdapter, BitcoinKit.Listener {

    private var bitcoinKit = BitcoinKit(words, network)
    private val satoshisInBitcoin = Math.pow(10.0, 8.0)

    override val balance: Double get() = bitcoinKit.balance / satoshisInBitcoin
    override val balanceSubject: PublishSubject<Double> = PublishSubject.create()

    val progressSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(0.0)

    override var state: AdapterState = AdapterState.Syncing(progressSubject)
        set(value) {
            field = value
            stateSubject.onNext(value)
        }
    override val stateSubject: PublishSubject<AdapterState> = PublishSubject.create()

    override val confirmationsThreshold: Int = 6
    override val lastBlockHeight: Int get() = bitcoinKit.lastBlockHeight
    override val lastBlockHeightSubject: PublishSubject<Int> = PublishSubject.create()

    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val debugInfo: String = ""

    override val receiveAddress: String get() = bitcoinKit.receiveAddress()

    override fun start() {
        bitcoinKit.listener = this
        bitcoinKit.start()
    }

    override fun refresh() {
    }

    override fun clear() {
        bitcoinKit.clear()
    }

    override fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))?) {
        try {
            bitcoinKit.send(address, (value * satoshisInBitcoin).toInt())
            completion?.invoke(null)
        } catch (ex: Exception) {
            completion?.invoke(ex)
        }
    }

    override fun fee(value: Double, senderPay: Boolean): Double {
        return bitcoinKit.fee(value = (value * satoshisInBitcoin).toInt(), senderPay = senderPay) / satoshisInBitcoin
    }

    override fun validate(address: String) {
        bitcoinKit.validateAddress(address)
    }

    //
    // BitcoinKit Listener implementations
    //
    override fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long) {
        balanceSubject.onNext(balance / satoshisInBitcoin)
    }

    override fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, lastBlockInfo: BlockInfo) {
        lastBlockHeightSubject.onNext(lastBlockInfo.height)
    }

    override fun onProgressUpdate(bitcoinKit: BitcoinKit, progress: Double) {
        when (state) {
            is AdapterState.Synced -> {
                if (progress < 1) {
                    state = AdapterState.Syncing(progressSubject)
                }

            }
            is AdapterState.Syncing -> {
                if (progress == 1.0) {
                    state = AdapterState.Synced()
                }
            }
        }

        progressSubject.onNext(progress)
    }

    override fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }
        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)

    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val record = TransactionRecord()

        record.transactionHash = transaction.transactionHash
        record.blockHeight = transaction.blockHeight?.toLong() ?: 0
        record.amount = transaction.amount / satoshisInBitcoin
        record.timestamp = transaction.timestamp ?: Date().time / 1000

        record.from = transaction.from.map {
            val address = TransactionAddress()
            address.address = it.address
            address.mine = it.mine
            address
        }

        record.to = transaction.to.map {
            val address = TransactionAddress()
            address.address = it.address
            address.mine = it.mine
            address
        }

        return record
    }
}
