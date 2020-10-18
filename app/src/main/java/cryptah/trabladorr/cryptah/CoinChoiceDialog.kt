package cryptah.trabladorr.cryptah

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class CoinChoiceDialog : DialogFragment() {

    private lateinit var listener: ChoiceDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as ChoiceDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(("$context must implement ChoiceDialogListener"))
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { it ->
            val selectedItems : MutableSet<String> = mutableSetOf()
            val coinset: Set<String> = activity?.getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE)?.getStringSet(getString(R.string.pref_coins) , setOf()) as Set<String>
            selectedItems.addAll(coinset)
            val coinarray : Array<String> = activity?.resources?.getStringArray(R.array.coins_100) as Array<String>
            val checkedarray = coinarray.map {it in coinset}.toTypedArray().toBooleanArray()
            val builder = AlertDialog.Builder(it)

            builder.setTitle(R.string.coin_choice)
                    .setMultiChoiceItems(coinarray, checkedarray
                    ) { _, which, isChecked ->
                        if (isChecked) {
                            selectedItems.add(coinarray[which])
                        } else {
                            selectedItems.remove(coinarray[which])
                        }
                    }
                    .setPositiveButton(R.string.choice_ok
                    ) { _, _ ->
                        val prefs = activity?.getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE)?.edit()
                        prefs?.putStringSet(getString(R.string.pref_coins), selectedItems)
                        prefs?.commit()
                        listener.onDialogPositiveClick(this)
                    }
                    .setNegativeButton(R.string.choice_cancel
                    ) { _, _ ->
                        listener.onDialogNegativeClick(this)
                    }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}