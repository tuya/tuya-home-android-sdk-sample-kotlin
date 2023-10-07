package com.thing.appbizsdk.familybiz.room

import android.content.Context
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
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.thing.appbizsdk.familybiz.model.FamilyManagerModel
import com.thing.appbizsdk.familybiz.R
import com.thing.appbizsdk.familybiz.databinding.FragmentRoomsettingBinding
import com.thingclips.smart.family.bean.TRoomBean
import kotlinx.coroutines.launch

class RoomSettingFragment : Fragment() {
    private var homeId: Long? = 0
    private val viewModel: FamilyManagerModel by activityViewModels()
    private lateinit var binding: FragmentRoomsettingBinding
    private var roomAdapter: RoomListAdapter? = null
    private var isMoved: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRoomsettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerview.layoutManager = LinearLayoutManager(context)
        roomAdapter = RoomListAdapter()
        binding.recyclerview.adapter = roomAdapter

        roomAdapter?.roomItemClickListener = object : IRoomItemClickListener {
            override fun onClick(room: TRoomBean) {
                gotoRoomDevSetting(room)
            }
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                roomAdapter?.moveItem(fromPosition, toPosition)
                isMoved = true
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                MaterialAlertDialogBuilder(this@RoomSettingFragment.requireContext())
                    .setTitle(getString(R.string.confirmation_delete_tip))
                    .setMessage(getString(R.string.confirmation_delete_room_desc))
                    .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                        roomAdapter?.items?.get(viewHolder.adapterPosition)?.let {
                            viewModel.deleteRoom(
                                homeId ?: 0,
                                it?.roomId ?: 0
                            )
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerview)

        binding.fabAddItem.setOnClickListener {
            addRoom(requireContext())
        }
        initBackPressed()
        initData()
        viewModel.mRoomList.observe(viewLifecycleOwner) {
            roomAdapter?.updateItem(it)
        }
        lifecycleScope.launch {
            viewModel.sortRoomBean.collect {
                if (!NavHostFragment.findNavController(this@RoomSettingFragment).popBackStack()) {
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

    private fun gotoRoomDevSetting(room: TRoomBean) {
        view?.let {
            Navigation.findNavController(it)
                .navigate(R.id.fragment_room_dev_setting, Bundle().apply {
                    putSerializable("roomBean", room)
                    homeId?.let { it1 -> putLong("homeId", it1) }
                })
        }
    }

    private fun addRoom(context: Context) {
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
            .setTitle(getString(R.string.add_room_title))
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                val name = editText.text.toString()
                viewModel.addRoom(homeId, name)
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .show()
    }

    private fun initData() {
        homeId = arguments?.getLong("homeId")
        viewModel.getRoomList(homeId)
    }

    private fun initBackPressed() {
        val obj = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isMoved) {
                    MaterialAlertDialogBuilder(this@RoomSettingFragment.requireContext())
                        .setTitle(getString(R.string.confirm_save))
                        .setMessage(getString(R.string.confirm_save_desc_sortroom))
                        .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                            roomAdapter?.items?.let { viewModel.sortRoom(homeId ?: 0, it) }
                        }
                        .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                            if (!NavHostFragment.findNavController(this@RoomSettingFragment)
                                    .popBackStack()
                            ) {
                                requireActivity().finish()
                            }
                        }
                        .show()

                } else {
                    if (!NavHostFragment.findNavController(this@RoomSettingFragment)
                            .popBackStack()
                    ) {
                        requireActivity().finish()
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, obj)
    }
}