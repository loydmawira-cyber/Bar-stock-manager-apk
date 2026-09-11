import re

file_path = "app/src/main/java/com/example/ui/viewmodel/BarStockViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

import_to_add = "import com.google.firebase.auth.PhoneAuthCredential\\nimport com.google.firebase.auth.FirebaseAuth\\nimport kotlinx.coroutines.tasks.await\\n"
content = re.sub(r"import androidx.lifecycle.ViewModel\n", f"import androidx.lifecycle.ViewModel\n{import_to_add}", content, count=1)

manager_code = """    val phoneAuthManager = PhoneAuthManager()

    fun loginWithPhoneCredential(credential: PhoneAuthCredential, phone: String) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().signInWithCredential(credential).await()
                val localUser = repository.getUserByPhone(phone)
                if (localUser != null) {
                    loginUser(localUser)
                } else {
                    _toastMessage.emit("Phone verified, but no local account found.")
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Phone sign-in failed")
            }
        }
    }
"""

content = content.replace("    fun loginWithCredentials", manager_code + "\n    fun loginWithCredentials")

with open(file_path, "w") as f:
    f.write(content)
