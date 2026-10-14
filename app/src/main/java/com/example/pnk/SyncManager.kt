package com.example.pnk

import org.json.JSONObject

/**
 * Manages synchronization of pending local changes with the remote MySQL database.
 * Processes inserts, updates, and deletes that were made while offline.
 * Only marks changes as synced after server confirmation (ítem 13).
 */
class SyncManager(private val dbHelper: DatabaseHelper) {

    interface SyncCallback {
        fun onProgress(synced: Int, total: Int)
        fun onComplete(successCount: Int, failCount: Int)
    }

    @Volatile
    private var isSyncing = false

    fun syncAll(callback: SyncCallback) {
        if (isSyncing) return
        isSyncing = true

        val pending = dbHelper.getPendingChanges()
        if (pending.isEmpty()) {
            isSyncing = false
            callback.onComplete(0, 0)
            return
        }

        var successCount = 0
        var failCount = 0
        var processed = 0
        val total = pending.size

        for (user in pending) {
            when (user.syncStatus) {
                DatabaseHelper.SYNC_PENDING_INSERT -> {
                    ApiService.syncInsert(user, object : ApiService.ApiCallback {
                        override fun onSuccess(response: JSONObject) {
                            if (response.optBoolean("success", false)) {
                                val serverId = response.optInt("id", 0)
                                dbHelper.markAsSynced(user.id, if (serverId > 0) serverId else null)
                                successCount++
                            } else {
                                failCount++
                            }
                            processed++
                            callback.onProgress(processed, total)
                            if (processed == total) {
                                isSyncing = false
                                callback.onComplete(successCount, failCount)
                            }
                        }

                        override fun onError(message: String) {
                            failCount++
                            processed++
                            callback.onProgress(processed, total)
                            if (processed == total) {
                                isSyncing = false
                                callback.onComplete(successCount, failCount)
                            }
                        }
                    })
                }

                DatabaseHelper.SYNC_PENDING_UPDATE -> {
                    ApiService.syncUpdate(user, object : ApiService.ApiCallback {
                        override fun onSuccess(response: JSONObject) {
                            if (response.optBoolean("success", false)) {
                                dbHelper.markAsSynced(user.id, user.serverId)
                                successCount++
                            } else {
                                failCount++
                            }
                            processed++
                            callback.onProgress(processed, total)
                            if (processed == total) {
                                isSyncing = false
                                callback.onComplete(successCount, failCount)
                            }
                        }

                        override fun onError(message: String) {
                            failCount++
                            processed++
                            callback.onProgress(processed, total)
                            if (processed == total) {
                                isSyncing = false
                                callback.onComplete(successCount, failCount)
                            }
                        }
                    })
                }

                DatabaseHelper.SYNC_PENDING_DELETE -> {
                    ApiService.syncDelete(user, object : ApiService.ApiCallback {
                        override fun onSuccess(response: JSONObject) {
                            if (response.optBoolean("success", false)) {
                                // Solo eliminar permanentemente después de confirmación del servidor
                                dbHelper.deleteUserPermanently(user.id)
                                successCount++
                            } else {
                                failCount++
                            }
                            processed++
                            callback.onProgress(processed, total)
                            if (processed == total) {
                                isSyncing = false
                                callback.onComplete(successCount, failCount)
                            }
                        }

                        override fun onError(message: String) {
                            // Conservar pendiente para reintentar (ítem 13)
                            failCount++
                            processed++
                            callback.onProgress(processed, total)
                            if (processed == total) {
                                isSyncing = false
                                callback.onComplete(successCount, failCount)
                            }
                        }
                    })
                }
            }
        }
    }

    fun hasPendingChanges(): Boolean {
        return dbHelper.getPendingCount() > 0
    }

    fun getPendingCount(): Int {
        return dbHelper.getPendingCount()
    }
}
