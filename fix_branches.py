import re
file_path = "app/src/main/java/com/example/ui/screens/AuthScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("AuthMode.RESET_PASSWORD,\n    PHONE_AUTH -> {", "AuthMode.RESET_PASSWORD -> {")

with open(file_path, "w") as f:
    f.write(content)
