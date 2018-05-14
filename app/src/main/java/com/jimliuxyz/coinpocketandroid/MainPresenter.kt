package com.jimliuxyz.coinpocketandroid

import com.jimliuxyz.coinpocketandroid.RPC.JsonRPC
import com.jimliuxyz.coinpocketandroid.RPC.RpcAPI
import com.jimliuxyz.coinpocketandroid.RPC.getList
import com.jimliuxyz.coinpocketandroid.RPC.getMap
import com.jimliuxyz.maprunner.utils.doMain

class MainPresenter : MainContract.Presenter {

    private val rpcAPI = RpcAPI.getInstance()
    private lateinit var view: MainContract.View

    override fun start(view: MainContract.View) {
        this.view = view

        println("rpcAPI.getLoginState() : " + rpcAPI.getLoginState())

        if (rpcAPI.getLoginState() == RpcAPI.LOGED_FAIL) {
            view.backToLogin()
        } else {
            updateBalance()
            listReceipt()

            rpcAPI.setEventListener(object : RpcAPI.EventListener {
                override fun takeReceipt(rpc: JsonRPC) {
                    updateBalance()
                    val tmp = rpc.params!!.getMap("receipt")
                    val list = TxReceipt.fromList(listOf(tmp))
                    println("takeReceipt~~~~~~" + list.toString())

                    doMain {
                        view.appendTxList(list)
                    }
                }

                override fun loginStateChanged(logged: Int) {
                    println("logged : " + logged)
                    if (logged == RpcAPI.LOGED_OKAY) {
                        updateBalance()
                    } else if (logged == RpcAPI.LOGED_FAIL) {
                        rpcAPI.disconn()
                        view.backToLogin()
                    }
                }
            })
            if (rpcAPI.getLoginState() == RpcAPI.LOGED_NONE){
                rpcAPI.connWithToken(){
                }
            }
        }
    }

    var loggout = false
    override fun stop() {
        //todo: 預定在activity destroy時呼叫stop並進行disconnt 但發現不同版本的sdk上執行順序不同
        //一種是登出時呼叫logout隨後finish()->destroy() 回到login畫面
        //另一種在finish()後不會destroy 而在再次登入進入main時才destroy上一次的 造成登入隨後又登出
        //結論:rpcAPI不該做成singleton 造成狀態共用
        if (!loggout){
            rpcAPI.setEventListener(null)
            rpcAPI.disconn()
        }
        loggout = true
    }

    override fun logout() {
        view.backToLogin()
        if (!loggout)
            rpcAPI.disconn()
        loggout = true
    }

    override fun updateBalance() {
        rpcAPI.getBalance() { rpc ->

            if (rpc.isResult()) {
                val list = rpc.result!!.getList("balance") as List<String>
                doMain {
                    view.updateBalance(list)
                }
            }
        }
    }

    override fun listReceipt() {
        rpcAPI.listReceipt() { rpc ->

            if (rpc.isResult()) {
                val tmp = rpc.result!!.getList("list") as List<*>

                val list = TxReceipt.fromList(tmp)
                println("list result" + list)

                doMain {
                    view.appendTxList(list)
                }
            }
        }
    }

    private fun appendPendingTx(rpc: JsonRPC) {
        val receipt = TxReceipt()
        receipt.txhash = rpc.result!!.get("txhash") as String

        doMain {
            view.appendTxList(arrayListOf(receipt))
        }
    }

    override fun deposit(dtype: Int, amount: Long) {
        updateBalance()

        rpcAPI.deposit(dtype, amount) { rpc ->
            Thread.sleep(500)

            doMain {
                view.hideDialog()

                println("deposit result" + rpc.toJsonString())
                if (rpc.isResult()) {
                    appendPendingTx(rpc)
                } else if (rpc.isError()) {
                    view.showError(rpc.getErrMsg())
                }
            }
        }
    }

    override fun withdraw(dtype: Int, txAmount: Long) {
        updateBalance()

        rpcAPI.withdraw(dtype, txAmount) { rpc ->
            Thread.sleep(500)

            doMain {
                view.hideDialog()

                println("withdraw result" + rpc.toJsonString())
                if (rpc.isResult()) {
                    appendPendingTx(rpc)
                } else if (rpc.isError()) {
                    view.showError(rpc.getErrMsg())
                }
            }
        }
    }

    override fun transfer(dtype: Int, txAmount: Long, txAccount: String) {
        updateBalance()

        rpcAPI.transfer(dtype, txAmount, txAccount) { rpc ->
            Thread.sleep(500)

            doMain {
                view.hideDialog()

                println("transfer result" + rpc.toJsonString())
                if (rpc.isResult()) {
                    appendPendingTx(rpc)
                } else if (rpc.isError()) {
                    view.showError(rpc.getErrMsg())
                }
            }
        }
    }


}