package com.thing.appbizsdk.familybiz

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputLayout
import com.thing.appbizsdk.familybiz.databinding.FragmentFamilylistBinding
import com.thing.appbizsdk.familybiz.model.FamilyManagerModel
import com.thingclips.smart.family.bean.CreateFamilyRequestBean
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.home.sdk.anntation.HomeStatus
import kotlinx.coroutines.launch
import java.io.Serializable

class FamilyListFragment : Fragment(), FamilyListAdapter.OnFamilyMenuItemClickListener {
    private val viewModel: FamilyManagerModel by activityViewModels()
    private lateinit var binding: FragmentFamilylistBinding
    private var familyListAdapter: FamilyListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFamilylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        familyListAdapter = context?.let { FamilyListAdapter(it) }
        binding.famliyList.layoutManager = LinearLayoutManager(context)
        binding.famliyList.adapter = familyListAdapter
        familyListAdapter?.setOnFooterItemClickListener(this)
        initBackPressed()
        viewModel.getFamilyList()


        lifecycleScope.launch {
            viewModel.mFamilyList.collect {
                updateList(it)
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

    private fun updateList(familyBeans: List<FamilyBean>) {
        familyListAdapter?.notifyDataSetChanged(familyBeans)
    }

    override fun onClickFamily(home: FamilyBean) {
        if (home.familyStatus == HomeStatus.WAITING) {
            showInvitationDialog(home)
        }else {
            view?.let {
                Navigation.findNavController(it)
                    .navigate(R.id.fragment_family_setting, Bundle().apply {
                        putSerializable("family", home as Serializable)
                    })
            }
        }
    }

    private fun showInvitationDialog(home: FamilyBean) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirmation_invatation_tip))
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                home?.homeId?.let { viewModel.processInvitation(it, true) }
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                home?.homeId?.let { viewModel.processInvitation(it, false) }
                d.dismiss()
            }
            .show()
    }

    override fun onAddFamily() {
        showCreateFamilyDialog(requireContext())
    }

    private fun showCreateFamilyDialog(context: Context) {

        val inputLayout = TextInputLayout(context).apply {
            hint = getString(R.string.add_family_name_hint)
            setPadding(32, 0, 32, 0)
        }
        val editText = EditText(context)
        inputLayout.addView(editText)

        val radioGroup = RadioGroup(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 16, 32, 16)
        }
        //120.13,30.27
        val radioButton1 = MaterialRadioButton(context).apply {
            text = "hangzhou"
        }
        //纬度: 40.6643, 经度: -73.9385
        val radioButton2 = MaterialRadioButton(context).apply {
            text = "location 2"
        }
        radioGroup.addView(radioButton1)
        radioGroup.addView(radioButton2)

        val checkBox1 = MaterialCheckBox(context).apply { text = "Living room" }
        val checkBox2 = MaterialCheckBox(context).apply { text = "Bedroom" }
        val checkBox3 = MaterialCheckBox(context).apply { text = "Kitchen" }
        val checkBox4 = MaterialCheckBox(context).apply { text = "Study room" }
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputLayout)
            addView(radioGroup)
            addView(checkBox1)
            addView(checkBox2)
            addView(checkBox3)
            addView(checkBox4)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.add_family))
            .setView(linearLayout)

            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                // 处理确定按钮的点击事件

                val name = editText.text.toString()
                var lon = 120.13
                var lat = 30.27
                var address = radioButton1.text.toString()
                if (radioGroup.checkedRadioButtonId == radioButton2.id) {
                    lon = 40.6643
                    lat = -73.9385
                    address = radioButton2.text.toString()
                }
                var list = ArrayList<String>()
                if (checkBox1.isChecked) {
                    list.add(checkBox1.text.toString())
                }
                if (checkBox2.isChecked) {
                    list.add(checkBox2.text.toString())
                }
                if (checkBox3.isChecked) {
                    list.add(checkBox3.text.toString())
                }
                if (checkBox4.isChecked) {
                    list.add(checkBox4.text.toString())
                }
                val requestBean = CreateFamilyRequestBean(name, lon, lat, address, list)
                viewModel.addFamily(requestBean)
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }.show()

    }

    override fun onJoinFamily() {
        showJoinFamilyDialog(requireContext())

    }

    private fun showJoinFamilyDialog(context: Context) {
        val inputLayout = TextInputLayout(context).apply {
            hint = getString(R.string.join_family_hint)
            setPadding(32, 0, 32, 0)
        }
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputLayout)
        }
        val editText = EditText(context)
        inputLayout.addView(editText)
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.confirm_join_family))
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                val code = editText.text.toString()
                viewModel.joinFamily(code)
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .show()
    }

    private fun initBackPressed() {
        val obj = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!findNavController(this@FamilyListFragment).popBackStack()) {
                    requireActivity().finish()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, obj)
    }
}