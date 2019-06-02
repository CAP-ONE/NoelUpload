package com.franckrj.noelupload.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.franckrj.noelupload.utils.SafeLiveData

/**
 * ViewModel contenant les diverses informations sur l'historique des uploads.
 */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val _historyEntryRepo: HistoryEntryRepository = HistoryEntryRepository.getInstance(app)
    private val _listOfHistoryEntries: MutableList<HistoryEntryInfos> =
        _historyEntryRepo.getACopyOfListOfHistoryEntries()

    val listOfHistoryEntries: List<HistoryEntryInfos> = _listOfHistoryEntries
    val listOfHistoryEntriesChanges: SafeLiveData<out List<HistoryEntryRepository.HistoryEntryChangeInfos>> =
        _historyEntryRepo.listOfHistoryEntriesChanges

    fun applyHistoryChange(historyEntryChange: HistoryEntryRepository.HistoryEntryChangeInfos): Boolean {
        when (historyEntryChange.changeType) {
            HistoryEntryRepository.HistoryEntryChangeType.NEW -> {
                if (historyEntryChange.changeIndex == _listOfHistoryEntries.size) {
                    _listOfHistoryEntries.add(historyEntryChange.newHistoryEntry)
                    return true
                }
                return false
            }
            HistoryEntryRepository.HistoryEntryChangeType.DELETED -> {
                if (historyEntryChange.changeIndex in (0 until _listOfHistoryEntries.size)) {
                    _listOfHistoryEntries.removeAt(historyEntryChange.changeIndex)
                    return true
                }
                return false
            }
            HistoryEntryRepository.HistoryEntryChangeType.CHANGED,
            HistoryEntryRepository.HistoryEntryChangeType.FINISHED -> {
                if (historyEntryChange.changeIndex in (0 until _listOfHistoryEntries.size)) {
                    _listOfHistoryEntries[historyEntryChange.changeIndex] = historyEntryChange.newHistoryEntry
                    return true
                }
                return false
            }
        }
    }

    fun removeFirstHistoryEntryChange() {
        _historyEntryRepo.removeFirstHistoryEntryChange()
    }
}
