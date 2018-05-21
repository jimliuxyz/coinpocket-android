package com.jimliuxyz.coinpocketandroid.RPC

import com.google.gson.GsonBuilder

class JsonRPC {
    val jsonrpc = "2.0"
    var id = ""

    var method: String? = null
    var params: Map<String, Any?>? = null

    var result: Map<String, Any?>? = null
    var error: Map<String, Any?>? = null


    companion object {
        val ERRCODE = "code"
        val ERRMSG = "message"

        fun createMethod(method: String, params: Map<String, Any?>?= mapOf()): JsonRPC {
            val jrpc = JsonRPC()
            jrpc.method = method
            jrpc.params = params
            jrpc.id = Math.round(Math.random() * Long.MAX_VALUE).toString()
            return jrpc
        }

        fun createError(code: Int, message: String): JsonRPC {
            val jrpc = JsonRPC()
            jrpc.error = mapOf(ERRCODE to code, ERRMSG to message)
            jrpc.id = "local generated"
            return jrpc
        }

        fun fromJsonString(json: String): JsonRPC {
            var gson = GsonBuilder().create()
            return gson.fromJson<JsonRPC>(json, JsonRPC::class.java)
        }
    }

    fun toJsonString(): String {
        var gson = GsonBuilder().create()
        return gson.toJson(this)
    }

    fun isMethod(): Boolean {
        return method != null
    }

    fun isResult(): Boolean {
        return result != null
    }

    fun isError(): Boolean {
        return error != null
    }

    fun getErrCode(): Int{
        return error?.getLong(ERRCODE)?.toInt()?:-1
    }

    fun getErrMsg(): String{
        return error?.getString(ERRMSG)?:""
    }
}


private fun get(obj: Any, keys: String): Any? {
    try {
        var arr = keys.split(".") as MutableList
        var obj = obj
        var idx = 0
        while (arr.size > idx) {
            var key = arr.get(idx++)
//                println("key:" + key + "   " + obj.javaClass.name)
            when (obj) {
                is Map<*, *> -> {
                    obj = obj.get(key)!!
                }
                is ArrayList<*> -> {
                    obj = obj.get(key.toInt())!!
                }
                else -> {
                    println("unknown datatype " + obj.javaClass.name)
                }
            }
        }
        return obj
    } catch (error: Exception) {
        println("can't get value : " + keys)
        error.printStackTrace()
    }
    return null
}

fun Map<*, *>.getMap(keys: String): Map<*,*>? {
    try {
        return get(this, keys) as Map<*,*>
    } catch (error: Exception) {
        error.printStackTrace()
    }
    return null
}
fun Map<*, *>.getList(keys: String): List<*>? {
    try {
        return get(this, keys) as List<*>
    } catch (error: Exception) {
        error.printStackTrace()
    }
    return null
}
fun Map<*, *>.getBoolean(keys: String, def: Boolean? = false): Boolean? {
    try {
        return get(this, keys).toString().toBoolean()
    } catch (error: Exception) {
        error.printStackTrace()
    }
    return def
}

fun Map<*, *>.getString(keys: String, def: String? = null): String? {
    try {
        return get(this, keys).toString()
    } catch (error: Exception) {
        error.printStackTrace()
    }
    return def
}

fun Map<*, *>.getLong(keys: String, def: Long? = null): Long? {
    try {
        return get(this, keys).toString().toDouble().toLong()
    } catch (error: Exception) {
        error.printStackTrace()
    }
    return def
}