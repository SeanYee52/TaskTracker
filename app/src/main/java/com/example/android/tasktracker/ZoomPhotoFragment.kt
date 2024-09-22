package com.example.android.tasktracker

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.android.tasktracker.databinding.FragmentPhotoZoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ZoomPhotoFragment : DialogFragment() {

    private var _binding: FragmentPhotoZoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())

        // Disables the white background of the dialog.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Load the image into the ImageView
        val imageUrl = arguments?.getString(ARG_IMAGE_URL) ?: ""
        loadImageFromFile(imageUrl)

        // Adjust dialog size to wrap content
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =
            FragmentPhotoZoomBinding.inflate(layoutInflater, container, false)
        return _binding!!.root
    }

    private fun loadImageFromFile(fileName: String) {
        lifecycleScope.launch {
            val file = File(requireContext().filesDir, fileName) // Adjust the directory if needed
            val exif = ExifInterface(file.absolutePath)
            val orientation =
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            val bitmap = withContext(Dispatchers.IO) {
                try {

                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
            }

            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    binding.photoView.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true))
                }
            } else {
                Log.e("ZoomPhotoFragment", "Failed to load image from file: $fileName")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): ZoomPhotoFragment {
            val args = Bundle().apply {
                putString(ARG_IMAGE_URL, imageUrl)
            }
            return ZoomPhotoFragment().apply {
                arguments = args
            }
        }
    }
}