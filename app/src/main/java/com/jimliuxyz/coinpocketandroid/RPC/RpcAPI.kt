package com.jimliuxyz.coinpocketandroid.RPC

import android.content.Context
import com.jimliuxyz.coinpocketandroid.MyApplication
import com.jimliuxyz.coinpocketandroid.R
import com.jimliuxyz.maprunner.utils.doNetwork
import com.jimliuxyz.maprunner.utils.getPref
import com.jimliuxyz.maprunner.utils.setPref
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import java.util.*

private typealias RpcHandler = (jsonrpc: JsonRPC) -> Unit

class RpcAPI private constructor(val context: Context) {
    companion object {
        val ERR_LOGIN_FAILED = 100
        val ERR_API_NOT_READY = 101
        val ERR_JWT_EXPIRED = 102

        var LOGED_NONE = 0  //not even connected
        var LOGED_OKAY = 1
        var LOGED_FAIL = -1


        private var _instance = RpcAPI(MyApplication.instance)
        fun getInstance(): RpcAPI {
            return _instance
        }
    }

    private var mSocket: Socket? = null
    private var mToken: String? = null
    private var logged = LOGED_NONE

    init {
        mToken = context.getPref(R.string.pref_jwt_token, "")
    }

    interface EventListener {
        fun loginStateChanged(logged: Int)
        fun takeReceipt(rpc: JsonRPC)
    }

    private var eventListener: EventListener? = null
    fun setEventListener(listener: EventListener?) {
        eventListener = listener
    }

    private fun changeLoginState(logged: Int) {
        this.logged = logged
        eventListener?.loginStateChanged(logged)
    }

    fun getLoginState(): Int {
        return logged
    }

    private var service: SocketService? = null

    private inner class SocketService(val name: String?, val pwd: String?, var token: String?) {

//        val HOST = "ec2-18-221-14-16.us-east-2.compute.amazonaws.com"
//        val HOST = "192.168.1.101"
//        val PORT = "8081"

        var firsttry = true
        var everlogged = false
        lateinit var socket: Socket
        var timer = Timer()

        fun conn(result: RpcHandler) {

            val ip = context.getPref(R.string.pref_server_ip, "")
            val port = context.getPref(R.string.pref_server_port, "")
            println("connect...$ip:$port")

            val opts = IO.Options()
            opts.transports = arrayOf(WebSocket.NAME)

            //seeu: do not use 'localhost' or '127.0.0.1' to reach your local server (it'll loop inside android device)
            socket = IO.socket("http://$ip:$port", opts)
            socket.apply {

                on(Socket.EVENT_CONNECT, Emitter.Listener {
                    println("EVENT_CONNECT" + it)

                    var jrpc: JsonRPC
                    if (!firsttry || name.isNullOrBlank())
                        jrpc = JsonRPC.createMethod("login", mapOf("token" to token))
                    else
                        jrpc = JsonRPC.createMethod("login", mapOf("name" to name, "pwd" to pwd))

                    //bind result handler
                    pushResultHandler(jrpc) { rpc ->

                        Thread.sleep(2000)
                        if (rpc.isError()) {
                            if (rpc.getErrCode() == ERR_JWT_EXPIRED || rpc.getErrCode() == ERR_LOGIN_FAILED) {
                                setToken(null)
                                changeLoginState(LOGED_FAIL)
                            }
                            if (firsttry) {
                                socket.disconnect()
                            }
                        } else if (rpc.isResult()) {
                            everlogged = true
                            setToken(rpc.result!!.getString("token"))
                            token = mToken
                            changeLoginState(LOGED_OKAY)
                            watchEvent()
                        }
                        if (firsttry)
                            result(rpc)
                        firsttry = false
                    }
                    socket.emit("jsonrpc", jrpc.toJsonString())
                })

                var onSocketException = { errmsg: String ->
                    println(errmsg)

                    if (firsttry) {
                        firsttry = false
                        result(JsonRPC.createError(0, errmsg))
                        socket.disconnect()
                        socket.close()
                    } else if (everlogged)
                        notifyErr(errmsg)
                    false
                }

                on(Socket.EVENT_DISCONNECT, Emitter.Listener {
                    onSocketException("連線中斷")
                })

                on(Socket.EVENT_ERROR, Emitter.Listener {
                    onSocketException("連線錯誤1")
                })

                on(Socket.EVENT_CONNECT_ERROR, Emitter.Listener {
                    onSocketException("連線錯誤2")
                })

                on(Socket.EVENT_CONNECT_TIMEOUT, Emitter.Listener {
                    onSocketException("連線逾時")
                })

                on("jsonrpc", Emitter.Listener {
                    val json = it[0] as String
                    println("receive " + json)

                    var rpc = JsonRPC.fromJsonString(json)
                    if (rpc.isMethod()) {
                        when (rpc.method!!) {
                            "takeReceipt" -> {
                                eventListener?.takeReceipt(rpc)
                            }
                        }

                    } else if (rpc.isResult() || rpc.isError()) {
                        popResultHandler(rpc)?.let { handler ->
                            handler(rpc)
                        }
                    }
                })
                connect()
            }

            timer.schedule(object : TimerTask() {
                override fun run() {
//                    println("TimerTask...${timemap.size}")
                    var currtime = Date().time
                    synchronized(this) {
                        var it = timemap.iterator()
                        for ((id, time) in it) {
                            if (currtime - time > 5000) {
                                it.remove()
                                map.remove(id)?.let { handler ->
                                    var rpc = JsonRPC.createError(0, "操作逾時")
                                    handler(rpc)
                                }
                            }
                        }
                    }
                }
            }, 0, 1000)
        }

        private val map = LinkedHashMap<String, RpcHandler>()
        private val timemap = LinkedHashMap<String, Long>()
        private fun pushResultHandler(rpc: JsonRPC, func: RpcHandler) {
            synchronized(this) {
                map.put(rpc.id, func)
                timemap.put(rpc.id, Date().time)
            }
        }

        private fun popResultHandler(rpc: JsonRPC): RpcHandler? {
            synchronized(this) {
                timemap.remove(rpc.id)
                return map.remove(rpc.id)
            }
        }

        private fun notifyErr(msg: String) {
            var jrpc = JsonRPC.createError(0, msg)

            synchronized(this) {
                for ((id, handler) in map) {
                    handler(jrpc)
                }
                map.clear()
            }
        }

        private fun watchEvent() {
            emit(JsonRPC.createMethod("watchEvent"))
        }

        fun disconnect() {
            socket.disconnect()
            socket.close()
            timer.cancel()
        }

        fun emit(jsonrpc: JsonRPC, handler: RpcHandler? = null) {
            println("send " + jsonrpc.toJsonString())

            handler?.also {
                pushResultHandler(jsonrpc, handler)
            }
            socket.emit("jsonrpc", jsonrpc.toJsonString())
        }
    }

