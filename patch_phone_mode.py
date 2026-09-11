import re

file_path = "app/src/main/java/com/example/ui/screens/AuthScreen.kt"
with open(file_path, "r") as f:
    content = f.read()


phone_auth_content = """                    AuthMode.PHONE_AUTH -> {
                        PhoneAuthContent(
                            phoneAuthManager = phoneAuthManager,
                            onLoginPhoneCredential = onLoginPhoneCredential,
                            onBackToLogin = { authMode = AuthMode.LOGIN }
                        )
                    }
                    AuthMode.RESET_PASSWORD"""
                    
content = content.replace("                    AuthMode.RESET_PASSWORD", phone_auth_content)


phone_auth_header = """                AuthMode.PHONE_AUTH -> {
                    Text(
                        text = "Phone Sign In",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Verify your number via SMS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AuthMode.RESET_PASSWORD"""
                
content = content.replace("                AuthMode.RESET_PASSWORD", phone_auth_header)

with open(file_path, "w") as f:
    f.write(content)
