package com.wavesplatform.wallet.v2.ui.auth.passcode.enter

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.text.InputType
import android.util.Log
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.mtramin.rxfingerprint.RxFingerprint
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v1.data.access.AccessState
import com.wavesplatform.wallet.v1.data.auth.IncorrectPinException
import com.wavesplatform.wallet.v1.ui.customviews.ToastCustom
import com.wavesplatform.wallet.v1.util.ViewUtils
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.ui.auth.fingerprint.FingerprintAuthDialogFragment
import com.wavesplatform.wallet.v2.ui.auth.new_account.NewAccountActivity
import com.wavesplatform.wallet.v2.ui.auth.passcode.enter.use_account_password.UseAccountPasswordActivity
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.custom.PassCodeEntryKeypad
import com.wavesplatform.wallet.v2.util.launchActivity
import kotlinx.android.synthetic.main.activity_enter_passcode.*
import pers.victor.ext.click
import javax.inject.Inject


class EnterPasscodeActivity : BaseActivity(), EnterPasscodeView{
    @Inject
    @InjectPresenter
    lateinit var presenter: EnterPasscodePresenter
    private lateinit var fingerprintDialog: FingerprintAuthDialogFragment

    @ProvidePresenter
    fun providePresenter(): EnterPasscodePresenter = presenter

    override fun configLayoutRes() = R.layout.activity_enter_passcode


    companion object {
        const val MAX_AVAILABLE_TIMES = 5
        const val KEY_PIN_CODE = "pin_code"
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, View.OnClickListener { onBackPressed() }, true,
                icon = R.drawable.ic_toolbar_back_black)

        text_use_acc_password.click {
            launchActivity<UseAccountPasswordActivity> {  }
        }

        pass_keypad.isFingerprintAvailable(
                RxFingerprint.isAvailable(this)
                        && AccessState.getInstance().isUseFingerPrint)

        pass_keypad.attachDots(pdl_dots)
        pass_keypad.setPadClickedListener(
                object : PassCodeEntryKeypad.OnPinEntryPadClickedListener {
                    override fun onPassCodeEntered(passCode: String) {
                        validate(passCode)
                    }

                    override fun onFingerprintClicked() {
                        if (AccessState.getInstance().isUseFingerPrint) {
                            showFingerPrint()
                        }
                    }
                })

        if (RxFingerprint.isAvailable(this)
                && AccessState.getInstance().isUseFingerPrint) {
            fingerprintDialog = FingerprintAuthDialogFragment.newInstance()
            fingerprintDialog.setFingerPrintDialogListener(
                    object : FingerprintAuthDialogFragment.FingerPrintDialogListener {
                        override fun onSuccessRecognizedFingerprint(decrypted: String) {
                            validate(decrypted)
                        }
                    })
            showFingerPrint()
        }
    }

    fun validate(passCode: String) {
        showProgressBar(true)
        AccessState.getInstance().validatePin(passCode).subscribe({ password ->
            AccessState.getInstance().removePinFails()
            showProgressBar(false)
            val data = Intent()
            data.putExtra(NewAccountActivity.KEY_INTENT_PASSWORD, password)
            setResult(Constants.RESULT_OK, data)
            finish()
        }, { err ->
            if (err !is IncorrectPinException) {
                Log.e(javaClass.simpleName, "Failed to validate pin", err)
            } else {
                AccessState.getInstance().incrementPinFails()
                checkPinFails()
            }
            showProgressBar(false)
            finish()
        })
    }

    private fun showFingerPrint() {
        fingerprintDialog.isCancelable = false;
        fingerprintDialog.show(fragmentManager, "fingerprintDialog")
    }

    private fun checkPinFails() {
        val fails = AccessState.getInstance().pinFails
        if (fails >= MAX_AVAILABLE_TIMES) {
            ToastCustom.makeText(this@EnterPasscodeActivity,
                    getString(R.string.pin_4_strikes),
                    ToastCustom.LENGTH_SHORT,
                    ToastCustom.TYPE_ERROR)
            showRequestPasswordDialog()
        }
    }

    override fun onBackPressed() {
        setResult(Constants.RESULT_CANCELED)
        finish()
    }

    private fun showRequestPasswordDialog() {
        val password = AppCompatEditText(this)
        password.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        password.hint = getString(R.string.password_entry)

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.pin_4_strikes))
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, password))
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    // todo viewModel.getAppUtil().restartApp()
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val passCode = password.text.toString()
                    if (passCode.isNotEmpty()) {
                        validate(passCode)
                    } else {
                        password.error = getString(R.string.invalid_password_too_short)
                    }

                }.show()
    }
}
