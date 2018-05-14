package com.jimliuxyz.coinpocketandroid

import com.google.gson.GsonBuilder
import com.jimliuxyz.coinpocketandroid.RPC.JsonRPC
import java.util.*

data class TxReceipt(var txhash:String="", var sender:String="", var dtypes:String="", var action:String="", var amount:String="", var receiver:String="") {
//    var txhash = ""
//    var sender = ""
//    var dtypes = ""
//    var action = ""
//    var amount = ""
//    var receiver = ""

    companion object {
        fun fromJsonString(json: String): JsonRPC {
            var gson = GsonBuilder().create()
            return gson.fromJson<JsonRPC>(json, JsonRPC::class.java)
        }

        fun fromList(list: List<*>): LinkedList<TxReceipt> {
            val list = list as List<Map<String, String>>
            val newlist = LinkedList<TxReceipt>()
            for(item in list){

                val receipt = TxReceipt()
                receipt.txhash = item.get("txhash")!!
                receipt.sender = item.get("sender")!!
                receipt.dtypes = item.get("dtypes")!!
                receipt.action = item.get("action")!!
                receipt.amount = item.get("amount")!!
                receipt.receiver = item.get("receiver")!!
                newlist.add(receipt)
            }
            newlist.reverse()
            return newlist
        }

    }
}
