package com.svyatoslav.mlapp.ui.journal

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.svyatoslav.mlapp.databinding.FragmentJournalItemBinding
import com.svyatoslav.mlapp.domain.model.JournalNote

interface OnNoteSwipedListener {
    fun onNoteSwiped(note: JournalNote)
}

class JournalNoteAdapter(
    private val onNoteClick: (JournalNote) -> Unit,
    internal val onNoteSwiped: OnNoteSwipedListener
) : ListAdapter<JournalNote, JournalNoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {

    inner class NoteViewHolder(val binding: FragmentJournalItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(note: JournalNote) {
            binding.timestampText.text = getRelativeTime(note.createdAt)
            binding.titleText.text = note.title
            binding.bodyText.text = note.content

//            binding.syncStatus.apply {
//                visibility = if (note.isSynced) View.GONE else View.VISIBLE
//                text = "Saving..."
//            }

            binding.root.setOnClickListener {
                onNoteClick(note)
            }

            // Анимация появления
            binding.root.alpha = 0f
            binding.root.animate().alpha(1f).setDuration(300).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = FragmentJournalItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun getRelativeTime(timestamp: Long): String {
        return DateUtils.getRelativeTimeSpanString(
            timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<JournalNote>() {
            override fun areItemsTheSame(oldItem: JournalNote, newItem: JournalNote): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: JournalNote, newItem: JournalNote): Boolean =
                oldItem == newItem
        }
    }
}
