package com.github.jvsena42.floresta_node.domain.model.florestaRPC

enum class RpcMethods(val method: String) {
    RESCAN("rescan"),
    GET_PEER_INFO("getpeerinfo"),
    STOP("stop"),
    GET_BLOCKCHAIN_INFO("getblockchaininfo"),
    LOAD_DESCRIPTOR("loaddescriptor"),
    GET_TRANSACTION("gettransaction"),
    ADD_NODE("addnode"),
    LIST_DESCRIPTORS("listdescriptors"),
}