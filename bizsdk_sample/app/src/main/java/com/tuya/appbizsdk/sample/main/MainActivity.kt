package com.tuya.appbizsdk.sample.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thing.appbizsdk.familybiz.ChooseFamilyPopupWindow
import com.thing.appbizsdk.familybiz.FamilyManagerActivity
import com.thing.appbizsdk.familybiz.IChooseFamilyListener
import com.thing.appbizsdk.sample.R
import com.thing.appbizsdk.sample.databinding.ActivityMainBinding
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.android.user.api.ILogoutCallback
import com.thingclips.smart.demo_login.base.utils.LoginHelper
import com.thingclips.smart.demo_login.base.utils.ToastUtil
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.utils.ProgressUtil

class MainActivity : AppCompatActivity(), IHomeView {

    private lateinit var binding: ActivityMainBinding
    private var homePresenter: HomePresenter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        homePresenter = HomePresenter(this)
        binding.logout.setOnClickListener {
            ThingHomeSdk.getUserInstance().logout(object : ILogoutCallback {
                override fun onSuccess() {
                    //演示代码
                    //demo use only start
                    LoginHelper.reLogin(this@MainActivity, false)
                }

                override fun onError(errorCode: String, errorMsg: String) {
                    L.e("tuya logout", "$errorCode $errorMsg")
                }
            })
        }
        binding.currentFamilyName.setOnClickListener {
            homePresenter?.chooseFamilyList()

        }
        homePresenter?.getHomeDefaultDetail()




        binding.family.setOnClickListener {
            val i = Intent()
            i.setClassName(this@MainActivity, "com.thing.appbizsdk.familybiz.FamilyManagerActivity")
            startActivity(i)
        }
        binding.activator.setOnClickListener {
            val i = Intent()
            i.putExtra("homeId",homePresenter?.mCurrentHomeId)
            i.setClassName(this@MainActivity, "com.tuya.appbizsdk.activator.main.ActivatorMainActivity")
            startActivity(i)
        }
        binding.scene.setOnClickListener {
            val i = Intent()
            i.setClassName(this@MainActivity, "com.tuya.appbizsdk.scenebiz.SceneMainActivity")
            startActivity(i)
        }
        binding.device.setOnClickListener {
            val i = Intent()
            i.setClassName(this@MainActivity, "com.thingclips.smart.devicebiz.DeviceListActivity")
            i.putExtra("homeId",homePresenter?.mCurrentHomeId)
            startActivity(i)
        }

    }

    override fun showChooseFamilyListView(familyList: List<FamilyBean>) {
        homePresenter?.mCurrentHomeId?.let {
            val chooseFamilyPopupWindow = ChooseFamilyPopupWindow(this, familyList, it,object:IChooseFamilyListener{
                override fun onChooseed(homeId: Long) {
                    homePresenter?.shiftFamily(homeId)
                }
            })
            chooseFamilyPopupWindow.show()
        }
    }

    override fun showErrorMsg(errorCode: String?, errorMessage: String?) {
        hideLoading()
        ToastUtil.showToast(this, errorMessage)
    }

    override fun getHomeDefaultName() = getString(R.string.my_default_family)


    override fun finishActivity() {
        finish()
    }

    override fun showToast(tip: String?) {
        ToastUtil.showToast(this, tip)
    }

    override fun showToast(resId: Int) {
        ToastUtil.showToast(this, getString(resId))
    }

    override fun showLoading() {
        ProgressUtil.showLoading(this)
    }

    override fun showLoading(resId: Int) {
        ProgressUtil.showLoading(this)
    }

    override fun hideLoading() {
        ProgressUtil.hideLoading()
    }

    override fun updateHome(familyName: String?) {
        binding.currentFamilyName.text = familyName

    }

    override fun updateWeather(weather: String) {
        binding.tvWeather.text = weather
    }

    override fun updateCityName(city: String) {
        binding.tvCity.text = city
    }

    override fun updateDevlist(bean: HomeBean?) {
        bean?.let {
            val devlist = it.deviceList
            val shareList = it.sharedDeviceList
            binding.devicemanager.text = getString(R.string.deivce, devlist.size, shareList.size)
            val grouplist = it.groupList
            val sharegroupList = it.sharedGroupList
            binding.groupmanager.text =
                getString(R.string.group, grouplist.size, sharegroupList.size)
        }
    }

    override fun showFamilyInviteDialog(homeId: Long, homeName: String?) {
        ToastUtil.showToast(this, getString(com.thing.appbizsdk.familybiz.R.string.confirmation_invatation_tip)+":"+homeName)
    }
    override fun showFamilyRemovedDialog() {
        ToastUtil.showToast(this, getString(com.thing.appbizsdk.familybiz.R.string.current_family_delete_tip))

    }
    override fun onDestroy() {
        super.onDestroy()
        homePresenter?.onDestroy()
    }

}