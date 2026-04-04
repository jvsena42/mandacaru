package com.github.jvsena42.floresta_node.data

import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockCountResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockHashResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockHeaderResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetMemoryInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetPeerInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.SendRawTransactionResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetTransactionResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.UptimeResponse
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

interface FlorestaRpc {
    /**
     * Returns some useful data about the current active network.
     * @return A `Result` containing `GetBlockchainInfoResponse` with the following fields:
     * - best_block: The best block we have headers for
     * - chain: The name of the current active network (e.g., bitcoin, testnet, regtest)
     * - difficulty: Current network difficulty
     * - height: The height of the best block we have headers for
     * - ibd: Whether we are currently in initial block download
     * - latest_block_time: The time in which the latest block was mined
     * - latest_work: The work of the latest block (e.g., the amount of hashes needed to mine it, on average)
     * - leaf_count: The amount of leaves in our current forest state
     * - progress: The percentage of blocks we have validated so far
     * - root_count: The amount of roots in our current forest state
     * - root_hashes: The hashes of the roots in our current forest state
     * - validated: The amount of blocks we have validated so far
     * - verification_progress: The percentage of blocks we have verified so far
     */
    suspend fun getBlockchainInfo(): Flow<Result<GetBlockchainInfoResponse>>

    /**
     * Tells our node to rescan blocks.
     * @return A `Result` containing a `JSONObject` with the following field:
     * - success: Whether we successfully started rescanning
     */
    suspend fun rescan(): Flow<Result<JSONObject>>

    /**
     * Tells our wallet to follow this new descriptor.
     * @param descriptor An output descriptor
     * @return A `Result` containing a `JSONObject` with the following field:
     * - status: Whether we succeed loading this descriptor
     */
    suspend fun loadDescriptor(descriptor: String): Flow<Result<JSONObject>>

    /**
     * Gracefully stops the node.
     * @return A `Result` containing a `JSONObject` with the following field:
     * - success: Whether we successfully stopped the node
     */
    suspend fun stop(): Flow<Result<JSONObject>>

    /**
     * Returns a list of peers connected to our node, and some useful information about them.
     * @return A `Result` containing `GetPeerInfoResponse` with the following fields:
     * - peers: A vector of peers connected to our node
     * - address: This peer's network address
     * - services: The services this peer announces as supported
     * - user_agent: A string representing this peer's software
     */
    suspend fun getPeerInfo(): Flow<Result<GetPeerInfoResponse>>

    /**
     * Returns a transaction data, given its id.
     * @param txId The id of a transaction
     * @return A `Result` containing `GetTransactionResponse` with transaction details
     */
    suspend fun getTransaction(txId: String): Flow<Result<GetTransactionResponse>>

    suspend fun listDescriptors(): Flow<Result<JSONObject>>

    /**
     * Adds a new node to our list of peers. This will make our node try to connect to this peer.
     * @param node A network address with the format ip[:port]
     */
    suspend fun addNode(node: String): Flow<Result<AddNodeResponse>>

    /**
     * Returns the number of seconds the daemon has been running.
     * @return A `Result` containing `UptimeResponse` with the uptime in seconds
     */
    suspend fun getUptime(): Flow<Result<UptimeResponse>>

    /**
     * Returns memory usage information from the daemon.
     * @return A `Result` containing `GetMemoryInfoResponse` with memory stats
     */
    suspend fun getMemoryInfo(): Flow<Result<GetMemoryInfoResponse>>

    /**
     * Returns the block hash at the given height.
     * @param height The block height
     * @return A `Result` containing `GetBlockHashResponse` with the block hash as a hex string
     */
    suspend fun getBlockHash(height: Int): Flow<Result<GetBlockHashResponse>>

    /**
     * Returns block header data for a given block hash.
     * @param blockHash The block hash as a hex string
     * @return A `Result` containing `GetBlockHeaderResponse` with header fields
     */
    suspend fun getBlockHeader(blockHash: String): Flow<Result<GetBlockHeaderResponse>>

    /**
     * Returns the hash of the best (most recent) block.
     * @return A `Result` containing `GetBlockHashResponse` with the best block hash
     */
    suspend fun getBestBlockHash(): Flow<Result<GetBlockHashResponse>>

    /**
     * Returns the number of blocks in the longest chain.
     * @return A `Result` containing `GetBlockCountResponse` with the block count
     */
    suspend fun getBlockCount(): Flow<Result<GetBlockCountResponse>>

    /**
     * Broadcasts a raw transaction to the network.
     * @param txHex The raw transaction as a hex string
     * @return A `Result` containing `SendRawTransactionResponse` with the transaction ID
     */
    suspend fun sendRawTransaction(txHex: String): Flow<Result<SendRawTransactionResponse>>
}