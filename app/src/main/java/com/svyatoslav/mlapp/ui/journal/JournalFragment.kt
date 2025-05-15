package com.svyatoslav.mlapp.ui.journal

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.svyatoslav.mlapp.R
import com.svyatoslav.mlapp.databinding.FragmentJournalListBinding
import com.svyatoslav.mlapp.domain.model.JournalNote
import org.koin.androidx.viewmodel.ext.android.viewModel

class JournalFragment : Fragment(R.layout.fragment_journal_list) {

    private var _binding: FragmentJournalListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalListViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJournalListBinding.bind(view)

        binding.progressCircular.visibility = View.VISIBLE
        val adapter = JournalNoteAdapter(
            onNoteClick = { openNote(it) },
            onNoteSwiped = object : OnNoteSwipedListener {
                override fun onNoteSwiped(note: JournalNote) {
                    viewModel.deleteNote(note)
                }
            }
        )

        binding.list.adapter = adapter

        binding.fabAdd.setOnClickListener {
            val action = JournalFragmentDirections.actionJournalToNoteEditor("-1")
            findNavController().navigate(action)
        }

        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && binding.fabAdd.isShown) {
                    binding.fabAdd.hide()
                } else if (dy < 0 && !binding.fabAdd.isShown) {
                    binding.fabAdd.show()
                }
            }
        })

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.adapterPosition
                val note = adapter.currentList[position]
                adapter.notifyItemChanged(position)
                adapter.onNoteSwiped.onNoteSwiped(note)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.5f
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.list)

        // Observe notes and update UI accordingly
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            Log.d("Journal","Notes count=${notes.size}")
            adapter.submitList(notes)

            val isEmpty = notes.isNullOrEmpty()
            binding.progressCircular.visibility = View.GONE
            binding.emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.list.visibility = if (isEmpty) View.GONE else View.VISIBLE
            //binding.fabAdd.visibility = if (isEmpty) View.VISIBLE else View.VISIBLE
        }
    }

    private fun openNote(entity: JournalNote) {
        val action = JournalFragmentDirections.actionJournalToNoteEditor(entity.id.toString())
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
