package com.bitmark.registry.feature.sync

import android.util.Log
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.google_drive.GoogleDriveService
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.UniqueConcurrentLinkedDeque
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

class AssetSynchronizer @Inject constructor(
    private val googleDriveService: GoogleDriveService,
    private val bitmarkRepo: BitmarkRepository,
    private val accountRepo: AccountRepository,
    private val realtimeBus: RealtimeBus
) {

    companion object {
        private const val TAG = "AssetSynchronizer"
    }

    private val taskQueue = UniqueConcurrentLinkedDeque<Completable>()

    private val isProcessing = AtomicBoolean(false)

    private val isPaused = AtomicBoolean(false)

    private var compositeDisposable = CompositeDisposable()

    private var taskProcessListener: TaskProcessListener? = null

    fun setTaskProcessingListener(listener: TaskProcessListener?) {
        this.taskProcessListener = listener
    }

    fun start() {
        Log.d(TAG, "starting...")
        googleDriveService.setServiceReadyListener(object :
            GoogleDriveService.ServiceReadyListener {
            override fun onReady() {
                resume()
            }

            override fun onPause() {
                pause()
            }

        })

        realtimeBus.assetFileSavedPublisher.subscribe(this) { assetId ->
            Log.d(TAG, "asset file save for $assetId, process to upload")
            process(assetId, upload(assetId))
        }

        realtimeBus.assetsSavedPublisher.subscribe(this) { p ->
            val isNewRec = p.second
            if (isNewRec) {
                val assetId = p.first.id
                process(assetId, download(assetId))
            }
        }

        googleDriveService.start()

    }

    fun stop() {
        compositeDisposable.dispose()
        realtimeBus.unsubscribe(this)
        googleDriveService.setServiceReadyListener(null)
        taskQueue.clear()
        isProcessing.set(false)
        isPaused.set(false)
        googleDriveService.stop()
        Log.d(TAG, "stopped")
    }

    fun resume() {
        Log.d(TAG, "resume")
        isPaused.set(false)
        taskQueue.addFirst(element = sync())
        execute()
    }

    fun pause() {
        isPaused.set(true)
        Log.d(TAG, "paused")
    }

    private fun process(assetId: String, task: Completable) {
        if (isProcessing.get() || isPaused.get()) {
            if (taskQueue.add(assetId, task)) {
                Log.d(TAG, "added task for $assetId to the pending queue")
            }
            return
        }
        Log.d(TAG, "start processing for asset id $assetId...")
        subscribe(task.doOnSubscribe { isProcessing.set(true) }
            .doAfterTerminate {
                isProcessing.set(false)
                execute()
            }.doOnDispose {
                isProcessing.set(false)
            }.subscribe(
                { taskProcessListener?.onSuccess() },
                { e ->
                    if (e is CompositeException) {
                        e.exceptions.forEach { ex ->
                            taskProcessListener?.onError(ex)
                            Log.e(
                                TAG,
                                "${ex.javaClass}-${ex.message}"
                            )
                        }
                    } else {
                        taskProcessListener?.onError(e)
                        Log.e(TAG, "${e.javaClass}-${e.message}")
                    }
                })
        )
    }

    private fun sync() = Completable.mergeArrayDelayError(download(), upload())

    private fun execute() {
        if (taskQueue.isEmpty()) return
        val task = taskQueue.poll()
        process(task.first, task.second)
    }

    private fun download(assetId: String) =
        getAccountNumber().flatMapCompletable { accountNumber ->
            bitmarkRepo.checkAssetFile(accountNumber, assetId)
                .map { p -> p.second != null }.flatMapCompletable { existing ->
                    if (existing) {
                        Log.d(TAG, "local file for asset $assetId is existing")
                        Completable.complete()
                    } else {
                        googleDriveService.listAppDataFiles(
                            folderName = accountNumber,
                            partialName = assetId
                        ).flatMapCompletable { files ->
                            if (files.isEmpty()) {
                                Log.d(
                                    TAG,
                                    "remote file for asset $assetId is not existing"
                                )
                                Completable.complete()
                            } else {
                                val file = files[0]
                                val fileId = file.id
                                val parsedName =
                                    parseCloudStorageFileName(file.name)
                                Log.d(
                                    TAG,
                                    "start downloading file id $fileId, name ${file.name}..."
                                )
                                googleDriveService.download(fileId)
                                    .flatMapCompletable { content ->
                                        Log.d(
                                            TAG,
                                            "downloaded file id $fileId with size ${content.size / 1024} KB"
                                        )
                                        bitmarkRepo.saveAssetFile(
                                            accountNumber,
                                            parsedName.first,
                                            parsedName.second,
                                            content
                                        ).ignoreElement()
                                    }
                            }
                        }
                    }
                }
        }

    private fun download() =
        determineDownloadAssets().flatMapCompletable { assetIds ->
            if (assetIds.isEmpty()) {
                Completable.complete()
            } else {
                val downloadStreams = mutableListOf<Completable>()

                assetIds.forEach { assetId ->
                    downloadStreams.add(download(assetId))
                }

                Completable.mergeDelayError(downloadStreams)
            }
        }

    private fun upload(assetId: String) =
        getAccountNumber().flatMapCompletable { accountNumber ->
            bitmarkRepo.checkAssetFile(accountNumber, assetId)
                .flatMapCompletable { p ->
                    val assetId = p.first
                    val file = p.second
                    if (file == null) {
                        Log.d(TAG, "local file for asset $assetId is null")
                        Completable.complete()
                    } else {
                        googleDriveService.checkExistingFile(
                            accountNumber,
                            assetId
                        ).flatMapCompletable { existing ->
                            if (existing) {
                                Log.d(
                                    TAG,
                                    "remote file for asset id $assetId is existing"
                                )
                                Completable.complete()
                            } else {
                                val fileName =
                                    canonicalCloudStorageFileName(
                                        assetId,
                                        file.name
                                    )
                                Log.d(
                                    TAG,
                                    "start uploading $fileName with size ${file.length() / 1024} KB ..."
                                )
                                googleDriveService.upload(
                                    fileName,
                                    accountNumber,
                                    file.absolutePath
                                ).doAfterSuccess {
                                    Log.d(
                                        TAG,
                                        "uploaded file id ${it.id} with size ${file.length() / 1024} KB"
                                    )
                                }.ignoreElement()
                            }
                        }
                    }
                }
        }

    private fun upload() =
        determineUploadAssets().flatMapCompletable { assetIds ->
            if (assetIds.isEmpty()) {
                Completable.complete()
            } else {
                val uploadStreams = mutableListOf<Completable>()

                assetIds.forEach { assetId ->
                    uploadStreams.add(upload(assetId))
                }

                Completable.mergeDelayError(uploadStreams)
            }
        }

    private fun determineUploadAssets() =
        getAccountNumber().flatMap { accountNumber ->
            Single.zip(
                listCloudStoredAssetIds(accountNumber),
                listLocalStoredAssetIdsStream(accountNumber),
                BiFunction<List<String>, List<String>, List<String>> { remote, local ->
                    val uploadAssetIds = mutableListOf<String>()
                    local.forEach { id ->
                        if (!remote.contains(id)) uploadAssetIds.add(
                            id
                        )
                    }
                    uploadAssetIds.toList()
                })
        }

    private fun determineDownloadAssets() =
        getAccountNumber().flatMap { accountNumber ->
            Single.zip(
                listCloudStoredAssetIds(accountNumber),
                listLocalStoredAssetIdsStream(accountNumber),
                listAssetIdsRefOwnedBitmark(accountNumber),
                Function3<List<String>, List<String>, List<String>, List<String>> { remoteIds, localIds, needIds ->
                    val expect = mutableListOf<String>()
                    needIds.forEach { id ->
                        if (!localIds.contains(id) && remoteIds.contains(id)) {
                            expect.add(id)
                        }
                    }
                    expect.toList()
                })
        }

    private fun listCloudStoredAssetIds(owner: String) =
        googleDriveService.listAppDataFiles(owner)
            .map { files ->
                if (files.isEmpty()) listOf() else files.map { f ->
                    parseCloudStorageFileName(
                        f.name
                    ).first
                }
            }

    private fun listLocalStoredAssetIdsStream(owner: String) =
        bitmarkRepo.listStoredAssetFile(owner)
            .map { files -> if (files.isEmpty()) listOf() else files.map { f -> f.name } }

    private fun listAssetIdsRefOwnedBitmark(owner: String) =
        bitmarkRepo.listStoredOwnedBitmarks(owner)
            .map { bitmarks ->
                if (bitmarks.isEmpty()) listOf()
                else {
                    bitmarks.distinctBy { b -> b.assetId }
                        .map { b -> b.assetId }
                }
            }

    private fun getAccountNumber() =
        accountRepo.getAccountInfo().map { a -> a.first }

    // return a pair of asset id and file name
    private fun parseCloudStorageFileName(name: String): Pair<String, String> {
        val splitName = name.split("-")
        if (splitName.size < 2) throw IllegalArgumentException("invalid name")
        return Pair(splitName[0], splitName[1])
    }

    private fun canonicalCloudStorageFileName(assetId: String, name: String) =
        "$assetId-$name"

    private fun subscribe(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    interface TaskProcessListener {

        fun onSuccess() {}

        fun onError(e: Throwable) {}
    }

}