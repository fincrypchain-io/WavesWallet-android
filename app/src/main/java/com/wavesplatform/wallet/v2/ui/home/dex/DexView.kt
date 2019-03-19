package com.wavesplatform.wallet.v2.ui.home.dex

import com.wavesplatform.sdk.net.model.WatchMarket
import com.wavesplatform.wallet.v2.ui.base.view.BaseMvpView

interface DexView : BaseMvpView {
    fun afterSuccessLoadMarkets(list: ArrayList<WatchMarket>)
    fun afterFailedLoadMarkets()
    fun afterSuccessLoadPairInfo(watchMarket: WatchMarket, index: Int)
    fun afterFailedLoadPairInfo()
}
