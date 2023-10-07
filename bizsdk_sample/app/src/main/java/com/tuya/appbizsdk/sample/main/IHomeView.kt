package com.tuya.appbizsdk.sample.main

import com.thingclips.smart.android.mvp.view.IView
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.home.sdk.bean.HomeBean

interface IHomeView:IView {
    fun updateHome(familyName: String?)
    fun updateWeather(weather:String)
    fun updateCityName(city:String)
    fun updateDevlist(homeBean:HomeBean?)
    fun showChooseFamilyListView(familyList: List<FamilyBean>)
    fun showFamilyInviteDialog(homeId: Long, homeName: String?)
    fun showErrorMsg(errorCode: String?, errorMessage: String?)
    fun getHomeDefaultName(): String?
    fun showFamilyRemovedDialog()

}