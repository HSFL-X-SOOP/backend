package hs.flensburg.marlin.business.api.auth.boundary

import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.api.auth.entity.IPAddressLookupResponse
import hs.flensburg.marlin.business.httpclient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking

object IPAddressLookupService {
    fun lookUpIpAddressInfo(ipAddress: String, config: Config.IPInfo): IPAddressLookupResponse = runBlocking {
        httpclient.get("https://ipinfo.io/$ipAddress?token=${config.token}").body<IPAddressLookupResponse>()
    }
}