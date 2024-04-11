package com.thing.appbizsdk.familybiz.setting

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
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
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputLayout
import com.thing.appbizsdk.familybiz.model.FamilyManagerModel
import com.thing.appbizsdk.familybiz.R
import com.thing.appbizsdk.familybiz.databinding.FragmentFamilysettingBinding
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.bean.MemberBean
import com.thingclips.smart.home.sdk.anntation.MemberRole
import kotlinx.coroutines.launch

class FamilySettingFragment: Fragment(), OnItemClickListener, OnHeadFootClickListener {
    private val viewModel: FamilyManagerModel by activityViewModels()
    private lateinit var binding: FragmentFamilysettingBinding
    private var familyAdapter: FamilySettingAdapter? = null
    private var settingFamilybean:FamilyBean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFamilysettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerview.layoutManager = LinearLayoutManager(context)
        familyAdapter = FamilySettingAdapter(requireContext())
        binding.recyclerview.adapter = familyAdapter
        familyAdapter?.setOnItemClick(this)
        familyAdapter?.setOnHeadFootClickListener(this)
        initBackPressed()
        initData()

        viewModel.mMemberList.observe(viewLifecycleOwner){
            familyAdapter?.notifyDataSetChanged(it,viewModel.isAdmin)
        }
        viewModel.mRefreshFamilybean.observe(viewLifecycleOwner) {
            settingFamilybean = it
            familyAdapter?.updateHead( it)
        }
        lifecycleScope.launch {
            viewModel.delFamilybean.collect {
                NavHostFragment.findNavController(this@FamilySettingFragment).popBackStack()
            }
        }
        lifecycleScope.launch {
            viewModel.mInvitationMessageBean.collect{
                Toast.makeText(
                    requireContext(),
                    it.invitationMsgContent,
                    Toast.LENGTH_LONG
                )
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

    private fun initData() {
        settingFamilybean = arguments?.get("family") as FamilyBean
        familyAdapter?.updateHead( settingFamilybean)
        settingFamilybean?.homeId?.let {
            viewModel.getFamilyDetail(it)
            viewModel.getMemberList(it)
        }

    }


    override fun onItemClick(memberBean: MemberBean?) {
        if(viewModel.mCurMemberBen == null || memberBean== null){
            return
        }
        if(viewModel.isAdmin && (viewModel.mCurMemberBen!!.role <= memberBean!!.role)){
            Toast.makeText(
                requireContext(),
                getString(R.string.member_can_not_delete_tip),
                Toast.LENGTH_LONG
            )
            return
        }
        showUpdateMemberDialog(requireContext(),memberBean)

    }

    override fun onLongItemClick(memberBean: MemberBean?) {
        if(viewModel.mCurMemberBen == null || memberBean== null){
            return
        }
        if(viewModel.isAdmin && (viewModel.mCurMemberBen!!.role <= memberBean!!.role)){
            Toast.makeText(
                requireContext(),
                getString(R.string.member_can_not_delete_tip),
                Toast.LENGTH_LONG
            )
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_delete_member))
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                settingFamilybean?.homeId?.let { viewModel.deleteMember(it,memberBean) }
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .show()
    }

    override fun onFamilyNameClick() {
        showUpdateFamilyDialog(requireContext())
    }

    override fun onRoomManageClick() {
        view?.let { Navigation.findNavController(it).navigate( R.id.fragment_room_setting, Bundle().apply {
            settingFamilybean?.homeId?.let { it1 -> putLong("homeId", it1) }
        }) }
    }

    override fun onLeaveFamilyClick() {
       if(viewModel.isOwn ){
           if(viewModel.isCanTransferOwn ) {
               Toast.makeText(
                   activity,
                   getString(R.string.family_can_not_delete_tip),
                   Toast.LENGTH_LONG
               ).show()
               return
           }else{
               viewModel.dismissfamily(settingFamilybean?.homeId)
           }
       }else {
           viewModel.leavefamily(settingFamilybean?.homeId)
       }
    }

    override fun onAddMemberClick() {
       showAddMemberDialog()
    }



    override fun onInvationMemberClick() {
        viewModel.addInvationMember(settingFamilybean?.homeId)
    }

    private fun showUpdateFamilyDialog(context: Context) {
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
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputLayout)
            addView(radioGroup)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.update_family))
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                val name = editText.text.toString()
                var lon = 120.13
                var lat = 30.27
                var address = radioButton1.text.toString()
                if (radioGroup.checkedRadioButtonId == radioButton2.id) {
                    lon = 40.6643
                    lat = -73.9385
                    address = radioButton2.text.toString()
                }
                viewModel.updateFamily(name,lon,lat,address,settingFamilybean)
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .show()
    }
    private fun showAddMemberDialog() {
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.add_member_name_hint)
            setPadding(32, 0, 32, 0)
        }
        val editText = EditText(context)
        inputLayout.addView(editText)
        val inputLayout2 = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.add_member_account_hint)
            setPadding(32, 0, 32, 0)
        }
        val editText2 = EditText(context)
        inputLayout2.addView(editText2)
        val radioGroup = RadioGroup(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 16, 32, 16)
        }
        val radioButton1 = MaterialRadioButton(requireContext()).apply {
            text = "admin"
        }
        val radioButton2 = MaterialRadioButton(requireContext()).apply {
            text = "common"
        }

        radioGroup.addView(radioButton1)
        radioGroup.addView(radioButton2)
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputLayout)
            addView(inputLayout2)
            addView(radioGroup)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.update_member))
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                val name = editText.text.toString()
                val account = editText2.text.toString()
                var role = MemberRole.ROLE_CUSTOM
                if (radioGroup.checkedRadioButtonId == radioButton1.id) {
                    role = MemberRole.ROLE_ADMIN
                }
                val bean = MemberBean()
                var code: String? = "86"
                if (!TextUtils.isEmpty(viewModel.mCurMemberBen?.countryCode)) {
                    code = viewModel.mCurMemberBen?.countryCode
                }
                bean.memberName = name
                bean.homeId = settingFamilybean?.homeId!!
                bean.countryCode = code
                bean.account = account
                bean.role = role
                viewModel.addMember(bean)
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .show()

    }
    private fun showUpdateMemberDialog(context: Context,member:MemberBean) {
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.add_member_name_hint)
            setPadding(32, 0, 32, 0)
        }
        val editText = EditText(context)
        inputLayout.addView(editText)

        val radioGroup = RadioGroup(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 16, 32, 16)
        }
        val radioButton1 = MaterialRadioButton(requireContext()).apply {
            text = "admin"
        }
        val radioButton2 = MaterialRadioButton(requireContext()).apply {
            text = "common"
        }
        radioGroup.addView(radioButton1)
        radioGroup.addView(radioButton2)
        if(viewModel.isOwn && viewModel.isCanTransferOwn ) {
            val radioButton3 = MaterialRadioButton(requireContext()).apply {
                text = "own"
            }
            radioGroup.addView(radioButton3)
        }

        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputLayout)
            addView(radioGroup)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.update_member))
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.confirm)) { d, _ ->
                val name = editText.text.toString()
                var role = MemberRole.ROLE_OWNER
                if (radioGroup.checkedRadioButtonId == radioButton1.id) {
                    role = MemberRole.ROLE_ADMIN
                } else if(radioGroup.checkedRadioButtonId == radioButton2.id){
                    role = MemberRole.ROLE_CUSTOM
                }
                member.memberName = name
                member.role = role
                if(role == MemberRole.ROLE_OWNER){
                    settingFamilybean?.homeId?.let { viewModel.transferOwner(it,member) }
                }else {
                    viewModel.updateMember(member)
                }

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
                if (!NavHostFragment.findNavController(this@FamilySettingFragment).popBackStack()) {
                    requireActivity().finish()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, obj)
    }
}