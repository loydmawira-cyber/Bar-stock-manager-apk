import re

file_path = "app/src/main/java/com/example/ui/screens/AuthScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

orig_params = r"""    onRegisterBar: \(\) -> Unit,
    onNext: \(\) -> Unit\) \{"""
new_params = """    onRegisterBar: () -> Unit,
    onPhoneLogin: () -> Unit,
    onNext: () -> Unit) {"""
content = re.sub(orig_params, new_params, content)

orig_call = r"""                            onRegisterBar = \{ authMode = AuthMode.REGISTER_BAR \},
                            onNext = \{ focusManager.moveFocus\(FocusDirection.Down\) \}
                        \)"""
new_call = """                            onRegisterBar = { authMode = AuthMode.REGISTER_BAR },
                            onPhoneLogin = { authMode = AuthMode.PHONE_AUTH },
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )"""
content = re.sub(orig_call, new_call, content)


orig_buttons = r"""            Spacer\(modifier = Modifier.height\(18.dp\)\)

            HorizontalDivider\("""
new_buttons = """            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onPhoneLogin,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign In with Phone SMS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider("""
content = re.sub(orig_buttons, new_buttons, content)

with open(file_path, "w") as f:
    f.write(content)
