package com.svyatoslav.mlapp.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.svyatoslav.mlapp.databinding.FragmentMainBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main UI Fragment for the app.
 * Allows user to input text and receive processed output (e.g., with PII masked).
 */
class MainFragment : Fragment() {

    // Inject the ViewModel using Koin dependency injection
    private val viewModel: MainViewModel by viewModel()

    // View binding for safe access to UI components
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    /**
     * Inflate the layout and initialize view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Setup event listeners and observe ViewModel LiveData when the view is created.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle the "Send" button click
        binding.buttonSend.setOnClickListener {
            val text = binding.inputText.text.toString()
            val useNer = binding.nerPreparation.isChecked
            viewModel.processText(text, useNer)
        }

        // Observe and display the processed (masked) text
        viewModel.resultLiveData.observe(viewLifecycleOwner) {
            binding.llmAnswer.text = it
        }

        // Observe and display the inference time
        viewModel.timeLiveData.observe(viewLifecycleOwner) {
            binding.timeField.text = "$it ms"
        }

        // Observe and display errors as Toasts; log the error and clear it from ViewModel
        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.errorMessageShown()
                Log.e("APP ERROR", it)
            }
        }
    }

    /**
     * Clear binding reference to avoid memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
