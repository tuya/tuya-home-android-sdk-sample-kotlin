package com.tuya.appsdk.sample.device.mgt

/**
 * create by dongdaqing[mibo] 2023/9/20 11:01
 */
data class SimpleDevice(
    val devId: String,
    val icon: String,
    val name: String,
    val online: Boolean,
    val category: String?,
    val displays: List<SimpleDp>,
    val operates: List<SimpleDp>,
    val simpleSwitch: SimpleSwitch?
) {

    fun sameContent(other: SimpleDevice): Boolean {
        return icon == other.icon
                && name == other.name
                && category == other.category
                && online == other.online
                && same(other.simpleSwitch)
                && same(displays, other.displays)
                && same(operates, other.operates)
    }

    private fun same(other: SimpleSwitch?): Boolean {
        return simpleSwitch?.switchOn == other?.switchOn
    }

    private fun same(a: List<SimpleDp>, b: List<SimpleDp>): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            val ax = a[i]
            val bx = b[i]
            if (!ax.same(bx)) return false
        }
        return true
    }
}

data class SimpleDp(
    val devId: String,
    val dpId: String,
    val iconFont: String?,
    val status: String,
    val dpName: String?,
    val type: String,
) {
    fun same(other: SimpleDp): Boolean {
        return dpId == other.dpId
                && devId == other.devId
                && iconFont == other.iconFont
                && status == other.status
                && dpName == other.dpName
    }
}

data class SimpleSwitch(val switchOn: Boolean)