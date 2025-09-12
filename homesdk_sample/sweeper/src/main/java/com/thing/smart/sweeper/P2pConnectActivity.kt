package com.thing.smart.sweeper

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.thing.smart.sweeper.databinding.ActivityP2pConnectBinding
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.optimus.sweeper.api.IThingSweeperKitSdk
import com.thingclips.smart.optimus.sweeper.api.IThingSweeperP2P
import com.thingclips.smart.sweepe.p2p.bean.SweeperP2PBean
import com.thingclips.smart.sweepe.p2p.callback.SweeperP2PCallback
import com.thingclips.smart.sweepe.p2p.callback.SweeperP2PDataCallback
import com.thingclips.smart.sweepe.p2p.manager.DownloadType

/**
 *
 * create by nielev on 2023/2/24
 */
class P2pConnectActivity : AppCompatActivity() {
    var mSweeperP2P: IThingSweeperP2P? = null
    private lateinit var binding: ActivityP2pConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityP2pConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.setNavigationOnClickListener { finish() }

        //get device id
        val devId = intent.getStringExtra("deviceId")

        //get sweeperp2p
        val iThingSweeperKitSdk = ThingOptimusSdk.getManager(
            IThingSweeperKitSdk::class.java
        )
        if (null != iThingSweeperKitSdk) {
            mSweeperP2P = iThingSweeperKitSdk.getSweeperP2PInstance(devId)
            //p2p connect
            binding.btnStartConnectP2pStep.setOnClickListener { v: View? ->
                mSweeperP2P?.connectDeviceByP2P(object : SweeperP2PCallback {
                    override fun onSuccess() {
                        binding.tvP2pConnectShow.text = "${getString(R.string.p2p_connect_status)} true"
                        //p2p connect suc, start get Sweeper data
                        mSweeperP2P?.startObserverSweeperDataByP2P(
                            DownloadType.P2PDownloadTypeStill,
                            object : SweeperP2PCallback {
                                override fun onSuccess() {
                                    //start suc
                                    binding.tvP2pDownloadDataStatus.text =
                                        "${getString(R.string.p2p_download_data_status)} true"
                                }

                                override fun onFailure(i: Int) {
                                    //start failure
                                    binding.tvP2pDownloadDataStatus.text =
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
                        binding.tvP2pConnectShow.text = "${getString(R.string.p2p_connect_status)} false"
                    }
                })
            }
            binding.btnStopP2PData.setOnClickListener {
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