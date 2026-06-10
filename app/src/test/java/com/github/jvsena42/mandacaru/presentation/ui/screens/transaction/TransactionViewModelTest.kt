package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.Result as BlockchainInfo
import com.github.jvsena42.mandacaru.domain.model.florestaRPC.response.SendRawTransactionResponse
import com.github.jvsena42.mandacaru.domain.scan.DecodedTransaction
import com.github.jvsena42.mandacaru.domain.scan.PayloadType
import com.github.jvsena42.mandacaru.domain.scan.QrTransactionScanner
import com.github.jvsena42.mandacaru.domain.scan.ScanState
import com.github.jvsena42.mandacaru.domain.scan.ScanTransport
import com.github.jvsena42.mandacaru.domain.scan.TransactionDecodeException
import com.github.jvsena42.mandacaru.domain.scan.TransactionDecoder
import com.github.jvsena42.mandacaru.fakes.FakeFlorestaRpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var rpc: FakeFlorestaRpc
    private lateinit var scanner: FakeQrScanner
    private lateinit var decoder: FakeTransactionDecoder
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rpc = FakeFlorestaRpc()
        scanner = FakeQrScanner()
        decoder = FakeTransactionDecoder()
        viewModel = TransactionViewModel(rpc, scanner, decoder, dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- broadcast validation (the VM-side guard before hitting the node) ---

    @Test
    fun `non-hex broadcast input is rejected without calling the node`() {
        viewModel.onAction(TransactionAction.OnRawTxChanged("zzz-not-hex"))
        viewModel.onAction(TransactionAction.OnClickBroadcast)

        assertEquals("Invalid hex format", viewModel.uiState.value.errorMessage)
        assertTrue(rpc.sentRawTransactions.isEmpty())
    }

    @Test
    fun `empty broadcast input does nothing`() {
        viewModel.onAction(TransactionAction.OnRawTxChanged(""))
        viewModel.onAction(TransactionAction.OnClickBroadcast)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(rpc.sentRawTransactions.isEmpty())
        assertFalse(viewModel.uiState.value.isBroadcasting)
    }

    @Test
    fun `valid hex is broadcast and the txid is surfaced`() {
        rpc.sendRawTransactionResult =
            Result.success(SendRawTransactionResponse(id = 1, jsonrpc = "2.0", result = "the-txid"))

        viewModel.onAction(TransactionAction.OnRawTxChanged("deadbeef"))
        viewModel.onAction(TransactionAction.OnClickBroadcast)
        assertTrue("isBroadcasting should flip synchronously", viewModel.uiState.value.isBroadcasting)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("deadbeef"), rpc.sentRawTransactions)
        assertEquals("the-txid", viewModel.uiState.value.broadcastResult)
        assertFalse(viewModel.uiState.value.isBroadcasting)
    }

    @Test
    fun `broadcast failure surfaces the error`() {
        rpc.sendRawTransactionResult = Result.failure(RuntimeException("node rejected"))

        viewModel.onAction(TransactionAction.OnRawTxChanged("deadbeef"))
        viewModel.onAction(TransactionAction.OnClickBroadcast)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("node rejected", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isBroadcasting)
    }

    // --- search validation & sync-aware messaging ---

    @Test
    fun `malformed txid search is rejected without calling the node`() {
        viewModel.onAction(TransactionAction.OnSearchChanged("not-a-txid"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Invalid transaction ID format", viewModel.uiState.value.errorMessage)
        assertTrue(rpc.getTransactionCalls.isEmpty())
    }

    @Test
    fun `not-found during IBD reports a syncing message`() {
        val txid = "a".repeat(64)
        rpc.transactionResult = Result.failure(RuntimeException("Transaction not found"))
        rpc.blockchainInfoResults = listOf(Result.success(blockchainInfo(ibd = true, progress = 0.5f)))

        viewModel.onAction(TransactionAction.OnSearchChanged(txid))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(txid), rpc.getTransactionCalls)
        assertTrue(viewModel.uiState.value.errorMessage.contains("still syncing"))
    }

    // --- scan state machine ---

    @Test
    fun `completed scan decodes and opens the confirmation`() {
        scanner.nextState = ScanState.Complete(byteArrayOf(1, 2, 3), ScanTransport.SINGLE)
        decoder.result = Result.success(decodedTransaction())

        viewModel.onAction(TransactionAction.OnQrFrameScanned("frame"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("the-txid", viewModel.uiState.value.decodedTx?.txid)
        assertFalse(viewModel.uiState.value.isScannerVisible)
        assertFalse(viewModel.uiState.value.isDecoding)
    }

    @Test
    fun `failed decode surfaces a scan error`() {
        scanner.nextState = ScanState.Complete(byteArrayOf(1, 2, 3), ScanTransport.SINGLE)
        decoder.result = Result.failure(TransactionDecodeException("isn't fully signed"))

        viewModel.onAction(TransactionAction.OnQrFrameScanned("frame"))
        // runCurrent executes the decode without advancing into the 10s error cooldown.
        dispatcher.scheduler.runCurrent()

        assertEquals("isn't fully signed", viewModel.uiState.value.scanError)
    }

    @Test
    fun `scanner error state is shown`() {
        scanner.nextState = ScanState.Error("unreadable QR")

        viewModel.onAction(TransactionAction.OnQrFrameScanned("frame"))

        assertEquals("unreadable QR", viewModel.uiState.value.scanError)
    }

    // --- helpers / fakes ---

    private fun blockchainInfo(ibd: Boolean, progress: Float): GetBlockchainInfoResponse =
        GetBlockchainInfoResponse(
            id = 1,
            jsonrpc = "2.0",
            result = BlockchainInfo(
                bestBlock = "00",
                chain = "bitcoin",
                difficulty = 1f,
                height = 100,
                ibd = ibd,
                latestBlockTime = 0,
                latestWork = "00",
                leafCount = 0,
                progress = progress,
                rootCount = 0,
                rootHashes = emptyList(),
                validated = 50,
            ),
        )

    private fun decodedTransaction(): DecodedTransaction = DecodedTransaction(
        rawHex = "deadbeef",
        txid = "the-txid",
        inputCount = 1,
        outputCount = 1,
        totalOutSats = 1_000L,
        feeSats = null,
        feeRateSatVb = null,
        vsize = 110L,
        weight = 440L,
        payloadType = PayloadType.TRANSACTION,
        transport = ScanTransport.SINGLE,
    )

    private class FakeQrScanner : QrTransactionScanner {
        var nextState: ScanState = ScanState.Idle
        override fun ingest(raw: String): ScanState = nextState
        override fun reset() { nextState = ScanState.Idle }
    }

    private class FakeTransactionDecoder : TransactionDecoder {
        var result: kotlin.Result<DecodedTransaction> =
            Result.failure(TransactionDecodeException("not set"))

        override fun decode(payload: ByteArray, transport: ScanTransport) = result
    }
}
