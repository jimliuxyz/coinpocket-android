package com.jimliuxyz.coinpocketandroid

interface MainContract {

    interface View {
        fun showDialog()
        fun hideDialog()

        fun updateBalance(list: List<String>)
        fun showError(msg:String)

        fun backToLogin()

        fun appendTxList(list: List<TxReceipt>)
    }

    interface Presenter {

        fun start(view: View)
        fun stop()

        fun logout()

        fun updateBalance()

        fun deposit(dtype: Int, amount: Long)
        fun withdraw(dtype: Int, txAmount: Long)
        fun transfer(dtype: Int, txAmount: Long, txAccount: String)

        fun listReceipt()
    }

}