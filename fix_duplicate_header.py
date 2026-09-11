import re
file_path = "app/src/main/java/com/example/ui/screens/AuthScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

bad_chunk = """                    AuthMode.PHONE_AUTH -> {
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
                }"""

content = content.replace(bad_chunk, "")

with open(file_path, "w") as f:
    f.write(content)
