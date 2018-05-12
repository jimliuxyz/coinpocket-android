package com.jimliuxyz.coinpocketandroid

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.jimliuxyz.maprunner.utils.doMain
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity(), MainContract.View {

    private lateinit var dtypeSpinner: Spinner
    private lateinit var tvBalance: TextView
    private lateinit var txAmount: TextView
    private lateinit var txAccount: TextView

    private val presenter = MainPresenter()
    private var balance: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        dtypeSpinner = findViewById(R.id.spinner)
        tvBalance = findViewById(R.id.tvBalance)
        txAmount = findViewById(R.id.txAmount)
        txAccount = findViewById(R.id.txAccount)

        dtypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                balance?.let {
                    updateBalance(it)
                }
            }
        }

        findViewById<RecyclerView>(R.id.recyclerView)?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)

            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))

            adapter = TxReceiptAdapter(arrayListOf(), object : TxReceiptAdapter.ItemListener {
                override fun onItemClick(info: TxReceipt) {
                }

                override fun onItemLongClick(info: TxReceipt) {
                }
            })

            setHasFixedSize(true)

            var defaultItemAnimator = DefaultItemAnimator()
//            defaultItemAnimator.setAddDuration(1000)
//            defaultItemAnimator.setRemoveDuration(1000)
//            defaultItemAnimator.changeDuration = 1000
            setItemAnimator(defaultItemAnimator)
        }

        presenter.start(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                presenter.logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    var dialog: AlertDialog? = null

    override fun showDialog(){
        val factory = LayoutInflater.from(this)
        val view = factory.inflate(R.layout.loading_layout, null)
        view.findViewById<TextView>(R.id.loading_msg)!!.setText("交易進行中")

        dialog = AlertDialog.Builder(this@MainActivity)
                .setView(view)
                .setCancelable(false)
                .create()

        dialog?.apply {
            getWindow().setWindowAnimations(android.R.style.Animation_Translucent)
            show()
        }
    }

    override fun hideDialog(){
        dialog?.apply {
            dismiss()
        }
    }

    private fun getDtype():Int{
        return dtypeSpinner.selectedItemPosition
    }

    private fun getTxAmount(): Long{
        return txAmount.text.toString().toDouble().toLong()
    }

    private fun getTxAccount(): String{
        return txAccount.text.toString()
    }

    fun deposit(view: View){
        view.isEnabled = false
        presenter.deposit(getDtype(), getTxAmount())
        view.isEnabled = true
    }
    fun withdraw(view: View){
        view.isEnabled = false
        presenter.withdraw(getDtype(), getTxAmount())
        view.isEnabled = true
    }
    fun transfer(view: View){
        view.isEnabled = false
        presenter.transfer(getDtype(), getTxAmount(), getTxAccount())
        view.isEnabled = true
    }

    override fun updateBalance(list: List<String>) {
        balance = list

        val type = dtypeSpinner.selectedItemPosition
        tvBalance.setText(list[type])
    }

    override fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun backToLogin() {
        finish()

        var intent = Intent(applicationContext, LoginActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun appendTxList(list: List<TxReceipt>) {
        (findViewById<RecyclerView>(R.id.recyclerView)?.adapter as TxReceiptAdapter?)?.also {
            doMain {
                it.setData(list)
                it.notifyDataSetChanged()
            }
        }
    }

}
