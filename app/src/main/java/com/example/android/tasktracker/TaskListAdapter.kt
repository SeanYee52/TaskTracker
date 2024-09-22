import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.android.tasktracker.R
import com.example.android.tasktracker.Task
import com.example.android.tasktracker.databinding.ListItemNoTaskBinding
import com.example.android.tasktracker.databinding.ListItemTaskBinding
import java.util.UUID

private const val VIEW_TYPE_EMPTY = 0
private const val VIEW_TYPE_NORMAL = 1

sealed class TaskHolder (
    itemView: View
) : RecyclerView.ViewHolder(itemView){

    class NormalTaskHolder(val binding: ListItemTaskBinding) : TaskHolder(binding.root) {
        fun bind(task: Task, onTaskClicked: (taskID: UUID) -> Unit) {
            binding.taskTitle.text = task.title
            binding.taskDate.text = DateFormat.format("EEEE, MMMM d, yyyy", task.date)

            // Displays the tasks based on their status.
            if (task.isImportant) {
                binding.taskTitle.setTextColor(Color.RED)
            }
            if (task.isCompleted) {
                binding.taskTitle.setTextColor(Color.parseColor("#26FD00"))
                if (task.isImportant)
                    binding.taskIcon.setImageResource(R.drawable.ic_green_warning)
                else
                    binding.taskIcon.setImageResource(R.drawable.ic_check)
            }
            if (!task.inProgress && !task.isCompleted) {
                if (task.isImportant) {
                    binding.taskIcon.setImageResource(R.drawable.ic_red_warning)
                } else {
                    binding.taskIcon.setImageResource(R.drawable.ic_yellow_warning)
                }
            } else if (!task.isCompleted) {
                binding.taskIcon.setImageResource(R.drawable.ic_clock_tick)
            }

            // Sets the function that will bring the user to the task's detailed view by tapping
            // on the task itself.
            binding.root.setOnClickListener {
                onTaskClicked(task.id)
            }


        }
    }

    // Is shown when there are no tasks.
    class DefaultHolder(val binding: ListItemNoTaskBinding) : TaskHolder(binding.root) {
        fun bind(onCreateTaskClicked: () -> Unit) {
            // Button that will create a new task and bring the user to the task detailed view
            binding.createTaskButton.setOnClickListener{
                onCreateTaskClicked()
            }
        }
    }

}

class TaskListAdapter(
    private val tasks: List<Task>,
    private val onTaskClicked: (taskID: UUID) -> Unit,
    private val onCreateTaskClicked: () -> Unit
) : RecyclerView.Adapter<TaskHolder>() {

    // Sets the view type, either there are tasks or no tasks
    override fun getItemViewType(position: Int): Int {
        return if (tasks.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    // Based on the view type, create the view holder that will be displayed in the task list fragment
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType){
            VIEW_TYPE_EMPTY -> {
                val binding = ListItemNoTaskBinding.inflate(inflater, parent, false)
                return TaskHolder.DefaultHolder(binding)
            }
            else -> {
                val binding = ListItemTaskBinding.inflate(inflater, parent, false)
                return TaskHolder.NormalTaskHolder(binding)
            }
        }

    }

    // Binds the the functions that were set in the task list fragment to the viewholder elements
    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        if (tasks.isEmpty()) {
            if (holder is TaskHolder.DefaultHolder) {
                holder.bind(onCreateTaskClicked)
            }
        } else {
            val task = tasks[position]
            when (holder) {
                is TaskHolder.NormalTaskHolder -> holder.bind(task, onTaskClicked)
                is TaskHolder.DefaultHolder -> holder.bind(onCreateTaskClicked)
                else -> {}
            }
        }
    }

    override fun getItemCount() : Int {
        return if (tasks.isEmpty()) 1 else tasks.size
    }

}
