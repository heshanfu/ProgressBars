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

package org.mattvchandler.progressbars.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns

// DB Table schema
open class Progress_bars_table: BaseColumns
{
    // associated enums
    enum class Days_of_week (val index: Int, val mask: Int)
    {
        SUNDAY(0, 0x01),
        MONDAY(1, 0x02),
        TUESDAY(2, 0x04),
        WEDNESDAY(3, 0x08),
        THURSDAY(4, 0x10),
        FRIDAY(5, 0x20),
        SATURDAY(6, 0x40);

        companion object
        {
            fun all_days_mask(): Int
            {
                return SUNDAY.mask or MONDAY.mask or TUESDAY.mask or WEDNESDAY.mask or THURSDAY.mask or FRIDAY.mask or SATURDAY.mask
            }
        }
    }

    enum class Unit(val index: Int)
    {
        SECOND(0),
        MINUTE(1),
        HOUR(2),
        DAY(3),
        WEEK(4),
        MONTH(5),
        YEAR(6)
    }

    companion object
    {
        const val TABLE_NAME = "progress_bar"

        const val ORDER_COL = "order_ind"
        const val START_TIME_COL = "start_time"
        const val START_TZ_COL = "start_tz"
        const val END_TIME_COL = "end_time"
        const val END_TZ_COL = "end_tz"

        const val REPEATS_COL = "repeats"
        const val REPEAT_COUNT_COL = "repeat_count"
        const val REPEAT_UNIT_COL = "repeat_unit"
        const val REPEAT_DAYS_OF_WEEK_COL = "repeat_days_of_week"

        const val TITLE_COL = "title"
        const val PRE_TEXT_COL = "pre_text"
        const val START_TEXT_COL = "start_text"
        const val COUNTDOWN_TEXT_COL = "countdown_text"
        const val COMPLETE_TEXT_COL = "complete_text"
        const val POST_TEXT_COL = "post_text"

        const val PRECISION_COL = "precision"

        const val SHOW_START_COL = "show_start"
        const val SHOW_END_COL = "show_end"
        const val SHOW_PROGRESS_COL = "show_progress"

        const val SHOW_YEARS_COL = "show_years"
        const val SHOW_MONTHS_COL = "show_months"
        const val SHOW_WEEKS_COL = "show_weeks"
        const val SHOW_DAYS_COL = "show_days"
        const val SHOW_HOURS_COL = "show_hours"
        const val SHOW_MINUTES_COL = "show_minutes"
        const val SHOW_SECONDS_COL = "show_seconds"

        const val TERMINATE_COL = "terminate"
        const val NOTIFY_START_COL = "notify_start"
        const val NOTIFY_END_COL = "notify_end"

        // Select stmt to get all columns, all rows, ordered by order #
        const val SELECT_ALL_ROWS = "SELECT * FROM $TABLE_NAME ORDER BY $ORDER_COL"

        // table schema
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ORDER_COL + " INTEGER UNIQUE NOT NULL, " +
                START_TIME_COL + " INTEGER NOT NULL, " +
                START_TZ_COL + " TEXT NOT NULL, " +
                END_TIME_COL + " INTEGER NOT NULL, " +
                END_TZ_COL + " TEXT NOT NULL, " +

                REPEATS_COL + " INTEGER NOT NULL, " +
                REPEAT_COUNT_COL + " INTEGER NOT NULL, " +
                REPEAT_UNIT_COL + " INTEGER NOT NULL, " +
                REPEAT_DAYS_OF_WEEK_COL + " INTEGER NOT NULL, " +

                TITLE_COL + " TEXT NOT NULL, " +
                PRE_TEXT_COL + " TEXT, " +
                START_TEXT_COL + " TEXT, " +
                COUNTDOWN_TEXT_COL + " TEXT, " +
                COMPLETE_TEXT_COL + " TEXT, " +
                POST_TEXT_COL + " TEXT, " +

                PRECISION_COL + " INTEGER NOT NULL, " +

                SHOW_START_COL + " INTEGER NOT NULL, " +
                SHOW_END_COL + " INTEGER NOT NULL, " +
                SHOW_PROGRESS_COL + " INTEGER NOT NULL, " +

                SHOW_YEARS_COL + " INTEGER NOT NULL, " +
                SHOW_MONTHS_COL + " INTEGER NOT NULL, " +
                SHOW_WEEKS_COL + " INTEGER NOT NULL, " +
                SHOW_DAYS_COL + " INTEGER NOT NULL, " +
                SHOW_HOURS_COL + " INTEGER NOT NULL, " +
                SHOW_MINUTES_COL + " INTEGER NOT NULL, " +
                SHOW_SECONDS_COL + " INTEGER NOT NULL, " +

                TERMINATE_COL + " INTEGER NOT NULL, " +
                NOTIFY_START_COL + " INTEGER NOT NULL, " +
                NOTIFY_END_COL + " INTEGER NOT NULL)"

        fun upgrade(db: SQLiteDatabase, old_version: Int)
        {
            val table_exists = db.query("sqlite_master", arrayOf("name"), "type = 'table' AND name = ?", arrayOf(TABLE_NAME), null, null, null)
            if(table_exists.count == 0)
            {
                db.execSQL(CREATE_TABLE)
                return
            }

            when(old_version)
            {
                1 ->
                {
                    // Added some new columns - copy old data and insert defaults for new columns
                    db.execSQL("ALTER TABLE $TABLE_NAME RENAME TO TMP_$TABLE_NAME")
                    db.execSQL(CREATE_TABLE)
                    db.execSQL("INSERT INTO " + TABLE_NAME +
                            "(" +
                            ORDER_COL + ", " +
                            START_TIME_COL + ", " +
                            END_TIME_COL + ", " +
                            START_TZ_COL + ", " +
                            END_TZ_COL + ", " +
                            REPEATS_COL + ", " +
                            REPEAT_COUNT_COL + ", " +
                            REPEAT_UNIT_COL + ", " +
                            REPEAT_DAYS_OF_WEEK_COL + ", " +
                            TITLE_COL + ", " +
                            PRE_TEXT_COL + ", " +
                            START_TEXT_COL + ", " +
                            COUNTDOWN_TEXT_COL + ", " +
                            COMPLETE_TEXT_COL + ", " +
                            POST_TEXT_COL + ", " +
                            PRECISION_COL + ", " +
                            SHOW_START_COL + ", " +
                            SHOW_END_COL + ", " +
                            SHOW_PROGRESS_COL + ", " +
                            SHOW_YEARS_COL + ", " +
                            SHOW_MONTHS_COL + ", " +
                            SHOW_WEEKS_COL + ", " +
                            SHOW_DAYS_COL + ", " +
                            SHOW_HOURS_COL + ", " +
                            SHOW_MINUTES_COL + ", " +
                            SHOW_SECONDS_COL + ", " +
                            TERMINATE_COL + ", " +
                            NOTIFY_START_COL + ", " +
                            NOTIFY_END_COL +
                            ")" +
                            " SELECT " +
                            ORDER_COL + ", " +
                            START_TIME_COL + ", " +
                            END_TIME_COL + ", " +
                            START_TZ_COL + ", " +
                            END_TZ_COL + ", " +
                            "0, " +
                            "1, " +
                            Unit.DAY.index.toString() + ", " +
                            Days_of_week.all_days_mask().toString() + ", " +
                            TITLE_COL + ", " +
                            PRE_TEXT_COL + ", " +
                            START_TEXT_COL + ", " +
                            COUNTDOWN_TEXT_COL + ", " +
                            COMPLETE_TEXT_COL + ", " +
                            POST_TEXT_COL + ", " +
                            PRECISION_COL + ", " +
                            SHOW_START_COL + ", " +
                            SHOW_END_COL + ", " +
                            SHOW_PROGRESS_COL + ", " +
                            SHOW_YEARS_COL + ", " +
                            SHOW_MONTHS_COL + ", " +
                            SHOW_WEEKS_COL + ", " +
                            SHOW_DAYS_COL + ", " +
                            SHOW_HOURS_COL + ", " +
                            SHOW_MINUTES_COL + ", " +
                            SHOW_SECONDS_COL + ", " +
                            TERMINATE_COL + ", " +
                            NOTIFY_START_COL + ", " +
                            NOTIFY_END_COL + " " +
                            "FROM TMP_" + TABLE_NAME)

                    db.execSQL("DROP TABLE TMP_$TABLE_NAME")
                }
                2 ->
                {
                    // fixed NOT NULL for some columns - copy all data over
                    db.execSQL("ALTER TABLE $TABLE_NAME RENAME TO TMP_$TABLE_NAME")
                    db.execSQL(CREATE_TABLE)
                    db.execSQL("INSERT INTO " + TABLE_NAME +
                            "(" +
                            ORDER_COL + ", " +
                            START_TIME_COL + ", " +
                            END_TIME_COL + ", " +
                            START_TZ_COL + ", " +
                            END_TZ_COL + ", " +
                            REPEATS_COL + ", " +
                            REPEAT_COUNT_COL + ", " +
                            REPEAT_UNIT_COL + ", " +
                            REPEAT_DAYS_OF_WEEK_COL + ", " +
                            TITLE_COL + ", " +
                            PRE_TEXT_COL + ", " +
                            START_TEXT_COL + ", " +
                            COUNTDOWN_TEXT_COL + ", " +
                            COMPLETE_TEXT_COL + ", " +
                            POST_TEXT_COL + ", " +
                            PRECISION_COL + ", " +
                            SHOW_START_COL + ", " +
                            SHOW_END_COL + ", " +
                            SHOW_PROGRESS_COL + ", " +
                            SHOW_YEARS_COL + ", " +
                            SHOW_MONTHS_COL + ", " +
                            SHOW_WEEKS_COL + ", " +
                            SHOW_DAYS_COL + ", " +
                            SHOW_HOURS_COL + ", " +
                            SHOW_MINUTES_COL + ", " +
                            SHOW_SECONDS_COL + ", " +
                            TERMINATE_COL + ", " +
                            NOTIFY_START_COL + ", " +
                            NOTIFY_END_COL +
                            ")" +
                            " SELECT " +
                            ORDER_COL + ", " +
                            START_TIME_COL + ", " +
                            END_TIME_COL + ", " +
                            START_TZ_COL + ", " +
                            END_TZ_COL + ", " +
                            REPEATS_COL + ", " +
                            REPEAT_COUNT_COL + ", " +
                            REPEAT_UNIT_COL + ", " +
                            REPEAT_DAYS_OF_WEEK_COL + ", " +
                            TITLE_COL + ", " +
                            PRE_TEXT_COL + ", " +
                            START_TEXT_COL + ", " +
                            COUNTDOWN_TEXT_COL + ", " +
                            COMPLETE_TEXT_COL + ", " +
                            POST_TEXT_COL + ", " +
                            PRECISION_COL + ", " +
                            SHOW_START_COL + ", " +
                            SHOW_END_COL + ", " +
                            SHOW_PROGRESS_COL + ", " +
                            SHOW_YEARS_COL + ", " +
                            SHOW_MONTHS_COL + ", " +
                            SHOW_WEEKS_COL + ", " +
                            SHOW_DAYS_COL + ", " +
                            SHOW_HOURS_COL + ", " +
                            SHOW_MINUTES_COL + ", " +
                            SHOW_SECONDS_COL + ", " +
                            TERMINATE_COL + ", " +
                            NOTIFY_START_COL + ", " +
                            NOTIFY_END_COL + " " +
                            "FROM TMP_" + TABLE_NAME)

                    db.execSQL("DROP TABLE TMP_$TABLE_NAME")
                }
                else ->
                {
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
                    db.execSQL(CREATE_TABLE)
                }
            }
            table_exists.close()
        }

        // redo order column to remove gaps, etc. Order #s will be sequential, from 0 to count
        fun cleanup_order(context: Context)
        {
            val db = DB(context).writableDatabase
            val cursor = db.rawQuery(SELECT_ALL_ROWS, null)

            val values = ContentValues()

            for(i in 0 until cursor.count)
            {
                values.clear()
                values.put(ORDER_COL, i)
                cursor.moveToPosition(i)
                db.update(TABLE_NAME, values, BaseColumns._ID + " = ?", arrayOf(cursor.get_nullable_string(BaseColumns._ID)!!))
            }

            cursor.close()
            db.close()
        }
    }
}
