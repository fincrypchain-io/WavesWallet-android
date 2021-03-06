package com.wavesplatform.wallet.v2.data.helpers

import com.vicpin.krealmextensions.RealmConfigStore
import com.vicpin.krealmextensions.queryFirst
import com.vicpin.krealmextensions.save
import com.wavesplatform.wallet.v1.ui.auth.EnvironmentManager
import com.wavesplatform.wallet.v1.util.PrefsUtil
import com.wavesplatform.wallet.v2.data.database.DBHelper
import com.wavesplatform.wallet.v2.data.model.remote.response.*
import com.wavesplatform.wallet.v2.data.model.userdb.AddressBookUser
import com.wavesplatform.wallet.v2.data.model.userdb.AssetBalanceStore
import com.wavesplatform.wallet.v2.util.MigrationUtil
import io.realm.Realm
import io.realm.RealmConfiguration
import javax.inject.Inject

class AuthHelper @Inject constructor(private var prefsUtil: PrefsUtil) {

    fun configureDB(address: String?, guid: String) {

        val configUserData = RealmConfiguration.Builder()
                .name(String.format("%s_userdata.realm", guid))
                .schemaVersion(1)
                .build()
        Realm.compactRealm(configUserData)
        RealmConfigStore.init(AddressBookUser::class.java, configUserData)
        RealmConfigStore.init(AssetBalanceStore::class.java, configUserData)
        RealmConfigStore.init(MarketResponse::class.java, configUserData)
        DBHelper.getInstance().realmUserDataConfig = configUserData
        Realm.getInstance(configUserData).isAutoRefresh = false

        migration(guid, address)

        val config = RealmConfiguration.Builder()
                .name(String.format("%s.realm", guid))
                .schemaVersion(MigrationUtil.VER_DB_WITHOUT_USER_DATA)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.compactRealm(config)
        RealmConfigStore.init(AssetBalance::class.java, config)
        RealmConfigStore.init(IssueTransaction::class.java, config)
        RealmConfigStore.init(Transaction::class.java, config)
        RealmConfigStore.init(Transfer::class.java, config)
        RealmConfigStore.init(Data::class.java, config)
        RealmConfigStore.init(AssetPair::class.java, config)
        RealmConfigStore.init(Order::class.java, config)
        RealmConfigStore.init(Lease::class.java, config)
        RealmConfigStore.init(Alias::class.java, config)
        RealmConfigStore.init(SpamAsset::class.java, config)
        RealmConfigStore.init(AssetInfo::class.java, config)
        DBHelper.getInstance().realmConfig = config
        Realm.getInstance(config).isAutoRefresh = false

        saveDefaultAssets()
    }

    private fun migration(guid: String, address: String?) {
        MigrationUtil.copyPrefDataFromDb(guid)
        MigrationUtil.checkPrevDbAndRename(address, guid)
        MigrationUtil.checkOldAddressBook(prefsUtil, guid)
    }

    private fun saveDefaultAssets() {
        EnvironmentManager.defaultAssets.forEach {
            val asset = queryFirst<AssetBalance> { equalTo("assetId", it.assetId) }
            if (asset == null) {
                it.save()
            }
        }
        prefsUtil.setValue(PrefsUtil.KEY_DEFAULT_ASSETS, true)
    }
}
