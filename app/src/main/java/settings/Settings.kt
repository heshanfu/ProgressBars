/*
Copyright (C) 2018 Matthew Chandler

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.mattvchandler.progressbars.settings

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.databinding.DataBindingUtil
import android.preference.PreferenceManager
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast

import org.mattvchandler.progressbars.db.Data
import org.mattvchandler.progressbars.db.Progress_bars_table
import org.mattvchandler.progressbars.util.Dynamic_theme_activity
import org.mattvchandler.progressbars.util.Preferences
import org.mattvchandler.progressbars.R
import org.mattvchandler.progressbars.databinding.ActivitySettingsBinding

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

// Settings for each timer
class Settings: Dynamic_theme_activity(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener
{
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var data: Data
    private lateinit var save_data: Data

    private var date_time_dialog_target: Int = 0

    private lateinit var date_format: String
    private var locale = Locale.getDefault()
    private var date_df = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT) as SimpleDateFormat
    private var time_df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM) as SimpleDateFormat

    private val on_24_hour_change = object: ContentObserver(Handler())
    {
        override fun onChange(selfChange: Boolean)
        {
            super.onChange(selfChange)
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // set up timezone spinners
        val tz_adapter = ArrayAdapter(this, R.layout.right_aligned_spinner, TimeZone.getAvailableIDs())
        tz_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.startTz.adapter = tz_adapter
        binding.endTz.adapter = tz_adapter

        // only run this on 1st creation
        if(savedInstanceState == null)
        {
            val rowid = intent.getLongExtra(EXTRA_EDIT_ROW_ID, -1)

            // no rowid passed? make a new one
            data = if(rowid < 0)
            {
                setTitle(R.string.add_title)
                Data(this)
            }
            else
            {
                // get data from row
                setTitle(R.string.edit_title)
                Data(this, rowid)
            }
            save_data = Data(data)

            date_format = PreferenceManager.getDefaultSharedPreferences(this).getString("date_format", resources.getString(R.string.pref_date_format_default))!!

            date_df.isLenient = true
            time_df.isLenient = true
        }
        else
        {
            // reload old and current data from save state
            data = savedInstanceState.getSerializable(STATE_DATA) as Data
            save_data = savedInstanceState.getSerializable(STATE_SAVE_DATA) as Data
            date_time_dialog_target = savedInstanceState.getInt(STATE_TARGET)

            date_format = savedInstanceState.getString(STATE_DATE_FORMAT)!!
            date_df = savedInstanceState.getSerializable(STATE_DATE_DF) as SimpleDateFormat
            time_df = savedInstanceState.getSerializable(STATE_TIME_DF) as SimpleDateFormat
            locale = savedInstanceState.getSerializable(STATE_LOCALE) as Locale

            // populate date/time widget values
            if(date_format != "locale")
                date_df.applyPattern(date_format)

            date_df.isLenient = true
            time_df.isLenient = true

            if(data.rowid < 0)
                setTitle(R.string.add_title)
            else
                setTitle(R.string.edit_title)
        }

        // populate timezones and set selected values
        binding.data = data

        var found = 0
        for(i in 0 until tz_adapter.count)
        {
            if(tz_adapter.getItem(i) != null)
            {
                if(tz_adapter.getItem(i) == data.start_tz)
                {
                    binding.startTz.setSelection(i)
                    ++found
                }

                if(tz_adapter.getItem(i) == data.end_tz)
                {
                    binding.endTz.setSelection(i)
                    ++found
                }
                if(found == 2)
                    break
            }
        }

        val start_date = Date(data.start_time * 1000)
        date_df.timeZone = TimeZone.getTimeZone(data.start_tz)
        time_df.timeZone = TimeZone.getTimeZone(data.start_tz)
        binding.startDateSel.setText(date_df.format(start_date))
        binding.startTimeSel.setText(time_df.format(start_date))

        val end_date = Date(data.end_time * 1000)
        date_df.timeZone = TimeZone.getTimeZone(data.end_tz)
        time_df.timeZone = TimeZone.getTimeZone(data.end_tz)
        binding.endDateSel.setText(date_df.format(end_date))
        binding.endTimeSel.setText(time_df.format(end_date))

        binding.repeatFreq.visibility = if(data.repeats) View.VISIBLE else View.GONE
        binding.repeatCount.setText(data.repeat_count.toString())
        binding.repeatUnits.setSelection(data.repeat_unit)
        binding.repeatDaysOfWeek.text = get_days_of_week_abbr(this, data.repeat_days_of_week)

        val week_selected = data.repeat_unit == Progress_bars_table.Unit.WEEK.index
        binding.repeatOn.visibility = if(week_selected) View.VISIBLE else View.GONE
        binding.repeatDaysOfWeek.visibility = if(week_selected) View.VISIBLE else View.GONE
    }

    public override fun onResume()
    {
        super.onResume()

        // check for date format change
        val old_date_df = date_df
        val old_time_df = time_df
        val old_locale = locale

        date_format = PreferenceManager.getDefaultSharedPreferences(this).getString("date_format", resources.getString(R.string.pref_date_format_default))!!
        date_df = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT) as SimpleDateFormat
        time_df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM) as SimpleDateFormat
        if(date_format != "locale")
            date_df.applyLocalizedPattern(date_format)
        locale = Locale.getDefault()

        date_df.isLenient = true
        time_df.isLenient = true

        if(old_date_df.toLocalizedPattern() != date_df.toLocalizedPattern() || locale != old_locale)
        {
            // date format has changed. get formatter for old and new formats

            // parse date as old format, replace w/ new
            var date = binding.startDateSel.text.toString()
            var date_obj = old_date_df.parse(date, ParsePosition(0))
            if(date_obj != null)
            {
                date = date_df.format(date_obj)
                binding.startDateSel.setText(date)
            }

            date = binding.endDateSel.text.toString()
            date_obj = old_date_df.parse(date, ParsePosition(0))
            if(date_obj != null)
            {
                date = date_df.format(date_obj)
                binding.endDateSel.setText(date)
            }
        }
        if(old_time_df.toLocalizedPattern() != time_df.toLocalizedPattern() || locale != old_locale)
        {
            var time = binding.startTimeSel.text.toString()
            var date_obj = old_time_df.parse(time, ParsePosition(0))
            if(date_obj != null)
            {
                time = time_df.format(date_obj)
                binding.startTimeSel.setText(time)
            }

            time = binding.endTimeSel.text.toString()
            date_obj = old_time_df.parse(time, ParsePosition(0))
            if(date_obj != null)
            {
                time = time_df.format(date_obj)
                binding.endTimeSel.setText(time)
            }
        }

        // set listeners on time and date fields
        binding.startTimeSel.onFocusChangeListener = Time_listener(data)
        binding.endTimeSel.onFocusChangeListener = Time_listener(data)

        binding.startDateSel.onFocusChangeListener = Date_listener(date_format, data)
        binding.endDateSel.onFocusChangeListener = Date_listener(date_format, data)

        binding.repeatCount.onFocusChangeListener = Repeat_count_listener(data)

        binding.repeatUnits.onItemSelectedListener = object: AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long)
            {
                val week_selected = i == Progress_bars_table.Unit.WEEK.index
                binding.repeatOn.visibility = if(week_selected) View.VISIBLE else View.GONE
                binding.repeatDaysOfWeek.visibility = if(week_selected) View.VISIBLE else View.GONE

                data.repeat_unit = i
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        contentResolver.registerContentObserver(android.provider.Settings.System.getUriFor(android.provider.Settings.System.TIME_12_24), false, on_24_hour_change)
    }

    override fun onPause()
    {
        // clear listeners
        binding.startTimeSel.onFocusChangeListener = null
        binding.endTimeSel.onFocusChangeListener = null
        binding.startDateSel.onFocusChangeListener = null
        binding.endDateSel.onFocusChangeListener = null
        binding.repeatCount.onFocusChangeListener = null
        binding.repeatUnits.onItemSelectedListener = null

        contentResolver.unregisterContentObserver(on_24_hour_change)

        super.onPause()
    }

    override fun onSaveInstanceState(out: Bundle)
    {
        super.onSaveInstanceState(out)

        // save all data to be restored
        store_widgets_to_data()
        out.putSerializable(STATE_DATA, data)
        out.putSerializable(STATE_SAVE_DATA, save_data)
        out.putInt(STATE_TARGET, date_time_dialog_target)
        out.putString(STATE_DATE_FORMAT, date_format)
        out.putSerializable(STATE_DATE_DF, date_df)
        out.putSerializable(STATE_TIME_DF, time_df)
        out.putSerializable(STATE_LOCALE, locale)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.settings_action_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when(item.itemId)
        {
            R.id.save_butt ->
            {
                // dump all widget data into data struct
                if(!store_widgets_to_data())
                    return true

                // check to make sure start time is before end
                if(data.end_time < data.start_time)
                {
                    Toast.makeText(this, R.string.end_before_start_err, Toast.LENGTH_LONG).show()
                    return true
                }

                // insert new or update existing row
                if(data.rowid < 0)
                    data.insert(this)
                else
                    data.update(this)

                finish()
                return true
            }

            R.id.settings ->
            {
                startActivity(Intent(this, Preferences::class.java))
                return true
            }
        }
        return false
    }

    // dump all widget data into data obj
    private fun store_widgets_to_data(): Boolean
    {
        var errors = false
        // precision data has been stored through its callback already
        data.start_tz = binding.startTz.selectedItem.toString()
        data.end_tz = binding.endTz.selectedItem.toString()

        date_df.timeZone = TimeZone.getTimeZone(data.start_tz)
        time_df.timeZone = TimeZone.getTimeZone(data.start_tz)

        val start_date = date_df.parse(binding.startDateSel.text.toString(), ParsePosition(0))
        val start_time = time_df.parse(binding.startTimeSel.text.toString(), ParsePosition(0))

        // validate date and time
        if(start_date == null)
        {
            Toast.makeText(this@Settings, resources.getString(R.string.invalid_date,
                    binding.startDateSel.text, if(date_format != "locale") date_format else date_df.toLocalizedPattern()),
                    Toast.LENGTH_LONG).show()

            errors = true
        }
        if(start_time == null)
        {
            Toast.makeText(this@Settings, resources.getString(R.string.invalid_time,
                    binding.startTimeSel.text, time_df.toLocalizedPattern()),
                    Toast.LENGTH_LONG).show()

            errors = true
        }

        if(start_date != null && start_time != null)
            data.start_time = combine_date_and_time(start_date, start_time)

        date_df.timeZone = TimeZone.getTimeZone(data.end_tz)
        time_df.timeZone = TimeZone.getTimeZone(data.end_tz)

        val end_date = date_df.parse(binding.endDateSel.text.toString(), ParsePosition(0))
        val end_time = time_df.parse(binding.endTimeSel.text.toString(), ParsePosition(0))

        // validate date and time
        if(end_date == null)
        {
            Toast.makeText(this@Settings, resources.getString(R.string.invalid_date,
                    binding.endDateSel.text, date_format),
                    Toast.LENGTH_LONG).show()

            errors = true
        }
        if(end_time == null)
        {
            Toast.makeText(this@Settings, resources.getString(R.string.invalid_time,
                    binding.endTimeSel.text, time_df.toLocalizedPattern()),
                    Toast.LENGTH_LONG).show()

            errors = true
        }

        if(end_date != null && end_time != null)
            data.end_time = combine_date_and_time(end_date, end_time)

        // other repeat data stored in callbacks
        var repeat_count = 0
        try
        {
            repeat_count = Integer.parseInt(binding.repeatCount.text.toString())
        }
        catch(ignored: NumberFormatException)
        {
        }

        if(repeat_count <= 0)
        {
            Toast.makeText(this@Settings, R.string.invalid_repeat_count, Toast.LENGTH_LONG).show()
            errors = true
        }
        else
        {
            data.repeat_count = repeat_count
        }

        data.title = binding.title.text.toString()

        return !errors
    }

    // Button pressed callbacks
    fun on_start_cal_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        // create a calendar dialog, pass current date string
        date_time_dialog_target = R.id.start_date_sel
        val frag = Datepicker_frag()
        val args = Bundle()
        args.putString(Datepicker_frag.DATE, binding.startDateSel.text.toString())
        args.putLong(Datepicker_frag.STORE_DATE, data.start_time)
        frag.arguments = args
        frag.show(supportFragmentManager, "start_date_picker")
    }

    fun on_start_clock_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        // create a clock dialog, pass current time string
        date_time_dialog_target = R.id.start_time_sel
        val frag = Timepicker_frag()
        val args = Bundle()
        args.putString(Timepicker_frag.TIME, binding.startTimeSel.text.toString())
        args.putLong(Timepicker_frag.STORE_TIME, data.start_time)
        frag.arguments = args
        frag.show(supportFragmentManager, "start_time_picker")
    }

    fun on_end_cal_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        // create a calendar dialog, pass current date string
        date_time_dialog_target = R.id.end_date_sel
        val frag = Datepicker_frag()
        val args = Bundle()
        args.putString(Datepicker_frag.DATE, binding.endDateSel.text.toString())
        args.putLong(Datepicker_frag.STORE_DATE, data.end_time)
        frag.arguments = args
        frag.show(supportFragmentManager, "end_date_picker")
    }

    fun on_end_clock_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        // create a clock dialog, pass current time string
        date_time_dialog_target = R.id.end_time_sel
        val frag = Timepicker_frag()
        val args = Bundle()
        args.putString(Timepicker_frag.TIME, binding.endTimeSel.text.toString())
        args.putLong(Timepicker_frag.STORE_TIME, data.end_time)
        frag.arguments = args
        frag.show(supportFragmentManager, "end_time_picker")
    }

    fun on_repeat_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        data.repeats = binding.repeatSw.isChecked
        binding.repeatFreq.visibility = if(data.repeats) View.VISIBLE else View.GONE
    }

    fun on_precision_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        val d = Precision_dialog_frag()
        val args = Bundle()
        args.putInt(Precision_dialog_frag.PRECISION_ARG, data.precision)
        d.arguments = args
        d.show(supportFragmentManager, "precision")
    }

    fun on_days_of_week_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        val selected = BooleanArray(Progress_bars_table.Days_of_week.values().size)
        for(day in Progress_bars_table.Days_of_week.values())
        {
            selected[day.index] = data.repeat_days_of_week and day.mask != 0
        }

        val frag = Checkbox_dialog_frag()

        val args = Bundle()
        args.putInt(Checkbox_dialog_frag.TITLE_ARG, R.string.days_of_week_title)
        args.putInt(Checkbox_dialog_frag.ENTRIES_ARG, R.array.day_of_week)
        args.putBooleanArray(Checkbox_dialog_frag.SELECTION_ARG, selected)

        frag.arguments = args
        frag.show(supportFragmentManager, DAYS_OF_WEEK_CHECKBOX_DIALOG)
    }

    fun on_show_elements_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        val selected = BooleanArray(3)
        selected[SHOW_PROGRESS_CHECKBOX] = data.show_progress
        selected[SHOW_START_CHECKBOX] = data.show_start
        selected[SHOW_END_CHECKBOX] = data.show_end

        val frag = Checkbox_dialog_frag()

        val args = Bundle()
        args.putInt(Checkbox_dialog_frag.TITLE_ARG, R.string.show_elements_header)
        args.putInt(Checkbox_dialog_frag.ENTRIES_ARG, R.array.show_elements)
        args.putBooleanArray(Checkbox_dialog_frag.SELECTION_ARG, selected)

        frag.arguments = args
        frag.show(supportFragmentManager, SHOW_ELEMENTS_CHECKBOX_DIALOG)
    }

    fun on_show_units_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        val selected = BooleanArray(7)
        selected[SHOW_SECONDS_CHECKBOX] = data.show_seconds
        selected[SHOW_MINUTES_CHECKBOX] = data.show_minutes
        selected[SHOW_HOURS_CHECKBOX] = data.show_hours
        selected[SHOW_DAYS_CHECKBOX] = data.show_days
        selected[SHOW_WEEKS_CHECKBOX] = data.show_weeks
        selected[SHOW_MONTHS_CHECKBOX] = data.show_months
        selected[SHOW_YEARS_CHECKBOX] = data.show_years

        val frag = Checkbox_dialog_frag()

        val args = Bundle()
        args.putInt(Checkbox_dialog_frag.TITLE_ARG, R.string.show_units_header)
        args.putInt(Checkbox_dialog_frag.ENTRIES_ARG, R.array.time_units)
        args.putBooleanArray(Checkbox_dialog_frag.SELECTION_ARG, selected)

        frag.arguments = args
        frag.show(supportFragmentManager, SHOW_UNITS_CHECKBOX_DIALOG)
    }

    fun on_timer_opts_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        val selected = BooleanArray(3)
        selected[TERMINATE_CHECKBOX] = data.terminate
        selected[NOTIFY_START_CHECKBOX] = data.notify_start
        selected[NOTIFY_END_CHECKBOX] = data.notify_end

        val frag = Checkbox_dialog_frag()

        val args = Bundle()
        args.putInt(Checkbox_dialog_frag.TITLE_ARG, R.string.timer_opts_header)
        args.putInt(Checkbox_dialog_frag.ENTRIES_ARG, R.array.timer_opts)
        args.putBooleanArray(Checkbox_dialog_frag.SELECTION_ARG, selected)

        frag.arguments = args
        frag.show(supportFragmentManager, TIMER_OPTS_CHECKBOX_DIALOG)
    }

    fun on_countdown_text_butt(@Suppress("UNUSED_PARAMETER") view: View)
    {
        // Launch screen to enter countdown text
        val intent = Intent(this, Countdown_text::class.java)
        intent.putExtra(Countdown_text.EXTRA_DATA, data)
        startActivityForResult(intent, Countdown_text.RESULT_COUNTDOWN_TEXT)
    }

    // Dialog return callbacks

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int)
    {
        // build new string from returned data
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)

        val date_df = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT) as SimpleDateFormat
        if(date_format != "locale")
            date_df.applyPattern(date_format)

        (findViewById<View>(date_time_dialog_target) as android.support.design.widget.TextInputEditText).setText(date_df.format(cal.time))
    }

    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int)
    {
        // build new string from returned data
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)

        val df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM) as SimpleDateFormat
        (findViewById<View>(date_time_dialog_target) as android.support.design.widget.TextInputEditText).setText(df.format(cal.time))
    }

    // called when OK pressed on precision dialog
    fun on_precision_set(precision: Int)
    {
        // get and store the data
        data.precision = precision
        binding.precision.text = data.precision.toString()
    }

    // called when OK pressed on checkbox dialogs
    fun on_checkbox_dialog_ok(id: String, selected: BooleanArray)
    {
        if(id == DAYS_OF_WEEK_CHECKBOX_DIALOG)
        {
            var days_of_week = 0
            for(day in selected.indices)
            {
                if(selected[day])
                    days_of_week = days_of_week or (1 shl day)
            }

            if(days_of_week == 0)
            {
                Toast.makeText(this@Settings, R.string.no_days_of_week_err, Toast.LENGTH_LONG).show()
            }
            else
            {
                data.repeat_days_of_week = days_of_week
                binding.repeatDaysOfWeek.text = get_days_of_week_abbr(this@Settings, data.repeat_days_of_week)
            }
        }
        if(id == SHOW_ELEMENTS_CHECKBOX_DIALOG)
        {
            data.show_progress = selected[SHOW_PROGRESS_CHECKBOX]
            data.show_start = selected[SHOW_START_CHECKBOX]
            data.show_end = selected[SHOW_END_CHECKBOX]
        }
        if(id == SHOW_UNITS_CHECKBOX_DIALOG)
        {
            data.show_seconds = selected[SHOW_SECONDS_CHECKBOX]
            data.show_minutes = selected[SHOW_MINUTES_CHECKBOX]
            data.show_hours = selected[SHOW_HOURS_CHECKBOX]
            data.show_days = selected[SHOW_DAYS_CHECKBOX]
            data.show_weeks = selected[SHOW_WEEKS_CHECKBOX]
            data.show_months = selected[SHOW_MONTHS_CHECKBOX]
            data.show_years = selected[SHOW_YEARS_CHECKBOX]
        }
        if(id == TIMER_OPTS_CHECKBOX_DIALOG)
        {
            data.terminate = selected[TERMINATE_CHECKBOX]
            data.notify_start = selected[NOTIFY_START_CHECKBOX]
            data.notify_end = selected[NOTIFY_END_CHECKBOX]
        }
    }

    // get data back from Countdown_text
    override fun onActivityResult(request_code: Int, result_code: Int, intent: Intent?)
    {
        if(request_code == Countdown_text.RESULT_COUNTDOWN_TEXT && result_code == Activity.RESULT_OK)
        {
            // get changed data
            data = intent?.getSerializableExtra(Countdown_text.EXTRA_DATA) as Data
        }
    }

    companion object
    {
        const val EXTRA_EDIT_ROW_ID = "org.mattvchandler.progressbars.EDIT_ROW_ID"

        private const val STATE_DATA = "data"
        private const val STATE_SAVE_DATA = "save_data"
        private const val STATE_TARGET = "target"
        private const val STATE_DATE_FORMAT = "date_format"
        private const val STATE_DATE_DF = "date_df"
        private const val STATE_TIME_DF = "time_df"
        private const val STATE_LOCALE = "locale"

        private const val DAYS_OF_WEEK_CHECKBOX_DIALOG = "DAYS_OF_WEEK"
        private const val SHOW_ELEMENTS_CHECKBOX_DIALOG = "SHOW_ELEMENTS"
        private const val SHOW_UNITS_CHECKBOX_DIALOG = "SHOW_UNITS"
        private const val TIMER_OPTS_CHECKBOX_DIALOG = "SHOW_TIMER_OPTS"

        private const val SHOW_PROGRESS_CHECKBOX = 0
        private const val SHOW_START_CHECKBOX = 1
        private const val SHOW_END_CHECKBOX = 2

        private const val SHOW_SECONDS_CHECKBOX = 0
        private const val SHOW_MINUTES_CHECKBOX = 1
        private const val SHOW_HOURS_CHECKBOX = 2
        private const val SHOW_DAYS_CHECKBOX = 3
        private const val SHOW_WEEKS_CHECKBOX = 4
        private const val SHOW_MONTHS_CHECKBOX = 5
        private const val SHOW_YEARS_CHECKBOX = 6

        private const val TERMINATE_CHECKBOX = 0
        private const val NOTIFY_START_CHECKBOX = 1
        private const val NOTIFY_END_CHECKBOX = 2

        private fun get_days_of_week_abbr(context: Context, days_of_week: Int): String
        {
            // set days of week for weekly repeat (ex: MWF)
            val days_of_week_str = StringBuilder()
            for(day in Progress_bars_table.Days_of_week.values())
            {
                if(days_of_week and day.mask != 0)
                    days_of_week_str.append(context.resources.getStringArray(R.array.day_of_week_abbr)[day.index])
            }

            return days_of_week_str.toString()
        }

        private fun combine_date_and_time(date: Date, time: Date): Long
        {
            val date_cal = Calendar.getInstance()
            date_cal.time = date
            val time_cal = Calendar.getInstance()
            time_cal.time = time
            date_cal.set(Calendar.HOUR_OF_DAY, time_cal.get(Calendar.HOUR_OF_DAY))
            date_cal.set(Calendar.MINUTE, time_cal.get(Calendar.MINUTE))
            date_cal.set(Calendar.SECOND, time_cal.get(Calendar.SECOND))
            return date_cal.timeInMillis / 1000
        }
    }
}
