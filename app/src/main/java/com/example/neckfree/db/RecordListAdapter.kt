package com.example.neckfree.db

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.neckfree.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordListAdapter(
    private val onItemClicked: (MeasurementRecord) -> Unit,
    private val onDeleteClicked: (MeasurementRecord) -> Unit // ✅ [추가] 삭제 콜백
) : RecyclerView.Adapter<RecordListAdapter.RecordViewHolder>() {

    private var records = emptyList<MeasurementRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.record_list_item, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val current = records[position]
        holder.bind(current, onItemClicked, onDeleteClicked)
    }

    override fun getItemCount() = records.size

    fun submitList(newRecords: List<MeasurementRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.recordDateText)
        private val timeTextView: TextView = itemView.findViewById(R.id.recordTimeText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
        private val timeFormat = SimpleDateFormat("a hh:mm:ss", Locale.KOREA)

        fun bind(record: MeasurementRecord, onItemClicked: (MeasurementRecord) -> Unit, onDeleteClicked: (MeasurementRecord) -> Unit) {
            val date = Date(record.timestamp)
            dateTextView.text = dateFormat.format(date)
            timeTextView.text = timeFormat.format(date)

            itemView.setOnClickListener { onItemClicked(record) }
            deleteButton.setOnClickListener { onDeleteClicked(record) }
        }
    }
}