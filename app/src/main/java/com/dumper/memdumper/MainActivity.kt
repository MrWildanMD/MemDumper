package com.dumper.memdumper

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dumper.memdumper.databinding.ActivityMainBinding
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService


class MainActivity : AppCompatActivity(), Handler.Callback {
    lateinit var bind: ActivityMainBinding
    private val myMessenger = Messenger(Handler(Looper.getMainLooper(), this))
    var remoteMessenger: Messenger? = null
    private var serviceTestQueued = false
    private var conn: MSGConnection? = null
    private var Exec = ""

    companion object {
        const val TAG = "MemDumper"
    }

    private fun initRoot() {
        if (Shell.rootAccess()) {
            if (remoteMessenger == null) {
                serviceTestQueued = true
                val intent = Intent(this, RootServices::class.java)
                conn = MSGConnection()
                RootService.bind(intent, conn!!)
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        reqStorage()
        initRoot()
        Exec = applicationInfo.nativeLibraryDir

        with(bind) {
            setContentView(root)
            beginDump.setOnClickListener {
                if (bind.pkg.text != null) {
                    Exec = applicationInfo.nativeLibraryDir + " -p " + bind.pkg.text + "-l " + "-n " + libName.text.toString()
                    if (bind.rawDump.isChecked) {
                        Exec = applicationInfo.nativeLibraryDir + " -p " + bind.pkg.text + "-l " + "-r " + "-n " + libName.text.toString()
                    }

                    if (bind.fastDump.isChecked) {
                        Exec = applicationInfo.nativeLibraryDir + " -p " + bind.pkg.text + "-l " + "-f " + "-n " + libName.text.toString()
                    }
                    runNative(
                            if (libName.text.isNullOrBlank()) {
                                "libil2cpp.so"
                            } else {
                                libName.text.toString()
                            }
                    )
                } else {
                    console.text = "Put package name!"
                }
            }
            github.setOnClickListener {
                startActivity(
                        Intent(
                                ACTION_VIEW,
                                Uri.parse("https://github.com/MrWildanMD/MemDumper")
                        )
                )
            }
        }
    }

    private fun reqStorage() {
        val permission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    10
            )
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        val dump = msg.data.getString("result")
        consoleList.add(dump)
        return false
    }

    private fun runNative(file: String) {
        val pkg = bind.pkg.text.toString()
        if (Shell.rootAccess()) {
            dumpRoot(pkg, file)
        } else {
            consoleList.add(Tools.dumpFile(Exec,pkg, file))
        }
    }

    private fun dumpRoot(pkg: String, file: String) {
        val message: Message = Message.obtain(null, RootServices.MSG_GETINFO)
        message.data.putString("native",Exec)
        message.data.putString("pkg", pkg)
        message.data.putString("file_dump", file)
        message.replyTo = myMessenger
        try {
            remoteMessenger?.send(message)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote error", e)
        }
    }

    inner class MSGConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "service onServiceConnected")
            remoteMessenger = Messenger(service)
            if (serviceTestQueued) {
                serviceTestQueued = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "service onServiceDisconnected")
            remoteMessenger = null
        }
    }

    private var consoleList = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            bind.console.append(s)
            bind.console.append("\n")
            bind.sv.postDelayed({ bind.sv.fullScroll(ScrollView.FOCUS_DOWN) }, 10)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        conn?.let {
            RootService.unbind(it)
        }
    }
}