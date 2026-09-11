file_path = "app/src/main/java/com/example/ui/viewmodel/BarStockViewModel.kt"
content = File.read(file_path)

import_to_add = "import com.google.firebase.auth.PhoneAuthCredential\nimport com.google.firebase.auth.FirebaseAuth\nimport kotlinx.coroutines.tasks.await\n"
content = content.sub(/import androidx.lifecycle.ViewModel\n/, "import androidx.lifecycle.ViewModel\n" + import_to_add)


manager_code = <<~RUBY
    val phoneAuthManager = PhoneAuthManager()

    fun loginWithPhoneCredential(credential: PhoneAuthCredential, phone: String) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().signInWithCredential(credential).await()
                val localUser = repository.getUserByPhone(phone)
                if (localUser != null) {
                    loginUser(localUser)
                } else {
                    _toastMessage.emit("Phone verified, but no account found for this number.")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Phone sign-in failed: ${e.message}")
            }
        }
    }
RUBY

content = content.sub(/    fun loginWithCredentials/, manager_code + "\n    fun loginWithCredentials")

File.write(file_path, content)
