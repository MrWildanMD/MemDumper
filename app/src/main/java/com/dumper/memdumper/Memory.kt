package com.dumper.memdumper

data class Memory(val pkg: String) {
    var pid: Int = 0
    var sAddress: Long = 0L
    var eAddress: Long = 0L
    var size:Long = 0L
}