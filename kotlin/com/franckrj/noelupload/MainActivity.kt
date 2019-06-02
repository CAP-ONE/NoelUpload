package com.franckrj.noelupload

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.franckrj.noelupload.databinding.ActivityMainBinding
import com.franckrj.noelupload.history.HistoryEntryInfos
import com.franckrj.noelupload.history.HistoryListAdapter
import com.franckrj.noelupload.history.HistoryViewModel
import com.franckrj.noelupload.upload.UploadViewModel
import androidx.recyclerview.widget.SimpleItemAnimator
import com.franckrj.noelupload.history.HistoryEntryRepository
import com.franckrj.noelupload.utils.Utils

/**
 * Activité principale pour consulter l'historique des uploads et upload des nouvelles images.
 */
class MainActivity : AbsToolbarActivity() {
    companion object {
        private const val CHOOSE_IMAGE_REQUEST_CODE: Int = 38
        private const val ACTION_PICK_IMAGE: String = "com.franckrj.noelupload.PICK_IMAGE"
    }

    //private val uploadViewModel: UploadViewModel by viewModels()
    //todo check ktx pour ça
    private lateinit var _historyViewModel: HistoryViewModel
    private lateinit var _uploadViewModel: UploadViewModel
    private val _adapterForHistory = HistoryListAdapter()

    /**
     * Callback appelé lorsqu'un item est cliqué dans la liste, copie le lien direct associé dans
     * le presse-papier ou affiche une erreur.
     */
    private fun itemInHistoryListClicked(historyEntryInfos: HistoryEntryInfos) {
        val linkOfImage: String = Utils.noelshackToDirectLink(historyEntryInfos.imageBaseLink)

        if (Utils.checkIfItsANoelshackImageLink(linkOfImage)) {
            Utils.putStringInClipboard(this, linkOfImage)
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.invalid_link, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Lance l'upload de [uri] ou affiche un message d'erreur.
     */
    private fun startUploadThisImage(uri: Uri?) {
        if (uri != null) {
            if (!_uploadViewModel.startUploadThisImage(uri)) {
                Toast.makeText(this, R.string.upload_already_running, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, R.string.invalid_file, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Consomme le [currIntent], s'il est du bon type initialise la nouvelle image à upload s'il est valide,
     * sinon s'il est invalide affiche un message d'erreur.
     */
    private fun consumeIntent(currIntent: Intent?) {
        if (currIntent != null) {
            if (currIntent.action == Intent.ACTION_SEND && currIntent.hasExtra(Intent.EXTRA_STREAM)) {
                val newUri: Uri? = currIntent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri

                startUploadThisImage(newUri)
            } else if (currIntent.action == ACTION_PICK_IMAGE) {
                pickAnImage()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        _historyViewModel = ViewModelProviders.of(this).get(HistoryViewModel::class.java)
        _uploadViewModel = ViewModelProviders.of(this).get(UploadViewModel::class.java)
        binding.lifecycleOwner = this
        binding.activity = this

        initToolbar(binding.toolbarHistory as Toolbar, homeIsBack = false, displayHome = false)

        _adapterForHistory.listOfHistoryEntries = _historyViewModel.listOfHistoryEntries
        _adapterForHistory.itemClickedCallback = ::itemInHistoryListClicked
        _adapterForHistory.notifyDataSetChanged()

        binding.uploadhistoryListHistory.layoutManager = GridLayoutManager(this, 3)
        binding.uploadhistoryListHistory.adapter = _adapterForHistory
        (binding.uploadhistoryListHistory.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        _historyViewModel.listOfHistoryEntriesChanges.observe(
            this,
            Observer { newListOfHistoryEntriesChanges: List<HistoryEntryRepository.HistoryEntryChangeInfos> ->
                while (newListOfHistoryEntriesChanges.isNotEmpty()) {
                    val historyEntryChange: HistoryEntryRepository.HistoryEntryChangeInfos =
                        newListOfHistoryEntriesChanges.first()

                    if (_historyViewModel.applyHistoryChange(historyEntryChange)) {
                        when (historyEntryChange.changeType) {
                            HistoryEntryRepository.HistoryEntryChangeType.NEW -> {
                                _adapterForHistory.notifyItemInserted(historyEntryChange.changeIndex)
                                binding.uploadhistoryListHistory.scrollToPosition(_adapterForHistory.itemCount - 1)
                            }
                            HistoryEntryRepository.HistoryEntryChangeType.DELETED -> {
                                _adapterForHistory.notifyItemRemoved(historyEntryChange.changeIndex)
                            }
                            HistoryEntryRepository.HistoryEntryChangeType.CHANGED -> {
                                _adapterForHistory.notifyItemChanged(historyEntryChange.changeIndex)
                            }
                            HistoryEntryRepository.HistoryEntryChangeType.FINISHED -> {
                                _adapterForHistory.notifyItemChanged(historyEntryChange.changeIndex)
                            }
                        }
                    }
                    _historyViewModel.removeFirstHistoryEntryChange()
                }
            })

        if (savedInstanceState == null) {
            binding.uploadhistoryListHistory.scrollToPosition(_adapterForHistory.itemCount - 1)
            consumeIntent(intent)
        }
    }

    /**
     * Intercepte les nouveaux intent reçus et les envoit à [consumeIntent].
     */
    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)

        consumeIntent(newIntent)
    }

    /**
     * Intercepte le retour de l'intent pour sélectionner une image et upload son résultat, ou
     * affiche une erreur si le retour est invalide.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val newUri: Uri? = data?.data

        if (requestCode == CHOOSE_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startUploadThisImage(newUri)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Lance un intent permettant de sélectionner une image ou affiche une erreur si la sélection est impossible.
     */
    fun pickAnImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        try {
            startActivityForResult(intent, CHOOSE_IMAGE_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.file_manager_not_found, Toast.LENGTH_LONG).show()
        }
    }
}
