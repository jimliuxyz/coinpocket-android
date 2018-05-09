package com.jimliuxyz.coinpocketandroid

import com.google.gson.GsonBuilder
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket


class RpcAPI private constructor() {
    companion object {
        private lateinit var _instance: RpcAPI
        fun getInstance(): RpcAPI {
            _instance = RpcAPI()
            return _instance
        }
    }

    lateinit var socket: Socket

    init {
            createConnection()
    }

    private fun createConnection() {
        println("---------")

//        IO.setDefaultHostnameVerifier(object : HostnameVerifier {
//            override fun verify(hostname: String, session: SSLSession): Boolean {
//                //TODO: Make this more restrictive
//                return true
//            }
//        })

        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)

        //seeu: do not use 'localhost' or '127.0.0.1' to reach your local server (it'll loop inside android device)
        socket = IO.socket("http://192.168.1.101:8081", opts)

        socket.on(Socket.EVENT_CONNECT, io.socket.emitter.Emitter.Listener {
//            socket.disconnect()
            println("EVENT_CONNECT" + it)
            login()
        })
        socket.on(Socket.EVENT_DISCONNECT, io.socket.emitter.Emitter.Listener {
            println("EVENT_DISCONNECT")
        })

        socket.on(Socket.EVENT_ERROR, io.socket.emitter.Emitter.Listener {
            println("EVENT_ERROR")
        })

        socket.on(Socket.EVENT_CONNECT_ERROR, io.socket.emitter.Emitter.Listener {
            println("EVENT_CONNECT_ERROR"+it[0])
        })

        socket.on(Socket.EVENT_CONNECT_TIMEOUT, io.socket.emitter.Emitter.Listener {
            println("EVENT_CONNECT_TIMEOUT")
        })

        socket.on("jsonrpc", io.socket.emitter.Emitter.Listener {
            println("got !!!" + it[0])
        })
        socket.connect()
        println("---------")

    }

    class JsonRPC(var method: String, var params: Map<String, String>) {
        var jsonrpc = "2.0"
        var id = Math.round(Math.random() * Long.MAX_VALUE)
    }


    private fun login() {

        var jrpc = JsonRPC("login", mapOf("name" to "jim6", "pwd" to ""))

        var gson = GsonBuilder().create()

        var jsonStr = gson.toJson(jrpc)

        println("jsonrpc ${jsonStr}")

        socket.emit("jsonrpc", jsonStr)
    }


}