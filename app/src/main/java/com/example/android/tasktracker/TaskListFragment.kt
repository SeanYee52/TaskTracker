package com.example.android.tasktracker

import TaskListAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.tasktracker.databinding.FragmentListTaskBinding
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

private const val TAG = "TaskListFragment"

class TaskListFragment : Fragment(){

    private var _binding: FragmentListTaskBinding? = null
    private val binding
        get() = checkNotNull(_binding){
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val taskListViewModel: TaskListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentListTaskBinding.inflate(inflater, container, false)

        binding.taskRecyclerView.layoutManager = LinearLayoutManager(context)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Binds the following function to each item in the navigation bar.
        binding.bottomNavigationView.setOnItemSelectedListener {
            taskListViewModel.setCategory(it.title.toString())
            true
        }

        // Pass the tasks from the view model to the list adapter, and also passes the
        // functions that handles when a task is tapped by the user and when the user
        // creates a new task.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskListViewModel.filteredTasks.collect { tasks ->
                    binding.taskRecyclerView.adapter = TaskListAdapter(
                        tasks,
                        onTaskClicked = { taskID ->
                            findNavController().navigate(
                                TaskListFragmentDirections.showTaskDetail(taskID)
                            )
                        },
                        onCreateTaskClicked = {
                            showNewTask()
                        }
                    )
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Creates the option menu that will contain the new task button and the filter dialog.
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_list_task, menu)
    }

    // Assigns the functions that are relevant to the option menu.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_task -> {
                showNewTask()
                true
            }
            R.id.filter_tasks -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Function to create a new task and to immediately go to its detailed view for editing.
    private fun showNewTask() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newTask = Task(
                id = UUID.randomUUID(),
                title = "",
                date = Date(),
                isCompleted = false,
                isImportant = false,
                inProgress = false,
                category = binding.bottomNavigationView.menu.findItem(binding.bottomNavigationView.selectedItemId).title.toString()
            )
            taskListViewModel.addTask(newTask) // Adds the task to the repository which passes it off to the DAO and database
            findNavController().navigate(
                TaskListFragmentDirections.showTaskDetail(newTask.id)
            )
        }
    }

    // Shows the filter dialog that allows the user to select the kind of tasks that they want to see.
    // These filters are based on the conditions of the task such as isCompleted, isImportant, inProgress.
    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.fragment_dialog_filter, null)
        val showImportantCheckBox = dialogView.findViewById<CheckBox>(R.id.showImportant)
        val showCompletedCheckBox = dialogView.findViewById<CheckBox>(R.id.showCompleted)
        val showInProgressCheckBox = dialogView.findViewById<CheckBox>(R.id.showInProgress)

        // Set initial state of checkboxes from ViewModel.
        val currentFilter = taskListViewModel.filter.value
        showImportantCheckBox.isChecked = currentFilter.showImportant
        showCompletedCheckBox.isChecked = currentFilter.showCompleted
        showInProgressCheckBox.isChecked = currentFilter.showInProgress

        AlertDialog.Builder(requireContext())
            .setTitle("Filter Tasks")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val newFilter = TaskFilter(
                    showImportant = showImportantCheckBox.isChecked,
                    showCompleted = showCompletedCheckBox.isChecked,
                    showInProgress = showInProgressCheckBox.isChecked,
                    applyImportantFilter = showImportantCheckBox.isChecked,
                    applyCompletedFilter = showCompletedCheckBox.isChecked,
                    applyInProgressFilter = showInProgressCheckBox.isChecked,
                    category = currentFilter.category // Preserve the current category filter.
                )
                // Passes the filter data to the view model which in turn will update the list to show
                // only the relevant tasks.
                taskListViewModel.setFilter(newFilter)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}