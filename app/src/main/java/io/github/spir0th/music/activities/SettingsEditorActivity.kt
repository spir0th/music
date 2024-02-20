package io.github.spir0th.music.activities

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivitySettingsEditorBinding
import io.github.spir0th.music.databinding.ViewholderRowSettingBinding
import io.github.spir0th.music.utils.adjustMarginsForSystemBarInsets
import io.github.spir0th.music.utils.adjustPaddingForSystemBarInsets

class SettingsEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsEditorBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        binding = ActivitySettingsEditorBinding.inflate(layoutInflater)
        binding.toolbar.adjustPaddingForSystemBarInsets(top=true)
        binding.settingsListReloader.adjustPaddingForSystemBarInsets(left=true, right=true, bottom=true)
        binding.settingsCreateItem.adjustMarginsForSystemBarInsets(left=true, right=true, bottom=true)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.prefs_editor_title)

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.settingsListReloader.setOnRefreshListener {
            populatePreferenceList()
        }
        binding.settingsCreateItem.setOnClickListener {
            val key = EditText(this).apply {
                hint = getString(R.string.prefs_editor_create_item_key_hint)
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.prefs_editor_create_item_title)
                .setView(key)
                .setPositiveButton(R.string.dialog_settings_edit_positive) { _, _ ->
                    preferences.edit().putString(key.text.toString(), String()).apply()
                    populatePreferenceList()
                }
                .setNegativeButton(R.string.dialog_settings_edit_negative) { _, _ -> }
                .show()
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_settings_edit_alert_title)
            .setMessage(R.string.dialog_settings_edit_alert_message)
            .setPositiveButton(R.string.dialog_settings_edit_alert_positive) { _, _ -> }
            .setCancelable(false)
            .show()

        populatePreferenceList()
    }

    private fun populatePreferenceList() {
        val data = mutableListOf<Pair<String, Any?>>()

        preferences.all.forEach { (key, value) ->
            data.add(Pair<String, Any?>(key, value))
        }

        Log.v(TAG, "Loaded ${data.size} keys")
        binding.settingsList.layoutManager = LinearLayoutManager(this)
        binding.settingsList.adapter = SettingsListAdapter(data)
        binding.settingsListReloader.isRefreshing = false
    }

    inner class SettingsListAdapter(private val data: MutableList<Pair<String, Any?>>) : RecyclerView.Adapter<SettingsListAdapter.ViewHolder>() {
        inner class ViewHolder(private val itemBinding: ViewholderRowSettingBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            init {
                itemBinding.root.setOnClickListener {
                    val input = EditText(this@SettingsEditorActivity).apply {
                        setText(itemBinding.value.text, TextView.BufferType.EDITABLE)
                    }
                    AlertDialog.Builder(this@SettingsEditorActivity)
                        .setTitle(itemBinding.key.text)
                        .setView(input)
                        .setPositiveButton(R.string.dialog_settings_edit_positive) { _, _ ->
                            val key = itemBinding.key.text.toString()
                            val value = input.text.toString()
                            itemBinding.value.text = input.text

                            if (value.toIntOrNull() != null) {
                                preferences.edit().putInt(key, value.toInt()).apply()
                            } else if (value.toLongOrNull() != null) {
                                preferences.edit().putLong(key, value.toLong()).apply()
                            } else if (value.lowercase() == "false" || value.lowercase() == "true") {
                                preferences.edit().putBoolean(key, value.toBoolean()).apply()
                            } else {
                                preferences.edit().putString(key, value).apply()
                            }
                        }
                        .setNegativeButton(R.string.dialog_settings_edit_negative) { _, _ -> }
                        .show()
                }
                itemBinding.root.setOnLongClickListener {
                    val key = itemBinding.key.text.toString()

                    AlertDialog.Builder(this@SettingsEditorActivity)
                        .setTitle(R.string.prefs_editor_delete_item_title)
                        .setMessage(getString(R.string.prefs_editor_delete_item_message, key))
                        .setPositiveButton(R.string.prefs_editor_delete_item_positive) { _, _ ->
                            preferences.edit().remove(key).apply()
                            data.removeAt(bindingAdapterPosition)
                            notifyItemRemoved(bindingAdapterPosition)
                            notifyItemRangeRemoved(bindingAdapterPosition, data.size - bindingAdapterPosition)
                        }
                        .setNegativeButton(R.string.prefs_editor_delete_item_negative) { _, _ -> }
                        .show()

                    true
                }
            }

            fun bind(key: String, value: Any?) {
                itemBinding.key.text = key
                itemBinding.value.text = value.toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ViewholderRowSettingBinding.inflate(LayoutInflater.from(this@SettingsEditorActivity), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.bind(item.first, item.second)
        }

        override fun getItemCount(): Int = data.size
    }

    companion object {
        const val TAG = "SettingsEditorActivity"
    }
}