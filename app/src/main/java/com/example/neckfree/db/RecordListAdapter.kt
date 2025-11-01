package com.example.neckfree.db

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.neckfree.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordListAdapter(
    private val onItemClicked: (MeasurementRecord) -> Unit
) : RecyclerView.Adapter<RecordListAdapter.RecordViewHolder>() {

    private var records = emptyList<MeasurementRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.record_list_item, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val current = records[position]
        holder.bind(current)
        // ✅ [수정] 클릭 시, 상세 리포트 업데이트가 아닌, 클릭 이벤트 전달
        holder.itemView.setOnClickListener { 
            onItemClicked(current)
        }
    }

    override fun getItemCount() = records.size

    fun submitList(newRecords: List<MeasurementRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.recordDateText)
        private val timeTextView: TextView = itemView.findViewById(R.id.recordTimeText)
        private val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
        private val timeFormat = SimpleDateFormat("a hh:mm:ss", Locale.KOREA)

        fun bind(record: MeasurementRecord) {
            val date = Date(record.timestamp)
            dateTextView.text = dateFormat.format(date)
            timeTextView.text = timeFormat.format(date)
        }
    }
}
