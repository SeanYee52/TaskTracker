import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import java.util.Calendar

class TimePickerFragment : DialogFragment() {

    // Passes the date of the task to the date picker dialog
    private val args: DatePickerFragmentArgs by navArgs()

    // Returns the time and ONLY the time (does not include year, month, and day).
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timePicker = TimePickerDialog.OnTimeSetListener{
            _: TimePicker, hour: Int, minute: Int ->

            val resultTime = (hour * 3600 + minute * 60) * 1000L

            setFragmentResult(REQUEST_KEY_TIME,
                bundleOf(BUNDLE_KEY_TIME to resultTime)
            )
        }
        val calendar = Calendar.getInstance()
        calendar.time = args.taskDate
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(
            requireContext(),
            timePicker,
            initialHour,
            initialMinute,
            DateFormat.is24HourFormat(activity)
        )
    }

    companion object{
        const val REQUEST_KEY_TIME = "REQUEST_KEY_TIME"
        const val BUNDLE_KEY_TIME = "BUNDLE_KEY_TIME"
    }

}