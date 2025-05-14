package com.svyatoslav.mlapp.ui.entry

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.svyatoslav.mlapp.databinding.FragmentNoteEditorBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class NoteEditorFragment : Fragment() {

    private val viewModel: NoteEditorViewModel by viewModel()

    private var debounceJob: Job? = null
    private val debounceDelay = 500L

    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!

    // Перенесли в поля
    private val titleWatcher = createTextWatcher { text ->
        viewModel.updateTitle(text)
        debounceSave()
    }
    private val contentWatcher = createTextWatcher { text ->
        viewModel.updateContent(text)
        debounceSave()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTitle.addTextChangedListener(titleWatcher)
        binding.editContent.addTextChangedListener(contentWatcher)

        viewModel.note.observe(viewLifecycleOwner) { note ->
            // Убираем, чтобы не ловить setText
            binding.editTitle.removeTextChangedListener(titleWatcher)
            binding.editContent.removeTextChangedListener(contentWatcher)

            binding.editTitle.setText(note.title)
            binding.editContent.setText(note.content)

            // Вешаем обратно
            binding.editTitle.addTextChangedListener(titleWatcher)
            binding.editContent.addTextChangedListener(contentWatcher)
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            binding.textStatus.apply {
                text = status
                visibility = View.VISIBLE
            }
        }
    }

    private fun createTextWatcher(onChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onChanged(s.toString())
            }
        }
    }

    private fun debounceSave() {
        debounceJob?.cancel()
        debounceJob = lifecycleScope.launch {
            delay(debounceDelay)
            viewModel.triggerSave()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}