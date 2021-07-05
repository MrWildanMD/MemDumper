package com.dumper.memdumper

import android.os.Handler
import android.os.Looper
import java.io.*
import java.nio.ByteBuffer

object Tools {
    private const val TAG = "TOOLS : "
    fun dumpFile(native:String,pkg: String, file: String, endAddr: Long = 0L): String {
        var log = ""
        try {
            log += "Begin Dumping For $file\n"
            val mem = Memory(pkg)
            getProcessID(mem)
            log += "PID : ${mem.pid}\n"
            if (mem.pid > 1 && mem.sAddress < 1L) {
                parseMap(mem, file)
                if (endAddr == 0L)
                    parseMapEnd(mem, file)
                else
                    mem.eAddress = endAddr
                mem.size = mem.eAddress - mem.sAddress
                log += "Start Address : ${longToHex(mem.sAddress)}\n"
                log += "End Address : ${longToHex(mem.eAddress)}\n"
                log += "Size Memory : ${mem.size}\n"
                if (mem.sAddress > 1L) {
                    val pathOut = File("/sdcard/$file")
                    RandomAccessFile("/proc/${mem.pid}/mem", "r").use { mems ->
                        mems.channel.use {
                            log += "Dumping...\n"
                            val buff: ByteBuffer =
                                    ByteBuffer.allocate(mem.size.toInt())
                            it.read(buff, mem.sAddress)
                            FileOutputStream(pathOut).use { out ->
                                out.write(buff.array())
                                out.close()
                            }
                            it.close()
                            Handler(Looper.getMainLooper()).postDelayed({
                                /* Create an Intent that will start the Menu-Activity. */
                                log += "Done. Saved at /sdcard/\n\n"
                            }, 10000)
                        }
                    }
                }
            }
        }catch (e:Exception){
            log += "$e\n"
        }
        return log
    }

    fun longToHex(ling: Long): String {
        return Integer.toHexString(ling.toInt())
    }

    private fun parseMap(nmax: Memory, lib_name: String) {
        val fil = File("/proc/${nmax.pid}/maps")
        if (fil.exists()) {
            fil.useLines { liness ->
                val start = liness.find { it.contains(lib_name) }
                if (start?.isBlank() == false && nmax.sAddress == 0L) {
                    val regex = "\\p{XDigit}+".toRegex()
                    val result = regex.find(start)?.value!!
                    nmax.sAddress = result.toLong(16)
                }
            }
        } else {
            throw FileNotFoundException("FAILED OPEN DIRECTORY : ${fil.path}")
        }
    }

    private fun parseMapEnd(nmax: Memory, lib_name: String) {
        val fil = File("/proc/${nmax.pid}/maps")
        if (fil.exists()) {
            fil.useLines { liness ->
                val end = liness.findLast { it.contains(lib_name) }
                if (end?.isBlank() == false && nmax.eAddress == 0L) {
                    val regex = "\\p{XDigit}+-\\p{XDigit}+".toRegex()
                    val result = regex.find(end)?.value!!.split("-")
                    nmax.eAddress = result[1].toLong(16)
                }
            }
        } else {
            throw FileNotFoundException("FAILED OPEN DIRECTORY : ${fil.path}")
        }
    }

    private fun getProcessID(nmax: Memory) {
        val process = Runtime.getRuntime().exec(arrayOf("pidof", nmax.pkg))
        val reader = BufferedReader(
                InputStreamReader(process.inputStream)
        )
        val buff = reader.readLine()
        reader.close()
        process.waitFor()
        process.destroy()
        nmax.pid = if (buff != null && buff.isNotEmpty()) buff.toInt() else 0
    }
}