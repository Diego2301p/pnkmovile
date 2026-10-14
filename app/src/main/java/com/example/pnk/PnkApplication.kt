package com.example.pnk

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Application class for global initialization.
 * Starts network monitoring and automatic sync on connectivity recovery.
 */
class PnkApplication : Application() {

    lateinit var dbHelper: DatabaseHelper
        private set
    lateinit var networkHelper: NetworkHelper
        private set
    lateinit var syncManager: SyncManager
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        instance = this

        dbHelper = DatabaseHelper.getInstance(this)
        UserRepository.init(this)
        networkHelper = NetworkHelper(this)
        syncManager = SyncManager(dbHelper)

        // Seed initial data if database is empty
        seedInitialData()

        // Start network monitoring
        networkHelper.registerNetworkCallback(
            onAvailable = {
                mainHandler.post {
                    Toast.makeText(this, "Conexión restaurada", Toast.LENGTH_SHORT).show()
                    val pendingCount = dbHelper.getPendingCount()
                    if (pendingCount > 0) {
                        Toast.makeText(this, "Sincronizando $pendingCount cambios pendientes...", Toast.LENGTH_SHORT).show()
                        syncManager.syncAll(object : SyncManager.SyncCallback {
                            override fun onProgress(synced: Int, total: Int) {
                                // Progress updates can be shown via notification
                            }
                            override fun onComplete(successCount: Int, failCount: Int) {
                                mainHandler.post {
                                    if (failCount == 0) {
                                        Toast.makeText(this@PnkApplication, "Sincronización completada ($successCount cambios)", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@PnkApplication, "Sincronización parcial: $successCount exitosos, $failCount pendientes", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        })
                    }
                }
            },
            onLost = {
                mainHandler.post {
                    Toast.makeText(this, "Sin conexión - Modo offline activado", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun seedInitialData() {
        if (dbHelper.getAllUsers().isEmpty()) {
            val seedUsers = listOf(
                User("María", "González López", "maria@mail.com", "Pass1234!", syncStatus = DatabaseHelper.SYNC_SYNCED),
                User("Carlos", "Rodríguez Pérez", "carlos@mail.com", "Pass1234!", syncStatus = DatabaseHelper.SYNC_SYNCED),
                User("Ana", "Martínez Silva", "ana@mail.com", "Pass1234!", syncStatus = DatabaseHelper.SYNC_SYNCED),
                User("Pedro", "Sánchez Muñoz", "pedro@mail.com", "Pass1234!", syncStatus = DatabaseHelper.SYNC_SYNCED)
            )
            for (user in seedUsers) {
                dbHelper.insertUser(user)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: PnkApplication? = null

        fun getInstance(): PnkApplication {
            return instance ?: throw IllegalStateException("PnkApplication not initialized")
        }
    }
}
