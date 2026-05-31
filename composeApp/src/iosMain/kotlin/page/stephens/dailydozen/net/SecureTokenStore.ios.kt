package page.stephens.dailydozen.net

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
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureTokenStore {
    private val service = "page.stephens.dailydozen"
    private val account = "dailyDozenAuthToken"

    actual fun getToken(): String? = memScoped {
        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(service as NSString))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account as NSString))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
    }

    actual fun setToken(token: String?) {
        val delete = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionaryAddValue(delete, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(delete, kSecAttrService, CFBridgingRetain(service as NSString))
        CFDictionaryAddValue(delete, kSecAttrAccount, CFBridgingRetain(account as NSString))
        SecItemDelete(delete)
        CFRelease(delete)

        if (token == null) return
        val data = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val add = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionaryAddValue(add, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(add, kSecAttrService, CFBridgingRetain(service as NSString))
        CFDictionaryAddValue(add, kSecAttrAccount, CFBridgingRetain(account as NSString))
        CFDictionaryAddValue(add, kSecValueData, CFBridgingRetain(data))
        SecItemAdd(add, null)
        CFRelease(add)
    }
}
