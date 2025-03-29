package com.github.jvsena42.floresta_node.data

import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.AddNodeResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetBlockchainInfoResponse
import com.github.jvsena42.floresta_node.domain.model.florestaRPC.response.GetPeerInfoResponse
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
 * @return A `Result` containing a `JSONObject` with the following fields:
 * - blockhash: The hash of the block containing this transaction, if it is in a block
 * - blocktime: Time when the block containing this transaction was mined, if it is in a block
 * - confirmations: The amount of confirmations this transaction has, if it is in a block
 * - hash: The hash of this transaction, a.k.a wtxid
 * - hex: The hex-encoded transaction
 * - in_active_chain: Whether this transaction is in the active chain
 * - locktime: The locktime value of this transaction
 * - size: The size of this transaction in bytes
 * - time: The time when this transaction was mined, if it is in a block
 * - txid: The id of this transaction. Only for witness transactions, this is different from the wtxid
 * - version: The version of this transaction
 * - vin: A vector of inputs
 * - vout: A vector of outputs
 * - vsize: The size of this transaction, in virtual bytes
 * - weight: The weight of this transaction
 */
suspend fun getTransaction(txId: String): Flow<Result<JSONObject>>
suspend fun listDescriptors(): Flow<Result<JSONObject>>

/**
 * Adds a new node to our list of peers. This will make our node try to connect to this peer.
 * @param node A network address with the format ip[:port]
 */
suspend fun addNode(node: String): Flow<Result<AddNodeResponse>>
}