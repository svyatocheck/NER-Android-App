package com.svyatoslav.mlapp.ui.entry

import android.R
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.svyatoslav.mlapp.databinding.FragmentNoteEditorBinding
import com.svyatoslav.mlapp.ui.MainActivity
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class NoteEditorFragment : Fragment() {

    private val viewModel: NoteEditorViewModel by viewModel()

    private var debounceJob: Job? = null
    private val debounceDelay = 500L

    private var suppressTextChanges = false


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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

            val userInput = binding.editContent.text
            val userText = userInput.toString()

            binding.editContent.setSelection(userText.length)
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            binding.textStatus.apply {
                text = status
                visibility = View.VISIBLE
            }
        }

        viewModel.wisdomReply.observe(viewLifecycleOwner) { answer ->
            suppressTextChanges = true

            val userInput = binding.editContent.text
            val userText = userInput.toString()

            val fullText = SpannableStringBuilder()
                .append(userText.trim())
                .append("\n\n")

            val header = "🪄 "
            val response = answer.trim()

            val startHeader = fullText.length
            fullText.append(header)
            fullText.setSpan(StyleSpan(Typeface.BOLD), startHeader, startHeader + header.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            val startReply = fullText.length
            fullText.append(response)
            fullText.setSpan(StyleSpan(Typeface.BOLD), startReply, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            binding.editContent.setText(fullText)
            binding.editContent.setSelection(fullText.length)

            suppressTextChanges = false
        }

        binding.buttonAskWisdom.setOnClickListener {
            val userInput = binding.editContent.text ?: return@setOnClickListener
            val userText = userInput.toString()

            // Ответ кота
            viewModel.getWisdomFromLlm(userText)
        }



    }

    private fun createTextWatcher(onChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!suppressTextChanges) {
                    onChanged(s.toString())
                }
            }
        }
    }

    private fun debounceSave() {
        if (suppressTextChanges) return

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