package page.stephens.dailydozen.data.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS token storage: the Keychain (kSecClassGenericPassword), accessible after
 * first unlock so a background sync after reboot still works.
 *
 * NOTE: Mac-only build target — this file is NOT compiled or run on the Linux
 * dev box or in CI (which build Android + Wasm only). It is written to the
 * standard Security-framework pattern but is UNVERIFIED; build and test it on a
 * Mac before claiming iOS support (per the council's honesty discipline).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class TokenStore {

    actual suspend fun read(): String? {
        val account = CFBridgingRetain(ACCOUNT as NSString)
        val service = CFBridgingRetain(SERVICE as NSString)
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, account)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val token = memScoped {
            val out = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, out.ptr) == errSecSuccess) {
                (CFBridgingRelease(out.value) as? NSData)
                    ?.let { NSString.create(it, NSUTF8StringEncoding) as String? }
            } else {
                null
            }
        }
        CFRelease(query)
        CFBridgingRelease(account)
        CFBridgingRelease(service)
        return token
    }

    actual suspend fun write(token: String) {
        clear() // one value per account
        val account = CFBridgingRetain(ACCOUNT as NSString)
        val service = CFBridgingRetain(SERVICE as NSString)
        val data = CFBridgingRetain((token as NSString).dataUsingEncoding(NSUTF8StringEncoding))
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, account)
        CFDictionaryAddValue(query, kSecValueData, data)
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
        SecItemAdd(query, null)
        CFRelease(query)
        CFBridgingRelease(account)
        CFBridgingRelease(service)
        CFBridgingRelease(data)
    }

    actual suspend fun clear() {
        val account = CFBridgingRetain(ACCOUNT as NSString)
        val service = CFBridgingRetain(SERVICE as NSString)
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)!!
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, account)
        SecItemDelete(query)
        CFRelease(query)
        CFBridgingRelease(account)
        CFBridgingRelease(service)
    }

    private companion object {
        const val SERVICE = "page.stephens.dailydozen"
        const val ACCOUNT = "jwt"
    }
}
