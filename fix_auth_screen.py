import re
file_path = "app/src/main/java/com/example/ui/screens/AuthScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Fix double PHONE_AUTH in enum
content = re.sub(r"enum class AuthMode \{\n    PHONE_AUTH,\n    LOGIN,\n    REGISTER_BAR,\n    RESET_PASSWORD,\n    PHONE_AUTH\n\}",
                 "enum class AuthMode {\n    LOGIN,\n    REGISTER_BAR,\n    RESET_PASSWORD,\n    PHONE_AUTH\n}", content)

# Remove extra PHONE_AUTH from when clauses and fix the onForgotPassword thing
content = content.replace("onForgotPassword = { authMode = AuthMode.RESET_PASSWORD,\n    PHONE_AUTH }", "onForgotPassword = { authMode = AuthMode.RESET_PASSWORD }")

# Let's fix missing imports
imports = """
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
"""
content = content.replace("import androidx.compose.runtime.mutableStateOf", "import androidx.compose.runtime.mutableStateOf" + imports)

with open(file_path, "w") as f:
    f.write(content)
