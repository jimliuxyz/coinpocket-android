package com.jimliuxyz.coinpocketandroid

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

    override fun logout() {
        rpcAPI.disconn()
        view.backToLogin()
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

        view.showDialog()

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

//        rpcAPI.login("jim7", "", null){
//            println("login result" + it.toJsonString())
//        }
    }

    override fun withdraw(dtype: Int, txAmount: Long) {
        view.showDialog()

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
        view.showDialog()

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