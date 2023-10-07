package com.tuya.appbizsdk.sample.main

import com.thingclips.smart.android.mvp.presenter.BasePresenter
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.bean.LocationCityBean
import com.thingclips.smart.family.callback.FamilyChangeListener
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.family.usecase.interf.IFamilyUseCase
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.api.IThingHomeRoomInfoChangeExListener
import com.thingclips.smart.home.sdk.api.IThingHomeRoomInfoChangeListener
import com.thingclips.smart.home.sdk.api.IThingHomeStatusListener
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.bean.WeatherBean
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.smart.sdk.bean.GroupBean

class HomePresenter(val view:IHomeView):BasePresenter() {
    private var mFamilyUseCase: IFamilyUseCase = FamilyManagerCoreKit.getFamilyUseCase()
    var mCurrentHomeId: Long? = null
    var mCurrentHomeName: String? = null

    private var homeStatusListener :IThingHomeStatusListener? = null
    private var mHomeRoomListener: IThingHomeRoomInfoChangeListener? = null
    private val familyChangeListener = object : FamilyChangeListener {
        override fun onFamilyAdded(homeId: Long) {

        }

        override fun onFamilyInvite(homeId: Long, homeName: String?) {
            //Family invitation pop-up
            view.showFamilyInviteDialog(homeId, homeName)
        }

        override fun onFamilyRemoved(homeId: Long, isBySelfRemoved: Boolean) {
            if (!isBySelfRemoved) { //If not deleted by oneself, give a prompt that the family has been deleted
                view.showFamilyRemovedDialog()
            }
            if (mCurrentHomeId == homeId) {
                getHomeDefaultDetail()
            }
        }

        override fun onFamilyInfoChanged(homeId: Long) {
            getHomeDetail(homeId)
        }

        override fun onFamilyShift(familyId: Long, familyName: String?) {
            registerHomeListener(familyId, familyName)
        }

        override fun onSharedDeviceList(sharedDeviceList: MutableList<DeviceBean>?) {
            //Refreshing the cache data can do
            refreshDevlist(mCurrentHomeId?.let { ThingHomeSdk.newHomeInstance(it).homeBean })
        }

        override fun onSharedGroupList(sharedGroupList: MutableList<GroupBean>?) {
            //Refreshing the cache data can do
            refreshDevlist(mCurrentHomeId?.let { ThingHomeSdk.newHomeInstance(it).homeBean })
        }

        override fun onServerConnectSuccess() {

        }
    }

    private fun refreshDevlist(home: HomeBean?) {
         view.updateDevlist(home)
        home?.let {
            //  Get weather
            getHomeWeather(it.lon,it.lat)
            // Get city name
            getHomeCity(it.lon,it.lat)
        }

    }

    private fun registerHomeListener(homeId: Long, familyName: String?) {
        homeId?.let {
            mCurrentHomeId?.let { it1 ->
                homeStatusListener?.let { it2 ->
                    FamilyManagerCoreKit.unregisterHomeStatusListener(
                        it1,
                        it2
                    )
                }
            }
            homeStatusListener = object : IThingHomeStatusListener {
                override fun onDeviceAdded(devId: String?) {
                    //Refreshing the cache data will do
                    refreshDevlist(mCurrentHomeId?.let { ThingHomeSdk.newHomeInstance(it).homeBean })
                }

                override fun onDeviceRemoved(devId: String?) {
                    //Refreshing the cache data will do
                    refreshDevlist(mCurrentHomeId?.let { ThingHomeSdk.newHomeInstance(it).homeBean })
                }

                override fun onGroupAdded(groupId: Long) {
                    //Refreshing the cache data will do
                    refreshDevlist(mCurrentHomeId?.let { ThingHomeSdk.newHomeInstance(it).homeBean })
                }

                override fun onGroupRemoved(groupId: Long) {
                    //Refreshing the cache data will do
                    refreshDevlist(mCurrentHomeId?.let { ThingHomeSdk.newHomeInstance(it).homeBean })
                }

                override fun onMeshAdded(meshId: String?) {

                }

            }
            FamilyManagerCoreKit.registerHomeStatusListener(homeId,
                homeStatusListener as IThingHomeStatusListener
            )
             mHomeRoomListener = object : IThingHomeRoomInfoChangeExListener {
                override fun onDeviceRoomInfoUpdate(devId: String?) {

                }

                override fun onGroupRoomInfoUpdate(groupId: Long?) {

                }

                override fun onRoomAdd(homeId: String?, roomId: Long?) {

                }

                override fun onRoomDelete(homeId: String?, roomId: Long?) {

                }

                override fun onDeviceRoomInfoChanged(roomId: Long, devId: String?, add: Boolean) {

                }

                override fun onGroupRoomInfoChanged(roomId: Long, id: Long, add: Boolean) {

                }

                override fun onRoomNameChanged(roomId: Long, name: String?) {

                }

                override fun onRoomOrderChanged() {

                }
            }
            FamilyManagerCoreKit.registerHomeDeviceRoomListener(homeId,
                mHomeRoomListener as IThingHomeRoomInfoChangeExListener
            )


            mCurrentHomeName = familyName
            mCurrentHomeId = homeId
            view.updateHome(familyName)
            //Obtain device data, set up family mqtt message service
            getHomeDeviceList(homeId)


        }

    }

