package com.thing.smart.sweeper

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.optimus.sweeper.api.IThingSweeperKitSdk
import com.thingclips.smart.optimus.sweeper.api.IThingSweeperP2P
import com.thingclips.smart.sweepe.p2p.bean.SweeperP2PBean
import com.thingclips.smart.sweepe.p2p.callback.SweeperP2PCallback
import com.thingclips.smart.sweepe.p2p.callback.SweeperP2PDataCallback
import com.thingclips.smart.sweepe.p2p.manager.DownloadType
import kotlinx.android.synthetic.main.activity_p2p_connect.*

/**
 *
 * create by nielev on 2023/2/24
 */
class P2pConnectActivity : AppCompatActivity(){
    var mSweeperP2P:IThingSweeperP2P? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p2p_connect)
        setSupportActionBar(topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }


        //get device id
        val devId = intent.getStringExtra("deviceId")

        //get sweeperp2p
        val iThingSweeperKitSdk = ThingOptimusSdk.getManager(
            IThingSweeperKitSdk::class.java
        )
        if (null != iThingSweeperKitSdk) {
            mSweeperP2P = iThingSweeperKitSdk.getSweeperP2PInstance(devId)
            //p2p connect
            btnStartConnectP2pStep.setOnClickListener { v: View? ->
                mSweeperP2P?.connectDeviceByP2P(object : SweeperP2PCallback {
                    override fun onSuccess() {
                        tvP2pConnectShow.text = "${getString(R.string.p2p_connect_status)} true"
                        //p2p connect suc, start get Sweeper data
                        mSweeperP2P?.startObserverSweeperDataByP2P(
                            DownloadType.P2PDownloadTypeStill,
                            object : SweeperP2PCallback {
                                override fun onSuccess() {
                                    //start suc
                                    tvP2pDownloadDataStatus.text =
                                        "${getString(R.string.p2p_download_data_status)} true"
                                }

                                override fun onFailure(i: Int) {
                                    //start failure
                                    tvP2pDownloadDataStatus.text =
                                        "${getString(R.string.p2p_download_data_status)} false"
                                }
                            },
                            object : SweeperP2PDataCallback {
                                override fun receiveData(i: Int, sweeperP2PBean: SweeperP2PBean?) {
                                    //get Data
                                }

                                override fun onFailure(i: Int) {}
                            })
                    }

                    override fun onFailure(i: Int) {
                        tvP2pConnectShow.text = "${getString(R.string.p2p_connect_status)} false"
                    }
                })
            }
            btnStopP2PData.setOnClickListener {
                mSweeperP2P?.stopObserverSweeperDataByP2P(object : SweeperP2PCallback {
                    override fun onSuccess() {
                        //stop suc
                    }

                    override fun onFailure(i: Int) {
                        //stop suc
                    }
                })
            }
        }

    }
}