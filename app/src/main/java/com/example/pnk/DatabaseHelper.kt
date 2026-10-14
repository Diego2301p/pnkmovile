package com.example.pnk

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite helper for local offline storage.
 * Tracks sync_status per record to enable offline-first CRUD
 * with deferred synchronization to MySQL via API.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "pnk_database.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_USUARIOS = "usuarios"
        const val COL_ID = "id"
        const val COL_SERVER_ID = "server_id"
        const val COL_NOMBRE = "nombre"
        const val COL_APELLIDO = "apellido"
        const val COL_EMAIL = "email"
        const val COL_USERNAME = "username"
        const val COL_PASSWORD = "password"
        const val COL_SYNC_STATUS = "sync_status"

        const val SYNC_SYNCED = "synced"
        const val SYNC_PENDING_INSERT = "pending_insert"
        const val SYNC_PENDING_UPDATE = "pending_update"
        const val SYNC_PENDING_DELETE = "pending_delete"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USUARIOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SERVER_ID INTEGER DEFAULT NULL,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_APELLIDO TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL,
                $COL_USERNAME TEXT DEFAULT '',
                $COL_PASSWORD TEXT DEFAULT '',
                $COL_SYNC_STATUS TEXT DEFAULT '$SYNC_SYNCED'
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
    }

    // ==================== CRUD Operations ====================

    fun insertUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE, user.nombre)
            put(COL_APELLIDO, user.apellido)
            put(COL_EMAIL, user.email)
            put(COL_USERNAME, user.username)
            put(COL_PASSWORD, user.password)
            if (user.serverId != null) put(COL_SERVER_ID, user.serverId)
            put(COL_SYNC_STATUS, user.syncStatus)
        }
        val id = db.insert(TABLE_USUARIOS, null, values)
        return id
    }

    fun getAllUsers(): MutableList<User> {
        val users = mutableListOf<User>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USUARIOS, null,
            "$COL_SYNC_STATUS != ?", arrayOf(SYNC_PENDING_DELETE),
            null, null, "$COL_NOMBRE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                users.add(cursorToUser(it))
            }
        }
        return users
    }

    fun getUserById(id: Int): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USUARIOS, null,
            "$COL_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            return if (it.moveToFirst()) cursorToUser(it) else null
        }
    }

    fun updateUser(id: Int, nombre: String, apellido: String, email: String, syncStatus: String = SYNC_PENDING_UPDATE): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE, nombre)
            put(COL_APELLIDO, apellido)
            put(COL_EMAIL, email)
            put(COL_SYNC_STATUS, syncStatus)
        }
        return db.update(TABLE_USUARIOS, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun markUserForDeletion(id: Int): Int {
        val db = writableDatabase
        val user = getUserById(id)
        // If never synced to server, just delete locally
        if (user?.syncStatus == SYNC_PENDING_INSERT) {
            return deleteUserPermanently(id)
        }
        val values = ContentValues().apply {
            put(COL_SYNC_STATUS, SYNC_PENDING_DELETE)
        }
        return db.update(TABLE_USUARIOS, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteUserPermanently(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_USUARIOS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ==================== Duplicate Checks ====================

    fun emailExists(email: String, excludeId: Int? = null): Boolean {
        val db = readableDatabase
        val selection: String
        val args: Array<String>
        if (excludeId != null) {
            selection = "LOWER($COL_EMAIL) = LOWER(?) AND $COL_ID != ? AND $COL_SYNC_STATUS != ?"
            args = arrayOf(email, excludeId.toString(), SYNC_PENDING_DELETE)
        } else {
            selection = "LOWER($COL_EMAIL) = LOWER(?) AND $COL_SYNC_STATUS != ?"
            args = arrayOf(email, SYNC_PENDING_DELETE)
        }
        val cursor = db.query(TABLE_USUARIOS, arrayOf(COL_ID), selection, args, null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun usernameExists(username: String, excludeId: Int? = null): Boolean {
        if (username.isBlank()) return false
        val db = readableDatabase
        val selection: String
        val args: Array<String>
        if (excludeId != null) {
            selection = "LOWER($COL_USERNAME) = LOWER(?) AND $COL_ID != ? AND $COL_SYNC_STATUS != ?"
            args = arrayOf(username, excludeId.toString(), SYNC_PENDING_DELETE)
        } else {
            selection = "LOWER($COL_USERNAME) = LOWER(?) AND $COL_SYNC_STATUS != ?"
            args = arrayOf(username, SYNC_PENDING_DELETE)
        }
        val cursor = db.query(TABLE_USUARIOS, arrayOf(COL_ID), selection, args, null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // ==================== Sync Operations ====================

    fun getPendingChanges(): List<User> {
        val users = mutableListOf<User>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USUARIOS, null,
            "$COL_SYNC_STATUS != ?", arrayOf(SYNC_SYNCED),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                users.add(cursorToUser(it))
            }
        }
        return users
    }

    fun getPendingCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_USUARIOS WHERE $COL_SYNC_STATUS != ?",
            arrayOf(SYNC_SYNCED)
        )
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun markAsSynced(localId: Int, serverId: Int?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SYNC_STATUS, SYNC_SYNCED)
            if (serverId != null) put(COL_SERVER_ID, serverId)
        }
        db.update(TABLE_USUARIOS, values, "$COL_ID = ?", arrayOf(localId.toString()))
    }

    fun clearSyncedAndInsertFromServer(users: List<User>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Only clear synced records, keep pending local changes
            db.delete(TABLE_USUARIOS, "$COL_SYNC_STATUS = ?", arrayOf(SYNC_SYNCED))
            for (user in users) {
                val values = ContentValues().apply {
                    put(COL_SERVER_ID, user.serverId ?: user.id)
                    put(COL_NOMBRE, user.nombre)
                    put(COL_APELLIDO, user.apellido)
                    put(COL_EMAIL, user.email)
                    put(COL_USERNAME, user.username)
                    put(COL_PASSWORD, user.password)
                    put(COL_SYNC_STATUS, SYNC_SYNCED)
                }
                db.insert(TABLE_USUARIOS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ==================== Helpers ====================

    private fun cursorToUser(cursor: Cursor): User {
        return User(
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
            apellido = cursor.getString(cursor.getColumnIndexOrThrow(COL_APELLIDO)),
            email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
            password = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
            serverId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_SERVER_ID))) null
                       else cursor.getInt(cursor.getColumnIndexOrThrow(COL_SERVER_ID)),
            username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)) ?: "",
            syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS))
        )
    }
}
