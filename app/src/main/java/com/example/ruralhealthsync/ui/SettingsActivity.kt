package com.example.ruralhealthsync.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.ruralhealthsync.data.local.PreferenceManager
import com.example.ruralhealthsync.databinding.ActivitySettingsBinding

/**
 * SettingsActivity — lets users choose their preferred display theme:
 * Light, Dark, or Follow System Default.
 * The choice is persisted in SharedPreferences and applied immediately.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        // Setup toolbar
        binding.settingsToolbar.setNavigationOnClickListener { finish() }

        // Reflect the currently saved theme choice on the radio buttons
        when (preferenceManager.getThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_NO   -> binding.radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES  -> binding.radioDark.isChecked = true
            else                              -> binding.radioSystem.isChecked = true
        }

        // Immediately apply + save when a radio button is selected
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.radioLight.id  -> AppCompatDelegate.MODE_NIGHT_NO
                binding.radioDark.id   -> AppCompatDelegate.MODE_NIGHT_YES
                else                   -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            preferenceManager.saveThemeMode(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
            Toast.makeText(this, "Theme updated!", Toast.LENGTH_SHORT).show()
        }

        // Voice Input Toggle
        binding.voiceInputSwitch.isChecked = preferenceManager.isVoiceInputEnabled()
        binding.voiceInputSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setVoiceInputEnabled(isChecked)
            val msg = if (isChecked) "Voice input enabled" else "Voice input disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
