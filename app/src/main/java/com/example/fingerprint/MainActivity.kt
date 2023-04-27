package com.example.fingerprint

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MainActivity : AppCompatActivity() {
    private lateinit var fm: FingerprintManager
    private lateinit var km: KeyguardManager

    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private var KEY_NAME = "my_key"


    private lateinit var cipher: Cipher
    private lateinit var cryptoFinger: FingerprintManager.CryptoObject
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        fm = getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

        if (!km.isKeyguardSecure) {
            Toast.makeText(this, "у вас отключена защита в настройках", Toast.LENGTH_SHORT).show()
            return
        }
        // условие на проверку есть л отпечаток пальца на телефоне
        if (!fm.hasEnrolledFingerprints()) {
            Toast.makeText(this, "у вас нет отпечатка пальцев", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.USE_FINGERPRINT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.USE_FINGERPRINT),
                17
            )
        } else {
            fingerValidation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 17 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fingerValidation()
        }
    }

    private fun fingerValidation() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyStore.load(null)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
            )
            keyGenerator.generateKey()
        } catch (_: Exception) {
        }
        if (initCipher()) {
            cipher.let {
                cryptoFinger = FingerprintManager.CryptoObject(it)
            }
        }
        var events = FingerPrintEvents(this)
        if (::fm.isInitialized && ::cryptoFinger.isInitialized) {
            events.startAuth(fm, cryptoFinger)
        }
    }

    private fun initCipher(): Boolean {
        try {
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
        } catch (_: Exception) {
        }
        try {
            keyStore.load(null)
            val key = keyStore.getKey(KEY_NAME, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}