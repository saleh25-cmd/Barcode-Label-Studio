package com.example.data

import kotlinx.coroutines.flow.Flow

class LabelRepository(
    private val labelDao: LabelDao,
    private val settingsDao: SettingsDao
) {
    val allLabels: Flow<List<LabelItem>> = labelDao.getAllLabels()
    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettingsFlow()

    fun searchLabels(query: String): Flow<List<LabelItem>> = labelDao.searchLabels(query)

    suspend fun insertLabel(label: LabelItem): Long = labelDao.insertLabel(label)

    suspend fun insertBatchLabels(labels: List<LabelItem>) = labelDao.insertLabels(labels)

    suspend fun updateLabel(label: LabelItem) = labelDao.updateLabel(label)

    suspend fun deleteLabel(label: LabelItem) = labelDao.deleteLabel(label)

    suspend fun deleteAllLabels() = labelDao.deleteAllLabels()

    suspend fun getSettings(): AppSettings {
        return settingsDao.getSettings() ?: AppSettings().also {
            settingsDao.saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings)
    }
}