    private fun getHomeCity( lon:Double, lat:Double) {
        mFamilyUseCase.getCityByLatLon(lon,lat,object:IFamilyDataCallback<LocationCityBean>{
            override fun onSuccess(result: LocationCityBean?) {
                result?.let {
                    view.updateCityName(it.province+"  "+it.city)
                }

            }

            override fun onError(errorCode: String?, errorMessage: String?) {

            }
        } )
    }

    private fun getHomeWeather( lon:Double, lat:Double) {
        mFamilyUseCase.getHomeWeather(lon,lat,object:IFamilyDataCallback<WeatherBean>{
            override fun onSuccess(result: WeatherBean?) {
                result?.let {
                    view.updateWeather(it.condition+" ,"+result.temp)
                }

            }

            override fun onError(errorCode: String?, errorMessage: String?) {

            }
        } )
    }


    init {
        FamilyManagerCoreKit.registerFamilyChangeListener(familyChangeListener)
    }

    fun chooseFamilyList(){
        view.showLoading()
        mFamilyUseCase.getFamilyList(object :IFamilyDataCallback<BizResponseData<List<FamilyBean>>>{
            override fun onSuccess(result: BizResponseData<List<FamilyBean>>?) {
                view.hideLoading()
                result?.data?.let {
                    view.showChooseFamilyListView(it)
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                view.showErrorMsg(errorCode, errorMessage)
            }
        })
    }

    fun getHomeDefaultDetail() {
        view.showLoading()
        mFamilyUseCase.getCurrentDefaultFamilyDetail(object :
            IFamilyDataCallback<BizResponseData<FamilyBean>> {
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                if (result?.data == null) {
                    creatDefaultFamily()
                } else {
                    view.hideLoading()
                    result?.data?.let {
                        registerHomeListener(it.homeId, it.familyName)
                    }
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                view.showErrorMsg(errorCode, errorMessage)
            }

        })
    }
    private  fun creatDefaultFamily() {
        mFamilyUseCase.createDefaultFamily( view.getHomeDefaultName(),
            object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
                override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                    result?.data?.let {
                        view.hideLoading()
                        mFamilyUseCase.shiftCurrentFamily(it.homeId, null)
                    }
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    view.showErrorMsg(errorCode, errorMessage)
                }
            })
    }

    private fun getHomeDeviceList(homeId: Long) {
        //Obtain device data
        ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean?) {
                refreshDevlist(bean)
            }

            override fun onError(errorCode: String?, errorMsg: String?) {
                view.showErrorMsg(errorCode, errorMsg)
            }
        })
    }

    private fun getHomeDetail(homeId: Long) {
        mFamilyUseCase?.getFamilyDetail(homeId, object :
            IFamilyDataCallback<BizResponseData<FamilyBean>> {
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                result?.data?.let {
                    view.updateHome(it.familyName)
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                view.showErrorMsg(errorCode, errorMessage)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mFamilyUseCase.onDestroy()
        FamilyManagerCoreKit.unregisterFamilyChangeListener(familyChangeListener)
        mCurrentHomeId?.let { it1 ->
            homeStatusListener?.let {
                FamilyManagerCoreKit.unregisterHomeStatusListener(it1, it)
            }
            mHomeRoomListener?.let {
                FamilyManagerCoreKit.unregisterHomeDeviceRoomListener(it1,it)
            }
        }
    }

    fun shiftFamily(homeId: Long) {
        mFamilyUseCase.shiftCurrentFamily(homeId,null)
    }
}