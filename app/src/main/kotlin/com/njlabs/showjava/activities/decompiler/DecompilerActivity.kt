/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2018 Niranjan Rajendran
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.njlabs.showjava.activities.decompiler

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.apps.adapters.getSystemBadge
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.decompilers.BaseDecompiler
import com.njlabs.showjava.decompilers.BaseDecompiler.Companion.isAvailable
import com.njlabs.showjava.utils.ktx.sourceDir
import com.njlabs.showjava.utils.ktx.toBundle
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_decompiler.*
import kotlinx.android.synthetic.main.layout_app_list_item.view.*
import kotlinx.android.synthetic.main.layout_pick_decompiler_list_item.view.*
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URI


class DecompilerActivity : BaseActivity() {

    private lateinit var packageInfo: PackageInfo

    @SuppressLint("SetTextI18n")
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_decompiler)

        loadPackageInfoFromIntent()

        if (!::packageInfo.isInitialized) {
            Toast.makeText(context, R.string.cannotDecompileFile, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val apkSize = FileUtils.byteCountToDisplaySize(packageInfo.file.length())

        itemLabel.itemLabel.text = if (packageInfo.isSystemPackage)
            SpannableString(
                TextUtils.concat(
                    packageInfo.label,
                    " ", " ",
                    getSystemBadge(context).toSpannable()
                )
            )
        else
            packageInfo.label

        itemSecondaryLabel.text = "${packageInfo.version} - $apkSize"

        val decompilersValues = resources.getStringArray(R.array.decompilersValues)
        val decompilers = resources.getStringArray(R.array.decompilers)
        val decompilerDescriptions = resources.getStringArray(R.array.decompilerDescriptions)

        decompilersValues.forEachIndexed { index, decompiler ->
            val view = LayoutInflater.from(pickerList.context)
                .inflate(R.layout.layout_pick_decompiler_list_item, pickerList, false)
            view.decompilerName.text = decompilers[index]
            view.decompilerDescription.text = decompilerDescriptions[index]
            view.decompilerItemCard.cardElevation = 1F
            view.decompilerItemCard.setOnClickListener {
                startProcess(it, decompiler, index)
            }
            pickerList.addView(view)
        }

        if (packageInfo.isSystemPackage) {
            systemAppWarning.visibility = View.VISIBLE
            val warning = getString(R.string.systemAppWarning)
            val sb = SpannableStringBuilder(warning)
            val bss = StyleSpan(Typeface.BOLD)
            val iss = StyleSpan(Typeface.ITALIC)
            val nss = StyleSpan(Typeface.NORMAL)
            sb.setSpan(bss, 0, 8, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            sb.setSpan(nss, 8, warning.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            sb.setSpan(iss, 0, warning.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            systemAppWarning.text = sb
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            decompilersUnavailableNotification.visibility = View.VISIBLE
        }

        disposables.add(
            Observable.fromCallable {
                packageInfo.loadIcon(context)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        resources.getDrawable(R.drawable.ic_list_generic, null)
                    } else {
                        resources.getDrawable(R.drawable.ic_list_generic)
                    }
                }
                .subscribe { itemIcon.setImageDrawable(it) }
        )

        assertSourceExistence(true)
    }

    private fun loadPackageInfoFromIntent() {
        if (intent.dataString.isNullOrEmpty()) {
            if (intent.hasExtra("packageInfo")) {
                packageInfo = intent.getParcelableExtra("packageInfo")
            } else {
                Toast.makeText(context, R.string.errorLoadingInputFile, Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            val info = PackageInfo.fromFile(
                context,
                File(URI.create(intent.dataString)).canonicalFile
            )
            if (info != null) {
                packageInfo = info
            }
        }
    }

    override fun onResume() {
        super.onResume()
        assertSourceExistence()
    }

    private fun assertSourceExistence(addListener: Boolean = false) {
        val sourceInfo = SourceInfo.from(sourceDir(packageInfo.name))
        if (addListener) {
            historyCard.setOnClickListener {
                val intent = Intent(context, NavigatorActivity::class.java)
                intent.putExtra("selectedApp", sourceInfo)
                startActivity(intent)
            }
        }
        if (sourceInfo.exists()) {
            historyCard.visibility = View.VISIBLE
            historyInfo.text = FileUtils.byteCountToDisplaySize(sourceInfo.sourceSize)
        } else {
            historyCard.visibility = View.GONE
        }
    }

    private fun startProcess(view: View, decompiler: String, decompilerIndex: Int) {

        if (!isAvailable(decompiler)) {
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.decompilerUnavailable))
                .setMessage(getString(R.string.decompilerUnavailableExplanation))
                .setIcon(R.drawable.ic_error_outline_black)
                .setNegativeButton(android.R.string.ok, null)
                .show()
            return
        }

        val inputMap = hashMapOf(
            "shouldIgnoreLibs" to userPreferences.ignoreLibraries,
            "maxAttempts" to userPreferences.maxAttempts,
            "chunkSize" to userPreferences.chunkSize,
            "memoryThreshold" to userPreferences.memoryThreshold,
            "keepIntermediateFiles" to userPreferences.keepIntermediateFiles,
            "decompiler" to decompiler,
            "name" to packageInfo.name,
            "label" to packageInfo.label,
            "inputPackageFile" to packageInfo.filePath,
            "type" to packageInfo.type.ordinal
        )

        BaseDecompiler.start(inputMap)

        firebaseAnalytics.logEvent(
            Constants.EVENTS.SELECT_DECOMPILER, hashMapOf(
                FirebaseAnalytics.Param.VALUE to decompiler
            ).toBundle()
        )

        firebaseAnalytics.logEvent(Constants.EVENTS.DECOMPILE_APP, inputMap.toBundle())

        val i = Intent(this, DecompilerProcessActivity::class.java)
        i.putExtra("packageInfo", packageInfo)
        i.putExtra("decompilerIndex", decompilerIndex)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val options = ActivityOptions
                .makeSceneTransitionAnimation(this, view, "decompilerItemCard")
            startActivity(i, options.toBundle())
        } else {
            startActivity(i)
        }
        finish()
    }
}