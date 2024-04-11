package com.thing.appbizsdk.familybiz.roomdev

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.thing.appbizsdk.familybiz.R
import com.thing.appbizsdk.familybiz.model.FamilyManagerModel
import com.thing.appbizsdk.familybiz.databinding.FragmentRoomdevSettingBinding
import com.thingclips.smart.family.bean.DeviceInRoomBean
import com.thingclips.smart.family.bean.TRoomBean
import kotlinx.coroutines.launch

class RoomDevSettingFragment : Fragment(), RoomDevSettingAdapter.OnAddRemoveListener {

    private var homeId: Long? = 0
    private val viewModel: FamilyManagerModel by activityViewModels()
    private lateinit var binding: FragmentRoomdevSettingBinding
    private var roomAdapter: RoomDevSettingAdapter? = null
    private var settingRoomBean: TRoomBean? = null
    private var isUpdate: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRoomdevSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initView()
        initBackPressed()
        viewModel.mAllDevList.observe(viewLifecycleOwner){
            val noids = ArrayList(it).apply {
                settingRoomBean?.ids?.let { it1 -> removeAll(it1) }
            }
            roomAdapter?.notifyDataChange(settingRoomBean?.ids,noids)
        }
        viewModel.mRefreshRoombean.observe(viewLifecycleOwner){
            settingRoomBean = it
            val newids = ArrayList(viewModel.mAllDevList.value).apply {
                removeAll(it.ids)
            }
            roomAdapter?.notifyDataChange(it?.ids,newids)
            roomAdapter?.updateName(it?.name)
        }
        lifecycleScope.launch {
            viewModel.sortDevInRoomBean.collect{
                if (!NavHostFragment.findNavController(this@RoomDevSettingFragment)
                        .popBackStack()
                ) {
                    requireActivity().finish()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.errorEvent.collect {
                Toast.makeText(
                    requireContext(),
                    it.second,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun initView() {
        binding.recyclerview.layoutManager = LinearLayoutManager(context)
        roomAdapter = RoomDevSettingAdapter(requireContext(),settingRoomBean?.name?:"")
        binding.recyclerview.adapter = roomAdapter
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                roomAdapter?.moveItem(fromPosition, toPosition)
                isUpdate = true
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerview)
        roomAdapter?.mListener = this
    }


    private fun initData() {
        settingRoomBean = arguments?.get("roomBean") as TRoomBean
        homeId = arguments?.getLong("homeId")
        if(viewModel.mAllDevList.value == null){
            homeId?.let { viewModel.getAllDevInRoomList(it) }
        }

    }

    private fun initBackPressed() {
        val obj = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isUpdate) {
                    MaterialAlertDialogBuilder(this@RoomDevSettingFragment.requireContext())
                        .setTitle(getString(R.string.confirm_save))
                        .setMessage(getString(R.string.confirm_save_desc_sortroomindev))
                        .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                            settingRoomBean?.let { roomAdapter?.mInRoomDevList?.let { it1 ->
                                viewModel.sortDevInRoom(it,
                                    it1
                                )
                            } }
                        }
                        .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                            if (!NavHostFragment.findNavController(this@RoomDevSettingFragment)
                                    .popBackStack()
                            ) {
                                requireActivity().finish()
                            }
                        }
                        .show()

                } else {
                    if (!NavHostFragment.findNavController(this@RoomDevSettingFragment)
                            .popBackStack()
                    ) {
                        requireActivity().finish()
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, obj)
    }

    override fun onRemove(position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirmation_delete_tip))
            .setMessage(getString(R.string.confirmation_delete_desc_removeinroom))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                roomAdapter?.mInRoomDevList?.get(position-1)?.let {
                    settingRoomBean?.let { it1 ->
                        viewModel.deleteDevFromRoom(
                            it , it1
                        )
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onAdd(position: Int) {
        val remove: DeviceInRoomBean? = roomAdapter?.mOutRoomDevList?.removeAt(position - 2 -(roomAdapter?.mInRoomDevList?.size?:0) )
        roomAdapter?.notifyItemRemoved(position)
        if (remove != null) {
            roomAdapter?.mInRoomDevList?.add(remove)
        }
        roomAdapter?.notifyItemInserted(roomAdapter?.mInRoomDevList?.size?:0)
        isUpdate = true
    }

    override fun onUpdateName() {
        val context = requireContext()
        val inputLayout = TextInputLayout(context).apply {
            hint = getString(R.string.add_room_hint)
            setPadding(32, 0, 32, 0)
        }
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputLayout)
        }
        val editText = EditText(context)
        inputLayout.addView(editText)
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.update_room_name_tip))
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                val name = editText.text.toString()
                settingRoomBean?.let { viewModel.updateRoomName(it, name) }
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .show()
    }
}