    private fun setToken(token: String?) {
        mToken = token
        context.setPref(R.string.pref_jwt_token, token ?: "")
    }

    fun connWithToken(handler: RpcHandler) {
        var service = SocketService(null, null, mToken)

        service.conn() { rpc ->
            if (rpc.isResult()) {
                this.service = service
            } else{
                println("do disconnect2")
                service.disconnect()
            }
            handler(rpc)
        }
    }

    fun conn(name: String?, pwd: String?, handler: RpcHandler) {
        var service = SocketService(name, pwd, null)

        service.conn() { rpc ->
            if (rpc.isResult()) {
                this.service = service
            } else{
                println("do disconnect1")
                service.disconnect()
            }
            handler(rpc)
        }
    }

    fun disconn() {
        service?.let {
            println("do disconnect3")
            it.disconnect()
            this.service = null
        }
    }

    val ERR_NOT_LOGIN_SERVICE = "尚未登入伺服器"

    fun getBalance(handler: RpcHandler) {
        service?.apply {
            val jrpc = JsonRPC.createMethod("balance")
            emit(jrpc, handler)
        } ?: doNetwork {
            handler(JsonRPC.createError(0, ERR_NOT_LOGIN_SERVICE))
        }
    }

    fun deposit(type: Int, amount: Long, handler: RpcHandler) {
        service?.apply {
            val jrpc = JsonRPC.createMethod("deposit",
                    mapOf<String, Any?>("type" to type, "amount" to amount))
            emit(jrpc, handler)
        } ?: doNetwork {
            handler(JsonRPC.createError(0, ERR_NOT_LOGIN_SERVICE))
        }
    }

    fun withdraw(type: Int, amount: Long, handler: RpcHandler) {
        service?.apply {
            val jrpc = JsonRPC.createMethod("withdraw",
                    mapOf<String, Any?>("type" to type, "amount" to amount))
            emit(jrpc, handler)
        } ?: doNetwork {
            handler(JsonRPC.createError(0, ERR_NOT_LOGIN_SERVICE))
        }
    }

    fun transfer(type: Int, amount: Long, receiver: String, handler: RpcHandler) {
        service?.apply {
            val jrpc = JsonRPC.createMethod("transfer",
                    mapOf<String, Any?>("type" to type, "amount" to amount, "receiver" to receiver))
            emit(jrpc, handler)
        } ?: doNetwork {
            handler(JsonRPC.createError(0, ERR_NOT_LOGIN_SERVICE))
        }
    }

    fun listReceipt(handler: RpcHandler) {
        service?.apply {
            val jrpc = JsonRPC.createMethod("listReceipt")
            emit(jrpc, handler)
        } ?: doNetwork {
            handler(JsonRPC.createError(0, ERR_NOT_LOGIN_SERVICE))
        }
    }

}

