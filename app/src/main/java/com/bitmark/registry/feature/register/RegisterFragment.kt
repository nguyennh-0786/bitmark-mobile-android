/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.register

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.WebViewFragment
import com.bitmark.registry.feature.register.authentication.AuthenticationFragment
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseSigninFragment
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import kotlinx.android.synthetic.main.fragment_register.*
import javax.inject.Inject

class RegisterFragment : BaseSupportFragment() {

    companion object {
        private const val URI = "uri"

        fun newInstance(uri: String? = null): RegisterFragment {
            val bundle = Bundle()
            if (uri != null) bundle.putString(URI, uri)
            val fragment = RegisterFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    internal lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_register

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()
        val uri = arguments?.getString(URI)

        val termOfServiceText = getString(R.string.terms_of_service)
        val policyText = getString(R.string.privacy_policy)
        val text = getString(R.string.by_continuing_you_agree_format).format(
            termOfServiceText,
            policyText
        )
        val spannable = SpannableString(text)
        val termOfServiceClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigator.anim(RIGHT_LEFT).replaceFragment(
                    R.id.layoutContainer,
                    WebViewFragment.newInstance(
                        BuildConfig.TERMS_OF_SERVICE_URL,
                        getString(R.string.terms_of_service),
                        true
                    )
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }

        }

        val policyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigator.anim(RIGHT_LEFT).replaceFragment(
                    R.id.layoutContainer,
                    WebViewFragment.newInstance(
                        BuildConfig.PRIVACY_POLICY_URL,
                        getString(R.string.privacy_policy),
                        true
                    )
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }

        }

        val startTermOfServicePos = text.indexOf(termOfServiceText)
        val startPolicyPos = text.indexOf(policyText)

        spannable.setSpan(
            termOfServiceClickableSpan,
            startTermOfServicePos,
            startTermOfServicePos + termOfServiceText.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            policyClickableSpan,
            startPolicyPos,
            startPolicyPos + policyText.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        tvAppPolicy.text = spannable
        tvAppPolicy.movementMethod = LinkMovementMethod.getInstance()
        tvAppPolicy.highlightColor = Color.TRANSPARENT


        btnAccessAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .replaceFragment(
                    R.id.layoutContainer,
                    RecoveryPhraseSigninFragment.newInstance(uri)
                )
        }

        btnCreateAccount.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT)
                .replaceFragment(
                    R.id.layoutContainer,
                    AuthenticationFragment.newInstance(uri = uri)
                )
        }
    }


}