import re

file_path = "app/src/main/java/com/example/MainActivity.kt"
with open(file_path, "r") as f:
    content = f.read()

orig = r"""                        onSelfResetPassword = { identifier, newPassword ->
                            viewModel.resetPassword(identifier, newPassword)
                        }"""

new_val = """                        onSelfResetPassword = { identifier, newPassword ->
                            viewModel.resetPassword(identifier, newPassword)
                        },
                        phoneAuthManager = viewModel.phoneAuthManager,
                        onLoginPhoneCredential = { cred, phone ->
                            viewModel.loginWithPhoneCredential(cred, phone)
                        }"""

content = re.sub(orig, new_val, content)

with open(file_path, "w") as f:
    f.write(content)
