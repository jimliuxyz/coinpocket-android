package com.jimliuxyz.coinpocketandroid

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class TxReceiptAdapter(var receipts: List<TxReceipt>, val itemListener: ItemListener) : RecyclerView.Adapter<TxReceiptAdapter.ViewHolder>() {

    interface ItemListener {
        fun onItemClick(info: TxReceipt)
        fun onItemLongClick(info: TxReceipt)
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var text1: TextView
        var text2: TextView
        val fmt = SimpleDateFormat("yyyy-MM-dd EEEE HH:mm", Locale.getDefault())
        lateinit var info: TxReceipt

        init {
            text1 = view.findViewById<TextView>(R.id.text1)
            text2 = view.findViewById<TextView>(R.id.text2)
            view.setOnClickListener {
                itemListener.onItemClick(info)
            }
            view.setOnLongClickListener {
                itemListener.onItemLongClick(info)
                true
            }
        }

        fun bindData(pos: Int) {
            info = receipts[pos]

            if (info.action == "deposit") {
                text1.setText("存入 " + info.amount + " " + info.dtypes)
            } else if (info.action == "withdraw") {
                text1.setText("提出 " + info.amount + " " + info.dtypes)
            } else if (info.action == "transfer") {
                text1.setText(info.sender + " 轉帳 " + info.amount + " " + info.dtypes + " 給 " + info.receiver)
            } else
                text1.setText("確認中...")

//            text2.setText(fmt.format(receipts[pos].attentionDate))
            text2.setText(info.txhash)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent?.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.main_list_item, parent, false)
        return ViewHolder(view)
    }


    override fun getItemCount(): Int {
        return receipts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(position)
    }

    fun setData(receipts_: List<TxReceipt>) {
        for ((idx1, value1) in receipts_.withIndex()) {
            for ((idx2, value2) in receipts.withIndex()) {
                if (value1.txhash.equals(value2.txhash) && value1.action.isNotEmpty()) {
                    (receipts_ as MutableList).removeAt(idx1)
                    (receipts as MutableList).set(idx2, value1) //replace
                    notifyItemChanged(idx1)
                    break
                }
            }
        }

        if (receipts_.size == 1)
            notifyItemInserted(1)
        else
            notifyDataSetChanged()

        (receipts_ as MutableList).addAll(receipts)

        receipts = receipts_
    }
}
