package com.example.neckfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // ✅ [수정] 어댑터 클릭 시, 상세 페이지로 이동하도록 변경
        adapter = RecordListAdapter { record ->
            val action = StatsFragmentDirections.actionStatsToDetail(record.id)
            findNavController().navigate(action)
        }
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
}
