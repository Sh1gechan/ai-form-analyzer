package com.sportanalyzer.app.di

import android.util.Log
import com.sportanalyzer.app.data.api.GeminiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .dns(FallbackDns())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(retrofit: Retrofit): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }
}

/**
 * DNS リゾルバー：
 *   1. システム DNS を試みる（通常の環境では問題なく動作）
 *   2. UnknownHostException が発生した場合、DNS-over-HTTPS (DoH) にフォールバック
 *      - Google DoH   : dns.google   → 8.8.8.8 (IP ハードコード)
 *      - Cloudflare   : one.one.one.one → 1.1.1.1 (IP ハードコード)
 *      ※ DoH クライアント自身は IP 直指定のため DNS を必要としない
 *
 * これにより Android エミュレータで QEMU の DNS プロキシ (10.0.2.3) が
 * 機能しない場合でも TCP さえ通っていれば API 通信が可能になる。
 */
class FallbackDns : Dns {

    // DoH リクエスト用の専用クライアント
    // dns.google / one.one.one.one → IP を直接返すことで循環 DNS 依存を排除
    private val dohBootstrapDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = when (hostname) {
            "dns.google"      -> listOf(InetAddress.getByName("8.8.8.8"),
                                        InetAddress.getByName("8.8.4.4"))
            "one.one.one.one" -> listOf(InetAddress.getByName("1.1.1.1"),
                                        InetAddress.getByName("1.0.0.1"))
            else              -> Dns.SYSTEM.lookup(hostname)
        }
    }

    private val dohClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(dohBootstrapDns)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            val result = Dns.SYSTEM.lookup(hostname)
            result
        } catch (e: UnknownHostException) {
            Log.w("FallbackDns", "システム DNS 失敗 ($hostname) → DoH フォールバック")
            queryDoH(hostname, "dns.google")
                ?: queryDoH(hostname, "one.one.one.one")
                ?: throw e
        }
    }

    /**
     * DNS-over-HTTPS (JSON API) でホスト名を解決する。
     * GET https://dns.google/resolve?name=<hostname>&type=A
     * レスポンス例: {"Answer":[{"type":1,"data":"142.250.183.106"},...]}
     */
    private fun queryDoH(hostname: String, dohHost: String): List<InetAddress>? {
        return try {
            val encodedName = URLEncoder.encode(hostname, "UTF-8")
            val url = "https://$dohHost/resolve?name=$encodedName&type=A"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()
            val response = dohClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("FallbackDns", "DoH $dohHost HTTP ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null

            // "type":1 (A レコード) の "data" フィールドから IPv4 アドレスを抽出
            val ipPattern = Regex(""""type"\s*:\s*1[^}]*?"data"\s*:\s*"([\d.]+)"""")
            val ips = ipPattern.findAll(body).map { it.groupValues[1] }.toList()

            if (ips.isEmpty()) {
                Log.w("FallbackDns", "DoH $dohHost: A レコードなし ($hostname)")
                null
            } else {
                Log.d("FallbackDns", "DoH $dohHost 解決成功: $hostname → $ips")
                ips.mapNotNull {
                    try { InetAddress.getByName(it) } catch (_: Exception) { null }
                }.takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.w("FallbackDns", "DoH $dohHost 失敗: ${e.message}")
            null
        }
    }
}
