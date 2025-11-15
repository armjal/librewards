package com.example.librewards

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHandler(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    //Method that creates the tables and the columns within where the columns have been given data types and names.
    override fun onCreate(db: SQLiteDatabase) {
        val table1 =
            "CREATE TABLE $TABLE1 (id INTEGER PRIMARY KEY AUTOINCREMENT,universities TEXT) "
        db.execSQL(table1)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE1")
        onCreate(db)
    }

    fun storeUniversities(universityList: List<String?>, table: String) {
        val db = this.writableDatabase
        val sql = "INSERT INTO $table(universities)VALUES (?)"
        db.beginTransaction()
        val stmt = db.compileStatement(sql)
        for (i in universityList) {
            stmt.bindString(1, i)
            stmt.execute()
            stmt.clearBindings()
        }
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun getAllUniversities(): List<String> {
        val labels: MutableList<String> = ArrayList()

        // Select All Query
        val selectQuery = "SELECT  * FROM $TABLE1"
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(selectQuery, null)

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                labels.add(cursor.getString(1))
            } while (cursor.moveToNext())
        }

        // closing connection
        cursor.close()
        db.close()

        // returning labels
        return labels
    }

    companion object {
        //Instantiating the database name and table names. Final values so they cannot be changed once they are created
        const val DATABASE_NAME = "universities.db"
        const val TABLE1 = "universities_table"
    }

    init {
        this.writableDatabase
    }

}