import re

file_path = "app/src/main/java/com/example/data/repository/BarStockRepository.kt"
with open(file_path, "r") as f:
    content = f.read()

auth_orig = r"""    suspend fun authenticateUser\(identifier: String, password: String\): User\? \{
        val user = dao.getUserByIdentifier\(identifier.trim\(\)\)
        if \(user != null && user.password == password.trim\(\)\) \{
            return user
        \}
        return null
    \}"""

auth_new = """    suspend fun authenticateUser(identifier: String, password: String): User? {
        val user = dao.getUserByIdentifier(identifier.trim())
        if (user != null) {
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
    }"""

content = re.sub(auth_orig, auth_new, content, flags=re.MULTILINE)

reg_orig = r"""    suspend fun registerUser\(name: String, email: String, phone: String, role: UserRole, password: String = "123456"\): Long \{
        val user = User\(
            name = name,
            role = role,
            status = if \(role == UserRole.ADMIN\) UserStatus.APPROVED else UserStatus.PENDING,
            email = email.ifBlank \{ "\$\{name.lowercase\(\).replace\(" ", ""\)\}@thebar.com" \},
            phone = phone,
            password = password
        \)"""

reg_new = """    suspend fun registerUser(name: String, email: String, phone: String, role: UserRole, password: String = "123456"): Long {
        val finalEmail = email.ifBlank { "${name.lowercase().replace(" ", "")}@thebar.com" }
        try {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(finalEmail).matches()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(finalEmail, password).await()
            }
        } catch (e: Exception) {}
        val user = User(
            name = name,
            role = role,
            status = if (role == UserRole.ADMIN) UserStatus.APPROVED else UserStatus.PENDING,
            email = finalEmail,
            phone = phone,
            password = password
        )"""

content = re.sub(reg_orig, reg_new, content, flags=re.MULTILINE)

att_orig = r"""    suspend fun createAttendantByAdmin\(name: String, email: String, phone: String, initialPassword: String\): Long \{
        val user = User\(
            name = name,
            role = UserRole.ATTENDANT,
            status = UserStatus.APPROVED,
            email = email.ifBlank \{ "\$\{name.lowercase\(\).replace\(" ", ""\)\}@thebar.com" \},
            phone = phone,
            password = initialPassword.ifBlank \{ "123456" \}
        \)"""

att_new = """    suspend fun createAttendantByAdmin(name: String, email: String, phone: String, initialPassword: String): Long {
        val finalEmail = email.ifBlank { "${name.lowercase().replace(" ", "")}@thebar.com" }
        val pass = initialPassword.ifBlank { "123456" }
        try {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(finalEmail).matches()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(finalEmail, pass).await()
            }
        } catch (e: Exception) {}
        val user = User(
            name = name,
            role = UserRole.ATTENDANT,
            status = UserStatus.APPROVED,
            email = finalEmail,
            phone = phone,
            password = pass
        )"""

content = re.sub(att_orig, att_new, content, flags=re.MULTILINE)

with open(file_path, "w") as f:
    f.write(content)

