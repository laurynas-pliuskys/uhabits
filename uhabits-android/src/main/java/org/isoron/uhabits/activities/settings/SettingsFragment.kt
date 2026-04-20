/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.activities.settings

import android.app.backup.BackupManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.datetimepicker.time.TimePickerDialog
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.RESULT_BUG_REPORT
import org.isoron.uhabits.activities.habits.list.RESULT_EXPORT_CSV
import org.isoron.uhabits.activities.habits.list.RESULT_EXPORT_DB
import org.isoron.uhabits.activities.habits.list.RESULT_IMPORT_DATA
import org.isoron.uhabits.activities.habits.list.RESULT_REPAIR_DB
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.reminders.ReminderScheduler
import org.isoron.uhabits.core.ui.NotificationTray
import org.isoron.uhabits.core.utils.DateUtils.Companion.getLongWeekdayNames
import org.isoron.uhabits.notifications.AndroidNotificationTray.Companion.createAndroidNotificationChannel
import org.isoron.uhabits.notifications.RingtoneManager
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.startActivitySafely
import org.isoron.uhabits.utils.toFixedAndroidColor
import org.isoron.uhabits.widgets.WidgetUpdater
import java.util.Calendar

class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private var sharedPrefs: SharedPreferences? = null
    private var ringtoneManager: RingtoneManager? = null
    private lateinit var prefs: Preferences
    private var widgetUpdater: WidgetUpdater? = null
    private var reminderScheduler: ReminderScheduler? = null

    private val ringtonePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            ringtoneManager?.update(result.data)
            updateRingtoneDescription()
        }

    private val publicBackupFolderLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@registerForActivityResult
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, flags)
            sharedPrefs?.edit()?.putString("publicBackupFolder", uri.toString())?.apply()
            updatePublicBackupFolderSummary()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val appContext = requireContext().applicationContext
        if (appContext is HabitsApplication) {
            prefs = appContext.component.preferences
            widgetUpdater = appContext.component.widgetUpdater
            reminderScheduler = appContext.component.reminderScheduler
        }
        setResultOnPreferenceClick("importData", RESULT_IMPORT_DATA)
        setResultOnPreferenceClick("exportCSV", RESULT_EXPORT_CSV)
        setResultOnPreferenceClick("exportDB", RESULT_EXPORT_DB)
        setResultOnPreferenceClick("repairDB", RESULT_REPAIR_DB)
        setResultOnPreferenceClick("bugReport", RESULT_BUG_REPORT)
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        // NOP
    }

    override fun onPause() {
        sharedPrefs!!.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sr = StyledResources(context!!)
        view.setBackgroundColor(sr.getColor(R.attr.contrast0))
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): RecyclerView? {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState)
            .also { it.applyBottomInset() }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key ?: return false
        when (key) {
            "reminderSound" -> {
                showRingtonePicker()
                return true
            }
            "reminderCustomize" -> {
                createAndroidNotificationChannel(requireContext())
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, NotificationTray.REMINDERS_CHANNEL_ID)
                startActivity(intent)
                return true
            }
            "rateApp" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.playStoreURL)))
                activity?.startActivitySafely(intent)
                return true
            }
            "publicBackupFolder" -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
                publicBackupFolderLauncher.launch(intent)
                return true
            }
            "pref_nightly_backup_time" -> {
                showNightlyBackupTimePicker()
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onResume() {
        super.onResume()
        ringtoneManager = RingtoneManager(requireActivity())
        sharedPrefs = preferenceManager.sharedPreferences
        sharedPrefs!!.registerOnSharedPreferenceChangeListener(this)
        if (!prefs.isDeveloper) {
            val devCategory = findPreference("devCategory") as PreferenceCategory
            devCategory.isVisible = false
        }
        updateWeekdayPreference()
        updatePublicBackupFolderSummary()
        updateNightlyBackupTimeSummary()

        findPreference("reminderSound").isVisible = false
    }

    private fun showNightlyBackupTimePicker() {
        val hour = prefs.nightlyBackupTime / 60
        val minute = prefs.nightlyBackupTime % 60
        val dialog = TimePickerDialog.newInstance(
            object : TimePickerDialog.OnTimeSetListener {
                override fun onTimeSet(view: com.android.datetimepicker.time.RadialPickerLayout?, hourOfDay: Int, minute: Int) {
                    prefs.nightlyBackupTime = hourOfDay * 60 + minute
                    updateNightlyBackupTimeSummary()
                }

                override fun onTimeCleared(view: com.android.datetimepicker.time.RadialPickerLayout?) {
                    // NOP
                }
            },
            hour,
            minute,
            DateFormat.is24HourFormat(requireContext()),
            PaletteColor(11).toFixedAndroidColor()
        )
        dialog.show(requireActivity().supportFragmentManager, "nightlyBackupTimePicker")
    }

    private fun updateNightlyBackupTimeSummary() {
        val pref = findPreference("pref_nightly_backup_time") ?: return
        val hour = prefs.nightlyBackupTime / 60
        val minute = prefs.nightlyBackupTime % 60
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        pref.summary = DateFormat.getTimeFormat(requireContext()).format(calendar.time)
    }

    private fun updateWeekdayPreference() {
        val weekdayPref = findPreference("pref_first_weekday") as ListPreference
        val currentFirstWeekday = prefs.firstWeekday.daysSinceSunday + 1
        val dayNames = getLongWeekdayNames(Calendar.SATURDAY)
        val dayValues = arrayOf("7", "1", "2", "3", "4", "5", "6")
        weekdayPref.entries = dayNames
        weekdayPref.entryValues = dayValues
        weekdayPref.setDefaultValue(currentFirstWeekday.toString())
        weekdayPref.summary = dayNames[currentFirstWeekday % 7]
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?
    ) {
        if (key == "pref_widget_opacity" && widgetUpdater != null) {
            Log.d("SettingsFragment", "updating widgets")
            widgetUpdater!!.updateWidgets()
        }

        if ((key == "pref_nightly_backup" || key == "pref_nightly_backup_time") && reminderScheduler != null) {
            Log.d("SettingsFragment", "scheduling alarms")
            reminderScheduler!!.scheduleAll()
        }

        BackupManager.dataChanged("org.isoron.uhabits")
        updateWeekdayPreference()
    }

    private fun setResultOnPreferenceClick(key: String, result: Int) {
        val pref = findPreference(key)
        pref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                requireActivity().setResult(result)
                requireActivity().finish()
                true
            }
    }

    private fun showRingtonePicker() {
        val existingRingtoneUri = ringtoneManager!!.getURI()
        val defaultRingtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(
            android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,
            android.media.RingtoneManager.TYPE_NOTIFICATION
        )
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        intent.putExtra(
            android.media.RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            defaultRingtoneUri
        )
        intent.putExtra(
            android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            existingRingtoneUri
        )
        ringtonePickerLauncher.launch(intent)
    }

    private fun updateRingtoneDescription() {
        val ringtoneName = ringtoneManager!!.getName() ?: return
        val ringtonePreference = findPreference("reminderSound")
        ringtonePreference.summary = ringtoneName
    }

    private fun updatePublicBackupFolderSummary() {
        val pref = findPreference("publicBackupFolder")
        val uriString = sharedPrefs?.getString("publicBackupFolder", null)
        if (uriString == null) {
            pref.summary = getString(R.string.no_public_backup_folder_selected)
            return
        }
        val uri = Uri.parse(uriString)
        val path = fullPathFor(uri)
        pref.summary = path ?: uriString
    }

    private fun fullPathFor(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val (type, rel) = docId.split(":", limit = 2).let {
                    it[0] to it.getOrElse(1) { "" }
                }
                val base = if (type.equals("primary", true)) {
                    Environment.getExternalStorageDirectory().absolutePath
                } else {
                    "/storage/$type"
                }
                if (rel.isEmpty()) base else "$base/$rel"
            }
            "file" -> java.io.File(uri.path!!).absolutePath
            else -> null
        }
    }
}
