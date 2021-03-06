/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.music_claiming

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.data.source.remote.api.error.HttpException
import com.bitmark.registry.feature.*
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.feature.transfer.TransferActivity
import com.bitmark.registry.logging.Event
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.logging.Tracer
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.AssetClaimingModelView
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.view.OptionsDialog
import com.bitmark.registry.util.view.ProgressAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_music_claiming.*
import kotlinx.android.synthetic.main.layout_bitmark_cert.*
import java.io.File
import javax.inject.Inject

class MusicClaimingActivity : BaseAppCompatActivity() {

    companion object {
        private const val BITMARK = "bitmark"

        private const val TAG = "MusicClaimingActivity"

        fun getBundle(
            bitmark: BitmarkModelView
        ): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(BITMARK, bitmark)
            return bundle
        }
    }

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var viewModel: MusicClaimingViewModel

    @Inject
    lateinit var dialogController: DialogController

    @Inject
    lateinit var logger: EventLogger

    private lateinit var progressDialog: ProgressAppCompatDialog

    private lateinit var bitmark: BitmarkModelView

    private lateinit var assetClaiming: AssetClaimingModelView

    override fun layoutRes(): Int = R.layout.activity_music_claiming

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getMusicClaimingInfo(
            bitmark.assetId,
            bitmark.edition
        )
    }

    override fun requestFeatures() {
        super.requestFeatures()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    override fun initComponents() {
        super.initComponents()

        bitmark = intent?.extras?.getParcelable(BITMARK) as BitmarkModelView
        tvAssetId.text = bitmark.assetId
        tvIssuanceDate.text = bitmark.createdAt()

        val bmCertBehavior = BottomSheetBehavior.from(layoutRootCert)
        setBmCertVisibility(bmCertBehavior, false)

        wvContent.settings.javaScriptEnabled = true
        wvContent.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.gone()
                    tvLoading.gone()
                } else {
                    progressBar.visible()
                    tvLoading.visible()
                }
            }
        }

        wvContent.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && request?.isRedirect == true) {
                    view?.loadUrl(request.url.toString())
                    return false
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String?
            ): Boolean {
                view?.loadUrl(url)
                return false
            }
        }

        wvContent.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val height =
                Math.floor((wvContent.contentHeight * wvContent.scale).toDouble())
                    .toInt()
            val webViewHeight = wvContent.measuredHeight
            if (wvContent.scrollY + webViewHeight >= height - 300 && oldScrollY < scrollY) {
                setBmCertVisibility(bmCertBehavior, true)
            } else {
                setBmCertVisibility(bmCertBehavior, false)
            }
        }

        ivClose.setOnClickListener {
            navigator.anim(BOTTOM_UP).finishActivity()
        }

        btnViewBmOpt.setSafetyOnclickListener {
            showOptionsDialog(bitmark)
        }

        layoutRootCert.setOnClickListener {
            val url =
                "${BuildConfig.REGISTRY_WEBSITE}/bitmark/${bitmark.id}?env=app"
            navigator.anim(RIGHT_LEFT).startActivity(
                WebViewActivity::class.java,
                WebViewActivity.getBundle(url, getString(R.string.registry))
            )
        }

        setBtnViewBmOptsEnable(bitmark.isSettled())
    }

    private fun setBtnViewBmOptsEnable(enable: Boolean) {
        if (enable) {
            btnViewBmOpt.setText(R.string.view_bitmark_opt)
            btnViewBmOpt.enable()
        } else {
            btnViewBmOpt.setText(R.string.authenticating_transfer_to_you_three_dot)
            btnViewBmOpt.disable()
        }
    }

    override fun deinitComponents() {
        wvContent.setOnScrollChangeListener(null)
        wvContent.webViewClient = null
        wvContent.webChromeClient = null
        wvContent.reload()
        wvContent.destroy()
        dialogController.dismiss()
        super.deinitComponents()
    }

    private fun setBmCertVisibility(
        behavior: BottomSheetBehavior<*>,
        visible: Boolean
    ) {
        if (visible) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun showOptionsDialog(bitmark: BitmarkModelView) {
        val opts = listOf(
            OptionsDialog.OptionsAdapter.Item(
                R.drawable.ic_play_circle_filled,
                getString(R.string.play_on_streaming_platform)
            ),
            OptionsDialog.OptionsAdapter.Item(
                R.drawable.ic_download,
                getString(R.string.download_property)
            ),
            OptionsDialog.OptionsAdapter.Item(
                R.drawable.ic_swap,
                getString(R.string.transfer_ownership)
            )
        )
        val optionsDialog = OptionsDialog(
            this,
            getString(R.string.bitmark_option),
            true,
            opts
        ) { item ->
            when (item.icon) {
                R.drawable.ic_play_circle_filled -> {
                    val url = bitmark.metadata?.get("playlink")
                        ?: return@OptionsDialog
                    navigator.openBrowser(url)
                }

                R.drawable.ic_download -> {
                    if (bitmark.assetFile != null) {
                        dialogController.alert(
                            "",
                            getString(R.string.you_have_already_downloaded_this_asset)
                        )
                    } else {
                        viewModel.prepareDownload()
                    }
                }

                R.drawable.ic_swap -> {
                    if (bitmark.isPending()) return@OptionsDialog
                    if (bitmark.assetFile == null) {
                        dialogController.confirm(
                            R.string.warning,
                            R.string.to_be_able_to_transfer_this_bitmark,
                            false,
                            R.string.download,
                            { viewModel.prepareDownload() },
                            R.string.cancel
                        )
                    } else {
                        navigator.anim(RIGHT_LEFT).startActivity(
                            TransferActivity::class.java,
                            TransferActivity.getBundle(bitmark)
                        )
                    }
                }
            }
        }

        optionsDialog.show()
    }

    override fun observe() {
        super.observe()

        viewModel.getMusicClaimingInfoLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    assetClaiming = res.data() ?: return@Observer
                    val url =
                        "${BuildConfig.MOBILE_SERVER_ENDPOINT}/api/claim_requests_view/${assetClaiming.assetId}?total=${assetClaiming.limitedEdition}&remaining=${assetClaiming.totalEditionLeft}&edition_number=${assetClaiming.editionNumber
                            ?: "?"}"
                    wvContent.loadUrl(url)
                }

                res.isError() -> {
                    logger.logError(
                        Event.MUSIC_CLAIMING_INFO_ERROR,
                        res.throwable()
                    )
                    dialogController.alert(
                        getString(R.string.error),
                        res.throwable()?.message
                            ?: getString(R.string.unexpected_error)
                    ) { navigator.anim(BOTTOM_UP).finishActivity() }
                }
            }
        })

        viewModel.prepareDownloadLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val data = res.data() ?: return@Observer
                    val accountNumber = data.first
                    val keyAlias = data.second
                    loadAccount(accountNumber, keyAlias) { account ->
                        viewModel.downloadAssetFile(
                            bitmark.assetId,
                            account.accountNumber,
                            account.encKeyPair
                        )
                    }
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "prepare download failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                }
            }
        })

        viewModel.downloadProgressLiveData.observe(this, Observer { progress ->
            progressDialog.setProgress(progress)
        })

        viewModel.downloadAssetLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    dialogController.dismiss(progressDialog)
                    val file = res.data() ?: return@Observer
                    bitmark.assetFile = file
                    shareFile(bitmark.name ?: "", file)
                }

                res.isError() -> {
                    dialogController.dismiss(progressDialog)
                    val e = res.throwable()
                    val errorMessage =
                        if (e is HttpException && e.code == 404) {
                            Tracer.WARNING.log(
                                TAG,
                                "asset is not available to download"
                            )
                            R.string.the_asset_is_not_available
                        } else {
                            Tracer.ERROR.log(
                                TAG,
                                "download asset failed: ${e?.message
                                    ?: "unknown"}"
                            )
                            logger.logError(
                                Event.MUSIC_CLAIMING_DOWNLOAD_ERROR,
                                e
                            )
                            R.string.could_not_download_asset
                        }
                    dialogController.alert(
                        R.string.error,
                        errorMessage,
                        R.string.ok
                    )
                }

                res.isLoading() -> {
                    progressDialog = ProgressAppCompatDialog(
                        this,
                        message = getString(R.string.preparing_to_export)
                    )
                    dialogController.show(progressDialog)
                }
            }
        })

        viewModel.bitmarksSavedLiveData.observe(this, Observer { bitmark ->
            if (bitmark.id != this.bitmark.id) return@Observer
            this.bitmark.status = bitmark.status
            setBtnViewBmOptsEnable(this.bitmark.isSettled())
        })

        viewModel.bitmarkDeletedLiveData.observe(this, Observer { p ->
            if (bitmark.id != p.first) return@Observer
            navigator.anim(BOTTOM_UP).finishActivity()
        })
    }

    private fun loadAccount(
        accountNumber: String,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec = KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
            .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
            .build()
        this.loadAccount(accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { navigator.gotoSecuritySetting() },
            invalidErrorAction = { e ->
                Tracer.ERROR.log(
                    TAG,
                    "biometric authentication is invalidated: ${e?.message}"
                )
                logger.logError(Event.AUTH_INVALID_ERROR, e)
                dialogController.alert(
                    R.string.account_is_not_accessible,
                    R.string.sorry_you_have_changed_or_removed
                ) {
                    navigator.startActivityAsRoot(
                        RegisterContainerActivity::class.java,
                        RegisterContainerActivity.getBundle(recoverAccount = true)
                    )
                }
            })
    }

    private fun shareFile(assetName: String, file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".file_provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_SUBJECT, assetName)
        intent.putExtra(Intent.EXTRA_TEXT, assetName)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        navigator.anim(BOTTOM_UP)
            .startActivity(Intent.createChooser(intent, assetName))
    }

    override fun onBackPressed() {
        navigator.anim(BOTTOM_UP).finishActivity()
        super.onBackPressed()
    }
}