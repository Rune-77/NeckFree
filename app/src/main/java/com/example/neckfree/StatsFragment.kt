package com.example.neckfree

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neckfree.db.MeasurementRecord
import com.example.neckfree.db.RecordListAdapter
import com.example.neckfree.viewmodel.StatsViewModel

class StatsFragment : Fragment() {

    private lateinit var statsViewModel: StatsViewModel
    private lateinit var recordsRecyclerView: RecyclerView
    private lateinit var adapter: RecordListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        recordsRecyclerView = view.findViewById(R.id.recordsRecyclerView)

        adapter = RecordListAdapter(
            onItemClicked = { record ->
                val action = StatsFragmentDirections.actionStatsToDetail(record.id)
                findNavController().navigate(action)
            },
            onDeleteClicked = { record ->
                showDeleteConfirmationDialog(record)
            }
        )
        recordsRecyclerView.adapter = adapter
        recordsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        statsViewModel = ViewModelProvider(this).get(StatsViewModel::class.java)

        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        statsViewModel.allRecords.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
        }
    }

    private fun showDeleteConfirmationDialog(record: MeasurementRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("기록 삭제")
            .setMessage("이 측정 기록을 정말로 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                statsViewModel.delete(record)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}