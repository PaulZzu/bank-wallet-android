package io.horizontalsystems.bankwallet.modules.backup

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import kotlinx.android.synthetic.main.fragment_backup_words_info.*

class BackupInfoFragment : Fragment(), BottomConfirmAlert.Listener {
    private lateinit var viewModel: BackupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupViewModel::class.java)
        }

        buttonBackup.setOnClickListener { viewModel.delegate.showWordsDidClick() }

        buttonLater.setOnClickListener {
            activity?.let {
                val confirmationList = mutableListOf(
                        R.string.backup_words_confirm_alert_wallet_held_on_device,
                        R.string.backup_words_confirm_alert_wallet_restore_with_words,
                        R.string.backup_words_confirm_alert_wallet_device_lock_warning
                )
                BottomConfirmAlert.show(it, confirmationList, this)
            }
        }
    }

    override fun onConfirmationSuccess() {
        viewModel.delegate.laterDidClick()
    }
}

