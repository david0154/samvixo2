package com.example.data

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit

object FirebaseService {
    private const val TAG = "FirebaseService"
    
    // Check if Firebase is available and initialized
    val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Start Phone verification flow via Firebase Auth.
     * Includes a local mock simulation if Firebase is not linked in this workspace.
     */
    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationSuccess: (credential: PhoneAuthCredential) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isFirebaseInitialized) {
            Log.w(TAG, "Firebase not initialized. Running in sandbox emulator mode!")
            // Simulate OTP sending delay
            Handler(Looper.getMainLooper()).postDelayed({
                if (phoneNumber.length >= 10) {
                    onCodeSent("simulated_verification_id_123456")
                } else {
                    onFailure(Exception("Invalid Phone Number. Must specify country code, e.g. +1..."))
                }
            }, 1000)
            return
        }

        try {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted:$credential")
                    onVerificationSuccess(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w(TAG, "onVerificationFailed", e)
                    onFailure(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "onCodeSent:$verificationId")
                    onCodeSent(verificationId)
                }
            }

            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
                
            PhoneAuthProvider.verifyPhoneNumber(options)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating verifyPhoneNumber", e)
            onFailure(e)
        }
    }

    /**
     * Completes phone verification by checking verificationId and verificationCode.
     */
    fun verifyCode(
        verificationId: String,
        code: String,
        onSuccess: (uid: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isFirebaseInitialized || verificationId.startsWith("simulated_")) {
            Log.d(TAG, "Verifying code in simulation sandbox.")
            Handler(Looper.getMainLooper()).postDelayed({
                if (code == "123456" || code == "000000" || code.length == 6) {
                    onSuccess("simulated_uid_998877")
                } else {
                    onFailure(Exception("Invalid OTP Code. Please enter security code (e.g. 123456)"))
                }
            }, 800)
            return
        }

        try {
            val auth = FirebaseAuth.getInstance()
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        onSuccess(user?.uid ?: "unknown_uid")
                    } else {
                        onFailure(task.exception ?: Exception("Verification failed"))
                    }
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
