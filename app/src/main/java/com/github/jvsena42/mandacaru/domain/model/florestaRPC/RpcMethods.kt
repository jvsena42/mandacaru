package com.github.jvsena42.mandacaru.domain.model.florestaRPC

enum class RpcMethods(val method: String) {
    RESCAN("rescanblockchain"),
    GET_PEER_INFO("getpeerinfo"),
    STOP("stop"),
    GET_BLOCKCHAIN_INFO("getblockchaininfo"),
    LOAD_DESCRIPTOR("loaddescriptor"),
    GET_TRANSACTION("getrawtransaction"),
    ADD_NODE("addnode"),
    LIST_DESCRIPTORS("listdescriptors"),
    UPTIME("uptime"),
    GET_MEMORY_INFO("getmemoryinfo"),
    GET_BLOCK_HASH("getblockhash"),
    GET_BLOCK_HEADER("getblockheader"),
    GET_BEST_BLOCK_HASH("getbestblockhash"),
    GET_BLOCK_COUNT("getblockcount"),
    SEND_RAW_TRANSACTION("sendrawtransaction"),
    DISCONNECT_NODE("disconnectnode"),
    PING("ping"),
}
