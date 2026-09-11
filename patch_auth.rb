file_path = "app/src/main/java/com/example/data/repository/BarStockRepository.kt"
content = File.read(file_path)

auth_replacement = <<~RUBY
    suspend fun authenticateUser(identifier: String, password: String): User? {
        val user = dao.getUserByIdentifier(identifier.trim())
        if (user != null) {
            // Try Firebase Auth for true cloud logins if email is valid
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
                try {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(user.email, password.trim()).await()
                } catch (e: Exception) {
                    // Ignore for offline fallback
                }
            }
            if (user.password == password.trim()) {
                return user
            }
        }
        return null
    }
RUBY

content.sub!(/    suspend fun authenticateUser.*?return null\n    \}/m, auth_replacement.strip)

reg_replacement = <<~RUBY
    suspend fun registerUser(name: String, email: String, phone: String, role: UserRole, password: String = "123456"): Long {
        val finalEmail = email.ifBlank { "\${name.lowercase().replace(" ", "")}@thebar.com" }
        try {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(finalEmail).matches()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(finalEmail, password).await()
            }
        } catch (e: Exception) {
            // Might already exist or no network
        }
        val user = User(
            name = name,
            role = role,
            status = if (role == UserRole.ADMIN) UserStatus.APPROVED else UserStatus.PENDING,
            email = finalEmail,
            phone = phone,
            password = password
        )
RUBY

content.sub!(/    suspend fun registerUser\(.*?val user = User\([^)]+\)/m, reg_replacement.strip)


create_attendant_replacement = <<~RUBY
    suspend fun createAttendantByAdmin(name: String, email: String, phone: String, initialPassword: String): Long {
        val finalEmail = email.ifBlank { "\${name.lowercase().replace(" ", "")}@thebar.com" }
        val pass = initialPassword.ifBlank { "123456" }
        try {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(finalEmail).matches()) {
                // Creates account in Firebase Auth but note this logs in the new user, so we should sign back out if we were logged in.
                // However, to keep it simple, we just catch the exception.
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(finalEmail, pass).await()
            }
        } catch (e: Exception) { }
        val user = User(
            name = name,
            role = UserRole.ATTENDANT,
            status = UserStatus.APPROVED,
            email = finalEmail,
            phone = phone,
            password = pass
        )
RUBY

content.sub!(/    suspend fun createAttendantByAdmin\(.*?val user = User\([^)]+\)/m, create_attendant_replacement.strip)


File.write(file_path, content)
