package com.example.pnk

import android.content.Context

/**
 * Data class for users with offline sync support.
 * Added id, serverId, username, and syncStatus for SQLite/MySQL synchronization.
 */
data class User(
    var nombre: String,
    var apellido: String,
    var email: String,
    var password: String = "",
    var id: Int = 0,
    var serverId: Int? = null,
    var username: String = "",
    var syncStatus: String = DatabaseHelper.SYNC_SYNCED
)

/**
 * Repository that delegates to DatabaseHelper (SQLite).
 * Provides a unified interface for CRUD operations.
 * All data persists locally and syncs to MySQL when online.
 */
object UserRepository {

    private var _dbHelper: DatabaseHelper? = null

    private val dbHelper: DatabaseHelper
        get() = _dbHelper ?: throw IllegalStateException("UserRepository not initialized. Call init(context) first.")

    fun init(context: Context) {
        _dbHelper = DatabaseHelper.getInstance(context)
    }

    /**
     * Returns all active users from SQLite (excludes pending deletes).
     */
    val users: MutableList<User>
        get() = dbHelper.getAllUsers()

    fun addUser(user: User): Long {
        return dbHelper.insertUser(user)
    }

    fun getUserById(id: Int): User? {
        return dbHelper.getUserById(id)
    }

    fun updateUser(id: Int, nombre: String, apellido: String, email: String) {
        dbHelper.updateUser(id, nombre, apellido, email, DatabaseHelper.SYNC_PENDING_UPDATE)
    }

    fun removeUser(id: Int) {
        dbHelper.markUserForDeletion(id)
    }

    fun deleteUserPermanently(id: Int) {
        dbHelper.deleteUserPermanently(id)
    }

    fun emailExists(email: String, excludeId: Int? = null): Boolean {
        return dbHelper.emailExists(email, excludeId)
    }

    fun usernameExists(username: String, excludeId: Int? = null): Boolean {
        return dbHelper.usernameExists(username, excludeId)
    }

    fun getPendingCount(): Int {
        return dbHelper.getPendingCount()
    }
}
