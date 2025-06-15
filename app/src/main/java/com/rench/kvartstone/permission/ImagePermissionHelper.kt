package com.rench.kvartstone.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


class ImagePermissionHelper(
    private val fragment: Fragment,
    private val onGranted: () -> Unit,
    private val onDenied: () -> Unit = {}
) {

    private val launcher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted() else onDenied()
    }

    fun request() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(fragment.requireContext(), permission)
                    == PackageManager.PERMISSION_GRANTED -> {
                onGranted()
            }
            fragment.shouldShowRequestPermissionRationale(permission) -> {

                showRationaleDialog(permission)
            }
            else -> launcher.launch(permission)
        }
    }

    private fun showRationaleDialog(permission: String) {
        androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
            .setTitle("Permission Required")
            .setMessage("This app needs storage permission to select custom card images.")
            .setPositiveButton("Grant") { _, _ ->
                launcher.launch(permission)
            }
            .setNegativeButton("Cancel") { _, _ ->
                onDenied()
            }
            .show()
    }
}
