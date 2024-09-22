package com.example.android.tasktracker

import DatePickerFragment
import TimePickerFragment
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.android.tasktracker.databinding.FragmentDetailTaskBinding
import getRotatedBitmap
import getScaledBitmap
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Date

private const val TAG = "TaskDetailFragment"

class TaskDetailFragment: Fragment() {

    private var _binding: FragmentDetailTaskBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val args: TaskDetailFragmentArgs by navArgs()

    private val taskDetailViewModel: TaskDetailViewModel by viewModels {
        TaskDetailViewModelFactory(args.taskID) // Pass the task id to the view model
    }

    // Stores the photo's file name into the task
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        if (didTakePhoto && photoName != null) {
            taskDetailViewModel.updateTask { oldTask ->
                oldTask.copy(photoFileName = photoName)
            }
        }
    }

    private var photoName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =
            FragmentDetailTaskBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensures that the user cannot leave the task's title empty.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.taskTitle.text.isBlank()) {
                    Toast.makeText(activity, "You need to enter a title!", Toast.LENGTH_SHORT).show()
                } else{
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Allows the user to update the task
        binding.apply{
            taskTitle.doOnTextChanged { text, _, _, _ ->
                taskDetailViewModel.updateTask { oldTask ->
                    if(text == null) oldTask
                    else oldTask.copy(title = text.toString())
                }
            }
            taskDescription.doOnTextChanged { text, _, _, _ ->
                taskDetailViewModel.updateTask { oldTask ->
                    oldTask.copy(description = text.toString())
                }
            }
            taskCompleted.setOnCheckedChangeListener { _, isChecked ->
                if (taskProgress.isChecked) {
                    taskProgress.isChecked = false
                }
                taskDetailViewModel.updateTask { oldTask ->
                    oldTask.copy(isCompleted = isChecked)
                }
            }
            taskImportant.setOnCheckedChangeListener { _, isChecked ->
                taskDetailViewModel.updateTask { oldTask ->
                    oldTask.copy(isImportant = isChecked)
                }
            }
            taskProgress.setOnCheckedChangeListener { _, isChecked ->
                if (taskCompleted.isChecked) {
                    taskCompleted.isChecked = false
                }
                taskDetailViewModel.updateTask { oldTask ->
                    oldTask.copy(inProgress = isChecked)
                }
            }
            // Allows the user to take a photo
            taskCamera.setOnClickListener {
                photoName = "IMG_${Date()}.JPG"
                val photoFile = File(requireContext().applicationContext.filesDir,
                    photoName)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.android.tasktracker.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }
            // Zooms in on the photo if the user taps on it
            taskPhoto.setOnClickListener {
                val photoFileName = taskDetailViewModel.task.value?.photoFileName // File name from ViewModel
                photoFileName?.let { fileName ->
                    val zoomPhotoFragment = ZoomPhotoFragment.newInstance(fileName)
                    zoomPhotoFragment.show(parentFragmentManager, "zoom_photo")
                }
            }
            // Checks if the user's device can take photos,
            // if not, disables the camera button
            val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            taskCamera.isEnabled = canResolveIntent(captureImageIntent)
            taskDescription.doOnTextChanged { text, _, _, _ ->
                taskDetailViewModel.updateTask { oldTask ->
                    oldTask.copy(description = text.toString())
                }
            }
            // Allows the user to categorise the task
            categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    view?.let {
                        val category = parent.getItemAtPosition(position).toString()
                        taskDetailViewModel.updateTask { oldTask ->
                            oldTask.copy(category = category)
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }
        }

        // Updates the UI based on the task collected from the mutable state flow which is provided
        // by the view model.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskDetailViewModel.task.collect { task ->
                    task?.let {updateUI(it)}
                }
            }
        }

        // Uses the result from the datepicker dialog fragment and update the task.
        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val newDate =
                bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            taskDetailViewModel.updateTask { it.copy(date = newDate) }
        }

        // Uses the result from the timepicker dialog fragment and update the task.
        // It first sets the original date to the start of the day, and then only
        // adds the time returned by the timepicker to the date.
        setFragmentResultListener(
            TimePickerFragment.REQUEST_KEY_TIME
        ) { _, bundle ->
            val result =
                bundle.getSerializable(TimePickerFragment.BUNDLE_KEY_TIME) as Long
            taskDetailViewModel.updateTask {
                val calendar = Calendar.getInstance().apply {
                    time = it.date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val newDateTimeInMillis = calendar.timeInMillis + result
                it.copy(date = Date(newDateTimeInMillis))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Updates the fragment to show the task correctly
    private fun updateUI(task: Task){
        binding.apply{
            if (taskTitle.text.toString() != task.title){
                taskTitle.setText(task.title)
            }
            if (taskDescription.text.toString() != task.description){
                taskDescription.setText(task.description)
            }
            val string =  DateFormat.format("yyyy, MMMM d, EEEE", task.date).toString() +
                    " at " + DateFormat.getTimeFormat(context).format(task.date)
            taskDateView.text = string
            taskDateButton.setOnClickListener {
                findNavController().navigate(
                    TaskDetailFragmentDirections.selectDate(task.date)
                )
            }
            taskTimeButton.setOnClickListener {
                findNavController().navigate(
                    TaskDetailFragmentDirections.selectTime(task.date)
                )
            }

            val categories = resources.getStringArray(R.array.task_categories) // Assuming you have categories defined in resources
            val position = categories.indexOf(task.category)
            if (position >= 0 && categorySpinner.selectedItemPosition != position) {
                categorySpinner.setSelection(position)
            }

            taskCompleted.isChecked = task.isCompleted
            taskImportant.isChecked = task.isImportant
            taskProgress.isChecked = task.inProgress

            updatePhoto(task.photoFileName)
        }
    }

    // Creates an option for the user to delete the task
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_detail_task, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.delete_task -> {
                deleteTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteTask(){
        Toast.makeText(activity, "Task Deleted!", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            taskDetailViewModel.deleteTask() // Deletes the taks through the view model
            findNavController().navigate(
                TaskDetailFragmentDirections.deleteTask()
            )
        }
    }

    // Checks if the intent (in this case the camera) can be resolved by the device.
    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    // Updates the photo linked to the task by using a scaled bit map.
    // The function used here is located in PictureUtils.kt
    private fun updatePhoto(photoFileName: String?) {
        if (binding.taskPhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            if (photoFile?.exists() == true) {
                binding.taskPhoto.doOnLayout { measuredView ->
                    val rotatedBitmap = getRotatedBitmap(photoFile)
                    val scaledBitmap = rotatedBitmap?.let {
                        getScaledBitmap(it, measuredView.width, measuredView.height)
                    }
                    binding.taskPhoto.setImageBitmap(scaledBitmap)
                    binding.taskPhoto.tag = photoFileName
                }
            } else {
                binding.taskPhoto.setImageBitmap(null)
                binding.taskPhoto.tag = null
            }
        }
    }
}