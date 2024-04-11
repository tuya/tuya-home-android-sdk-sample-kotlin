package com.thing.appbizsdk.familybiz.model

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.thingclips.sdk.core.PluginManager
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.*
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.family.callback.IFamilyMemberDataCallback
import com.thingclips.smart.family.callback.IFamilyMemberResultCallback
import com.thingclips.smart.family.usecase.interf.IFamilyRoomUseCase
import com.thingclips.smart.family.usecase.interf.IFamilyUseCase
import com.thingclips.smart.family.usecase.interf.IMemberUseCase
import com.thingclips.smart.home.sdk.anntation.HomeStatus
import com.thingclips.smart.home.sdk.anntation.MemberRole
import com.thingclips.smart.home.sdk.anntation.MemberStatus
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.bean.RoomBean
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.interior.api.IThingHomePlugin
import com.thingclips.smart.interior.api.IThingUserPlugin
import com.thingclips.smart.interior.enums.BizParentTypeEnum
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.smart.sdk.bean.GroupBean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class FamilyManagerModel(application: Application) : AndroidViewModel(application) {

    private val userPlugin = PluginManager.service(
        IThingUserPlugin::class.java
    )

    var isAdmin = false
    var isOwn = false
    var isCanTransferOwn = false
    var mCurMemberBen: MemberBean? = null

    private val _mFamilyList = MutableSharedFlow<List<FamilyBean>>(replay = 1)
    val mFamilyList: SharedFlow<List<FamilyBean>> = _mFamilyList

    private val _mMemberList = MutableLiveData<ArrayList<MemberBean>>()
    val mMemberList: MutableLiveData<ArrayList<MemberBean>> = _mMemberList

    private val _mRoomList = MutableLiveData<ArrayList<TRoomBean>>()
    val mRoomList: MutableLiveData<ArrayList<TRoomBean>> = _mRoomList

    private val _mAllDevList = MutableLiveData<ArrayList<DeviceInRoomBean>>()
    val mAllDevList: MutableLiveData<ArrayList<DeviceInRoomBean>> = _mAllDevList


    private val _errorEvent = MutableSharedFlow<Pair<String?, String?>>()
    val errorEvent: SharedFlow<Pair<String?, String?>> = _errorEvent

    private val _settingFamilybean = MutableLiveData<FamilyBean>()
    val mRefreshFamilybean = _settingFamilybean

    private val _settingRoombean = MutableLiveData<TRoomBean>()
    val mRefreshRoombean = _settingRoombean

    private val _delFamilybean = MutableSharedFlow<Boolean>()
    val delFamilybean: SharedFlow<Boolean> = _delFamilybean

    private val _sortRoomBean = MutableSharedFlow<Boolean>()
    val sortRoomBean: SharedFlow<Boolean> = _sortRoomBean

    private val _sortDevInRoomBean =  MutableSharedFlow<Boolean>()
    val sortDevInRoomBean:SharedFlow<Boolean> = _sortDevInRoomBean

    private val _mInvitationMessageBean = MutableSharedFlow<InvitationMessageBean>()
    val mInvitationMessageBean: SharedFlow<InvitationMessageBean> = _mInvitationMessageBean


    private var mFamilyUseCase: IFamilyUseCase = FamilyManagerCoreKit.getFamilyUseCase()
    private var mMemberUseCase: IMemberUseCase = FamilyManagerCoreKit.getMemberUseCase()
    private var mRoomUseCase: IFamilyRoomUseCase = FamilyManagerCoreKit.getRoomUseCase()


    fun getFamilyList() {
        mFamilyUseCase.getFamilyList(object :
            IFamilyDataCallback<BizResponseData<List<FamilyBean>>> {
            override fun onSuccess(result: BizResponseData<List<FamilyBean>>?) {
                viewModelScope.launch {
                    result?.data?.let {
                        _mFamilyList.emit(it.filter { family->family.familyStatus != HomeStatus.REJECT })
                    }
                }

            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(errorCode, errorMessage))
                }
            }
        })
    }



    fun addFamily(bean: CreateFamilyRequestBean) {

        mFamilyUseCase.createFamily(bean,
            object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
                override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                    viewModelScope.launch {
                        result?.data?.let {
                            var list = ArrayList<FamilyBean>()
                            list.addAll(mFamilyList.replayCache[0])
                            list.add(it)
                            _mFamilyList.emit(list)
                        }
                    }
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    viewModelScope.launch {
                        _errorEvent.emit(Pair(errorCode, errorMessage))
                    }
                }
            })
    }

    fun joinFamily(inviteCode: String) {
        mFamilyUseCase.joinFamily(inviteCode, object : IFamilyDataCallback<Boolean> {
            override fun onSuccess(result: Boolean?) {
                getFamilyList()
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(errorCode, errorMessage))
                }
            }
        })

    }

    fun updateFamily(
        name: String,
        lon: Double,
        lat: Double,
        address: String,
        settingFamilybean: FamilyBean?
    ) {
        val requestBean = settingFamilybean?.rooms?.let {
            CreateFamilyRequestBean(name, lon, lat, address, null)
        } ?: CreateFamilyRequestBean(name, lon, lat, address, arrayListOf("room1")).apply {
            isForComplete = true // set isForComplete to Improve family information, and it will return family information.
        }
        settingFamilybean?.homeId?.let {
            mFamilyUseCase.updateFamily(
                it,
                requestBean,
                object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
                    override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                        result?.let {result->
                            if( result.data == null){
                                getFamilyDetail(it)
                            }else{
                                _settingFamilybean.postValue(result.data)
                            }
                        }

                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        viewModelScope.launch {
                            _errorEvent.emit(Pair(errorCode, errorMessage))
                        }
                    }
                })
        }
    }

    fun dismissfamily(homeId: Long?) {
        homeId?.let {
            mFamilyUseCase.dismissFamily(it, object : IFamilyDataCallback<Boolean> {
                override fun onSuccess(result: Boolean?) {
                    viewModelScope.launch {
                        _delFamilybean.emit(true)
                    }
                }

                override fun onError(errorCode: String?, errorMessage: String?) {

                }
            })
        }
    }

    fun leavefamily(homeId: Long?) {
        homeId?.let {
            mCurMemberBen?.memberId?.let { it1 ->
                mFamilyUseCase.leaveFamily(it, it1, object : IFamilyDataCallback<Boolean> {
                    override fun onSuccess(result: Boolean?) {
                        viewModelScope.launch {
                            _delFamilybean.emit(true)
                        }
                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        viewModelScope.launch {
                            _errorEvent.emit(Pair(errorCode, errorMessage))
                        }
                    }
                })
            }
        }
    }

    fun getMemberList(homeId: Long) {
        mFamilyUseCase.getFamilyMemberList(homeId,
            object : IFamilyDataCallback<BizResponseData<List<MemberBean>>> {
                override fun onSuccess(result: BizResponseData<List<MemberBean>>?) {
                    val members: ArrayList<MemberBean> = arrayListOf()
                    result?.data?.let { members.addAll(it) }
                    var existJoined = false
                    if (members.size == 1) {
                        isAdmin = true
                        mCurMemberBen = members.get(0)
                    } else {
                        val user: User? = userPlugin.getUserInstance().getUser()
                        val uid = user?.uid ?: ""
                        for (j in members.indices) {
                            val bean: MemberBean = members.get(j)
                            if (TextUtils.equals(uid, bean.uid)) {
                                isAdmin = bean.isAdmin
                                mCurMemberBen = bean
                            } else if (bean.memberStatus != MemberStatus.WAITING && !TextUtils.isEmpty(
                                    bean.account
                                )
                            ) {
                                existJoined = true
                            }
                        }
                        isOwn =
                            members.size == 1 || (members.size > 1 && mCurMemberBen != null && mCurMemberBen!!.role == MemberRole.ROLE_OWNER)
                        isCanTransferOwn =
                            members.size > 1 && mCurMemberBen != null && mCurMemberBen!!.role == MemberRole.ROLE_OWNER && existJoined
                    }
                    mFamilyUseCase.getInvitationMemberList(homeId,
                        object : IFamilyDataCallback<BizResponseData<List<MemberBean>>> {
                            override fun onSuccess(result1: BizResponseData<List<MemberBean>>?) {
                                result1?.data?.let { members.addAll(it) }
                                _mMemberList.postValue(members)
                            }

                            override fun onError(errorCode: String?, errorMessage: String?) {
                                _mMemberList.postValue(members)
                            }
                        })
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    viewModelScope.launch {
                        _errorEvent.emit(Pair(errorCode, errorMessage))
                    }
                }
            })
    }

    fun addMember(memberBean: MemberBean) {
        val bean = MemberWrapperBean.Builder()
            .setHomeId(memberBean.homeId)
            .setRole(memberBean.role)
            .setNickName(memberBean.memberName)
            .setAccount(memberBean.account)
            .setCountryCode(memberBean.countryCode)
            .setAdmin(memberBean.isAdmin)
            .build()
        mMemberUseCase.addMember(bean, object : IFamilyMemberDataCallback<MemberBean> {
            override fun onSuccess(result: MemberBean?) {
                getMemberList(memberBean.homeId)
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(errorCode, errorMessage))
                }
            }
        })
    }
    fun transferOwner(homeId:Long,memberBean: MemberBean){
        mFamilyUseCase.transferOwner(homeId,memberBean.memberId,object :IFamilyDataCallback<Boolean>{
            override fun onSuccess(result: Boolean?) {
                if(result == true) {
                   getMemberList(homeId)
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(errorCode, errorMessage))
                }
            }
        })
    }
    fun updateMember(memberBean: MemberBean) {
        val bean = MemberWrapperBean.Builder()
            .setMemberId(memberBean.memberId)
            .setRole(memberBean.role)
            .setNickName(memberBean.memberName)
            .setAdmin(memberBean.isAdmin)
            .build()
        mMemberUseCase.updateMember(bean, object : IFamilyMemberResultCallback {
            override fun onError(code: String?, error: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(code, error))
                }
            }

            override fun onSuccess() {
                val beans =
                    (_mMemberList.value?.filter { it.memberId != memberBean.memberId }) as ArrayList<MemberBean>?
                beans?.add(memberBean)
                _mMemberList.postValue(beans)
            }
        })
    }

    fun deleteMember(homeId: Long, memberBean: MemberBean) {
        mMemberUseCase.removeMember(
            homeId,
            memberBean.memberId,
            object : IFamilyMemberResultCallback {
                override fun onError(code: String?, error: String?) {
                    viewModelScope.launch {
                        _errorEvent.emit(Pair(code, error))
                    }
                }

                override fun onSuccess() {
                    val beans = _mMemberList.value?.filter { it.memberId != memberBean.memberId }
                    _mMemberList.postValue(beans as ArrayList<MemberBean>?)
                }
            })
    }

    fun addInvationMember(homeId: Long?) {
        if (homeId != null) {
            mMemberUseCase.getInvitationMessage(homeId,
                object : IFamilyMemberDataCallback<InvitationMessageBean> {
                    override fun onSuccess(result: InvitationMessageBean?) {
                        viewModelScope.launch {
                            if (result != null) {
                                _mInvitationMessageBean.emit(result)
                            }
                        }
                        getMemberList(homeId)
                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        viewModelScope.launch {
                            _errorEvent.emit(Pair(errorCode, errorMessage))
                        }
                    }
                })
        }

    }

    fun getRoomList(homeId: Long?) {
        if (homeId != null) {
            mRoomUseCase?.getRoomList(
                homeId,
                true,
                object : IFamilyDataCallback<BizResponseData<List<TRoomBean>>> {
                    override fun onSuccess(result: BizResponseData<List<TRoomBean>>?) {
                        result?.data?.let {
                            val list = arrayListOf<TRoomBean>()
                            list.addAll(it)
                            _mRoomList.postValue(list)
                        }
                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        viewModelScope.launch {
                            _errorEvent.emit(Pair(errorCode, errorMessage))
                        }
                    }
                })
        }
    }

    fun addRoom(homeId: Long?,name: String?) {
        if (homeId != null) {
            mRoomUseCase?.addRoom(
                homeId,
                name,
                object : IFamilyDataCallback<BizResponseData<TRoomBean>> {
                    override fun onSuccess(result: BizResponseData<TRoomBean>?) {
                        result?.data?.let {
                            val list = arrayListOf<TRoomBean>()
                            _mRoomList.value?.let { it1 -> list.addAll(it1) }
                            list.add(it)
                            _mRoomList.postValue(list)
                        }
                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        viewModelScope.launch {
                            _errorEvent.emit(Pair(errorCode, errorMessage))
                        }
                    }
                })
        }
    }

    fun deleteRoom(homeId: Long, roomId: Long) {
        if (roomId != null) {
            mRoomUseCase?.removeRoom(homeId, roomId, object : IFamilyDataCallback<Boolean> {
                override fun onSuccess(result: Boolean?) {
                   getRoomList(homeId)
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    viewModelScope.launch {
                        _errorEvent.emit(Pair(errorCode, errorMessage))
                    }
                }
            })
        }
    }

    fun sortRoom(homeId:Long,list:List<TRoomBean>){
        val roomids = list?.map { it.roomId }
        mRoomUseCase?.sortRoom(homeId,roomids,object :IFamilyDataCallback<Boolean>{
            override fun onSuccess(result: Boolean?) {
                viewModelScope.launch {
                    _sortRoomBean.emit(true)
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(errorCode, errorMessage))
                }
            }
        })
    }

    fun deleteDevFromRoom(dev: DeviceInRoomBean, room: TRoomBean) {
        if(dev.type == BizParentTypeEnum.GROUP.getType()) {
            mRoomUseCase?.removeDeviceFromRoom(room.roomId,dev.id,object :IFamilyDataCallback<Boolean>{
                override fun onSuccess(result: Boolean?) {
                    room.ids.remove(dev)
                    _settingRoombean.postValue(room)
                }

                override fun onError(errorCode: String?, errorMessage: String?) {

                }
            })
        }else{
            mRoomUseCase?.removeDeviceFromRoom(room.roomId,dev.id,object :IFamilyDataCallback<Boolean>{
                override fun onSuccess(result: Boolean?) {
                    room.ids.remove(dev)
                    _settingRoombean.postValue(room)
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    viewModelScope.launch {
                        _errorEvent.emit(Pair(errorCode, errorMessage))
                    }
                }
            })
        }
    }

    fun updateRoomName(room: TRoomBean,name: String){
        mRoomUseCase?.updateRoom(room.roomId,name,object :IFamilyDataCallback<Boolean>{
            override fun onSuccess(result: Boolean?) {
                room.name = name
                _settingRoombean.postValue(room)
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(errorCode, errorMessage))
                }
            }
        })
    }

    fun sortDevInRoom(room: TRoomBean,ids:ArrayList<DeviceInRoomBean>){
        mRoomUseCase?.moveRoomDevGroupList(room.roomId,ids,object :IFamilyDataCallback<Boolean>{
            override fun onSuccess(result: Boolean?) {
                viewModelScope.launch {
                    _sortDevInRoomBean.emit(true)
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(errorCode, errorMessage))
                }
            }
        })
    }

    fun getFamilyDetail(homeId: Long) {
        mFamilyUseCase?.getFamilyDetail(homeId,object :IFamilyDataCallback<BizResponseData<FamilyBean>>{
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                result?.data?.let {
                    _settingFamilybean.postValue(it)
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {

            }
        })
    }

    fun processInvitation(homeId: Long, isAccept: Boolean) {
        mMemberUseCase?.processInvitation(homeId,isAccept,object :IFamilyMemberResultCallback{
            override fun onError(code: String?, error: String?) {
                viewModelScope.launch {
                    _errorEvent.emit(Pair(code, error))
                }
            }

            override fun onSuccess() {
                getFamilyList()
            }
        })
    }


    fun getAllDevInRoomList(homeId: Long){
        val homePlugin = PluginManager.service(
            IThingHomePlugin::class.java
        )
        val roomBeanList = homePlugin.dataInstance.getHomeRoomList(homeId)
        // If there is no room data, please request it again.
        if (roomBeanList == null || roomBeanList.isEmpty()) {
            homePlugin.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
                override fun onSuccess(bean: HomeBean) {
                    val data: ArrayList<DeviceInRoomBean>? = transferRoomDeviceList(0L, bean)
                    data?.let {
                        _mAllDevList.postValue(it)
                    }
                }
                override fun onError(errorCode: String, errorMsg: String) {}
            })
        } else {
            val data: ArrayList<DeviceInRoomBean>? = transferRoomDeviceList(homeId, homePlugin.dataInstance.getHomeBean(homeId!!))
            data?.let {
                _mAllDevList.postValue(it)
            }
        }
    }

    private fun transferRoomDeviceList(homeId: Long, homeBean: HomeBean?): ArrayList<DeviceInRoomBean>? {
        val homePlugin = PluginManager.service(
            IThingHomePlugin::class.java
        )
        val dataManager = homePlugin.dataInstance
        val list: List<RoomBean>? = if (homeBean != null) {
            homeBean.rooms
        } else {
            dataManager.getHomeRoomList(homeId)
        }
        val deviceListInRoomBeans: ArrayList<DeviceInRoomBean> =  ArrayList()
        if (null == list || list.isEmpty()) {
            return deviceListInRoomBeans
        }
        // All the device in the rooms
        for (roomBean in list) {
            val groupList: List<GroupBean> = roomBean.groupList
            val deviceList: List<DeviceBean> = roomBean.deviceList
            if (null != groupList && !groupList.isEmpty()) {
                for (groupBean in groupList) {
                    val deviceBean = DeviceInRoomBean()
                    deviceBean.id = groupBean.id.toString()
                    deviceBean.name = groupBean.name
                    deviceBean.roomName = roomBean.name
                    deviceBean.type = BizParentTypeEnum.GROUP.getType()
                    deviceBean.iconUrl = groupBean.iconUrl
                    deviceBean.displayOrder = groupBean.displayOrder
                    deviceListInRoomBeans.add(deviceBean)
                }
            }
            if (null != deviceList && !deviceList.isEmpty()) {
                for (bean in deviceList) {
                    val deviceBean = DeviceInRoomBean()
                    deviceBean.id = bean.getDevId()
                    deviceBean.name = bean.getName()
                    deviceBean.roomName = roomBean.name
                    deviceBean.type = BizParentTypeEnum.DEVICE.getType()
                    deviceBean.iconUrl = bean.getIconUrl()
                    deviceBean.displayOrder = bean.displayOrder
                    deviceListInRoomBeans.add(deviceBean)
                }
            }
        }

        // all Device list under the household
        var deviceList: List<DeviceBean>
        var groupList: List<GroupBean>
        if (homeBean != null) {
            deviceList = homeBean.deviceList
            groupList = homeBean.groupList
        } else {
            deviceList = dataManager.getHomeDeviceList(homeId)
            groupList = dataManager.getHomeGroupList(homeId)
        }
        var allDevice: ArrayList<DeviceInRoomBean> =  ArrayList()
        for (bean in groupList) {
            val deviceInRoomBean = DeviceInRoomBean()
            deviceInRoomBean.id = bean.id.toString() + ""
            deviceInRoomBean.name = bean.name
            deviceInRoomBean.type = BizParentTypeEnum.GROUP.getType()
            deviceInRoomBean.iconUrl = bean.iconUrl
            allDevice.add(deviceInRoomBean)
        }
        for (bean in deviceList) {
            val deviceInRoomBean = DeviceInRoomBean()
            deviceInRoomBean.id = bean.getDevId()
            deviceInRoomBean.name = bean.getName()
            deviceInRoomBean.type = BizParentTypeEnum.DEVICE.getType()
            deviceInRoomBean.iconUrl = bean.getIconUrl()
            allDevice.add(deviceInRoomBean)
        }
        if (allDevice.size == deviceListInRoomBeans.size) {
            allDevice = deviceListInRoomBeans
        } else {
            allDevice.removeAll(deviceListInRoomBeans)
            allDevice.addAll(deviceListInRoomBeans)
        }
        return allDevice
    }

    override fun onCleared() {
        super.onCleared()
        mFamilyUseCase.onDestroy()
        mMemberUseCase.onDestroy()
        mRoomUseCase.onDestroy()
    }



}