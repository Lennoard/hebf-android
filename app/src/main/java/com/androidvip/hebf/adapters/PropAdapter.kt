package com.androidvip.hebf.adapters

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*

class PropAdapter(
    private val propList: MutableList<Pair<String, String>>,
    private val activity: Activity
) : RecyclerView.Adapter<PropAdapter.ViewHolder>() {
    private val fab: FloatingActionButton? = activity.findViewById(R.id.buildPropFab)
    private val propPresets = arrayOf(
        "Disable logcat", "Enable navigation bar*",
        "Enable lockscreen rotation", "Enable homescreen rotation*",
        "Toggle adb notification*", "Toggle multitouch",
        "Max number of touches", "Disable anonymous data send",
        "Proximity sensor delay", "Unlock tethering*", "Increase JPG quality",
        "Enable ADB over TCP"
    )

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var key: TextView = v.findViewById(R.id.build_prop_key)
        var value: TextView = v.findViewById(R.id.build_prop_value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_prop, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (key, value) = propList[position]

        holder.key.text = key
        holder.value.text = value

        fab?.setOnLongClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.add)
                .setItems(propPresets) { _, which ->
                    when (which) {
                        0 -> showAddPropDialog("logcat.live", "enable")
                        1 -> showAddPropDialog("qemu.hw.mainkeys", "1")
                        2 -> showAddPropDialog("lockscreen.rot_override", "true")
                        3 -> showAddPropDialog("log.tag.launcher_force_rotate", "VERBOSE")
                        4 -> showAddPropDialog("persist.adb.notify", "1")
                        5 -> showAddPropDialog("ro.product.multi_touch_enabled", "true")
                        6 -> showAddPropDialog("ro.product.max_num_touch", "8")
                        7 -> showAddPropDialog("ro.config.nocheckin", "1")
                        8 -> showAddPropDialog("mot.proximity.delay", "150")
                        9 -> showAddPropDialog("net.tethering.noprovisioning", "true")
                        10 -> showAddPropDialog("ro.media.enc.jpeg.quality", "100")
                        11 -> showAddPropDialog("service.adb.tcp.port", "5555")
                        12 -> showAddPropDialog("ro.opa.eligible_device", "true")
                    }
                }
                .show()

            true
        }

        fab?.setOnClickListener { showAddPropDialog("", "") }

        holder.itemView.setOnClickListener {
            val menuOptions = arrayListOf(
                SheetOption(activity.getString(R.string.edit), "edit", R.drawable.ic_file),
                SheetOption(activity.getString(R.string.delete), "delete", R.drawable.ic_delete)
            )
            val contextSheet = ContextBottomSheet.newInstance(key, menuOptions)
            contextSheet.onOptionClickListener = object : ContextBottomSheet.OnOptionClickListener {
                override fun onOptionClick(tag: String) {
                    contextSheet.dismiss()

                    when (tag) {
                        "edit" -> {
                            if (key.contains("ro.build.version.sdk")) {
                                Toast.makeText(activity, activity.getString(R.string.unsafe_operation), Toast.LENGTH_LONG).show()
                            } else {
                                val propDialog = PropDialog(activity.getString(R.string.edit), key, value)
                                propDialog.setOkListener { _, _ ->
                                    val newProp = propDialog.newKey to propDialog.newValue

                                    RootUtils.executeAsync("sed 's|$key=$value|${newProp.first}=${newProp.second}|g' -i /system/build.prop")
                                    Logger.logInfo("Changed $key value to ${newProp.second}", activity)

                                    propList[position] = newProp
                                    notifyItemChanged(position)
                                    Snackbar.make(fab!!, R.string.done, Snackbar.LENGTH_LONG).show()

                                    val userPrefs = UserPrefs(activity)
                                    val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
                                    if (!achievementsSet.contains("prop")) {
                                        Utils.addAchievement(activity.applicationContext, "prop")
                                        Toast.makeText(
                                            activity,
                                            activity.getString(
                                                R.string.achievement_unlocked,
                                                activity.getString(R.string.achievement_prop)
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                propDialog.show()
                            }
                        }

                        "delete" -> {
                            if (key.contains("ro.build.version.sdk")) {
                                Toast.makeText(activity, activity.getString(R.string.unsafe_operation), Toast.LENGTH_LONG).show()
                            } else {
                                MaterialAlertDialogBuilder(activity)
                                    .setTitle(activity.getString(R.string.warning))
                                    .setIcon(activity.getThemedVectorDrawable(R.drawable.ic_warning))
                                    .setMessage(activity.getString(R.string.confirmation_message))
                                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                    .setPositiveButton(R.string.remove) { _, _ ->
                                        RootUtils.executeAsync("sed 's|$key=$value|#$key=$value|g' -i /system/build.prop")
                                        Logger.logInfo("Removed (commented) $key from build.prop", activity)
                                        Snackbar.make(fab!!, R.string.done, Snackbar.LENGTH_LONG).show()
                                        propList.removeAt(position)
                                        notifyItemRemoved(position)
                                        notifyItemRangeChanged(position, itemCount)
                                    }.show()
                            }
                        }
                    }
                }
            }

            contextSheet.show((activity as AppCompatActivity).supportFragmentManager, "sheet")
        }

    }

    override fun getItemCount(): Int {
        return propList.size
    }

    // Custom instance of PropDialog for adding a prop only
    private fun showAddPropDialog(defaultKey: String, defaultValue: String) {
        val propDialog = PropDialog(activity.getString(R.string.add), defaultKey, defaultValue)
        propDialog.setOkListener(DialogInterface.OnClickListener { _, _ ->
            val newProp = propDialog.newKey to propDialog.newValue

            if (newProp.first.isEmpty()) {
                Utils.showEmptyInputFieldSnackbar(fab)
            } else {
                RootUtils.executeAsync(
                    "chmod +w /system/build.prop && echo ${newProp.first}=${newProp.second} >> /system/build.prop"
                )
                Logger.logInfo("Added ${newProp.first} to build.prop", activity)

                propList.add(newProp)
                notifyItemInserted(propList.size + 1)
                Snackbar.make(fab!!, R.string.done, Snackbar.LENGTH_LONG).show()

                val userPrefs = UserPrefs(activity)
                val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
                if (!achievementsSet.contains("prop")) {
                    Utils.addAchievement(activity.applicationContext, "prop")
                    Toast.makeText(activity, activity.getString(R.string.achievement_unlocked, activity.getString(R.string.achievement_prop)), Toast.LENGTH_LONG).show()
                }
            }
        })

        propDialog.show()
    }

    /**
     * Class used to show a dialog allowing user to edit or add a system property.
     */
    private inner class PropDialog(title: String, key: String, value: String) {
        private val editKey: EditText
        private val editValue: EditText
        private val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(activity)
        private var okListener: DialogInterface.OnClickListener? = null

        internal val newKey: String
            get() = editKey.text.toString().trim()

        internal val newValue: String
            get() = editValue.text.toString().trim()

        init {
            // The base view used in this dialog, contains 2 EditTexts, one for the key, other for the value
            val dialogView = activity.layoutInflater.inflate(R.layout.dialog_build_prop_edit, null)

            builder.setTitle(title)
            builder.setView(dialogView)
            editKey = dialogView.findViewById(R.id.edit_prop_key)
            editValue = dialogView.findViewById(R.id.edit_prop_value)

            // Fill up text fields with prop information
            editKey.setText(key)
            editValue.setText(value)

            builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        }

        /**
         * Do not forget to call this method, it comes away from the constructor so the dev
         * can have a chance to finish instantiating the dialog and (then) be able to use
         * [.getNewKey] and [.getNewValue] in the OK button listener.
         *
         * @param okListener the listener that indicates what to do when the user presses the positive button
         */
        fun setOkListener(okListener: DialogInterface.OnClickListener) {
            this.okListener = okListener
        }

        fun show() {
            checkNotNull(okListener) { "Cannot show a PropDialog if no positive button callback is set for it" }
            builder.setPositiveButton(R.string.done, okListener)
            builder.show()
        }
    }

}