file_path = "app/src/main/java/com/example/ui/screens/AuthScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

import re
match = re.search(r"private fun LoginFormContent\(.*?^}", content, re.MULTILINE | re.DOTALL)
if match:
    print(match.group(0))
