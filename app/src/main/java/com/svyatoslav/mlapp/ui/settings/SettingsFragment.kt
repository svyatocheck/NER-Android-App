package com.svyatoslav.mlapp.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.svyatoslav.mlapp.R
import com.svyatoslav.mlapp.domain.PrivacySettings
import com.svyatoslav.mlapp.domain.model.PrivacyMode
import androidx.core.content.edit

class SettingsFragment : Fragment() {

    private lateinit var editToken: EditText
    private lateinit var buttonSave: Button
    private lateinit var privacyGroup: RadioGroup

    private val prefs by lazy {
        requireContext().getSharedPreferences("wisdomCatToken", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        editToken = view.findViewById(R.id.editToken)
        buttonSave = view.findViewById(R.id.buttonSaveToken)
        privacyGroup = view.findViewById(R.id.privacyModeGroup)

        // Load saved token
        editToken.setText(prefs.getString("access_token", ""))

        // Save token on button click
        buttonSave.setOnClickListener {
            val token = editToken.text.toString().trim()
            prefs.edit() { putString("access_token", token) }
            Toast.makeText(requireContext(), "Token saved", Toast.LENGTH_SHORT).show()
        }

        // Restore saved privacy mode
        val mode = prefs.getString("privacy_mode", "none")
        when (mode) {
            "none" -> privacyGroup.check(R.id.privacyNone)
            "secure" -> privacyGroup.check(R.id.privacySecure)
            "max" -> privacyGroup.check(R.id.privacyMax)
        }

        privacyGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.privacyNone -> PrivacyMode.NONE
                R.id.privacySecure -> PrivacyMode.SECURE
                R.id.privacyMax -> PrivacyMode.MAXIMUM
                else -> PrivacyMode.NONE
            }
            PrivacySettings.savePrivacyMode(requireContext(), selected)
        }

        return view
    }
}
