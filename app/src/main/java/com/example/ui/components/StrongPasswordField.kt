package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.PasswordValidator
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EmeraldGreen

@Composable
fun StrongPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password *",
    placeholder: String = "Min 8 chars, 1 uppercase, 1 digit, 1 special",
    modifier: Modifier = Modifier,
    testTag: String = "password_input",
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val validation = remember(value) { PasswordValidator.validate(value) }
    val isError = value.isNotEmpty() && !validation.isValid

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (validation.isValid) EmeraldGreen else if (isError) MaterialTheme.colorScheme.error else AmberPrimary
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = isError,
            supportingText = {
                if (value.isEmpty()) {
                    Text(
                        text = "Req: 8+ chars, uppercase (A-Z), lowercase (a-z), digit (0-9), special (!@#$)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!validation.isValid) {
                    Text(
                        text = validation.missingRequirementsMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "✓ Strong password",
                        fontSize = 11.sp,
                        color = EmeraldGreen
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (validation.isValid) EmeraldGreen else AmberPrimary,
                focusedLabelColor = if (validation.isValid) EmeraldGreen else AmberPrimary,
                cursorColor = AmberPrimary,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )
    }
}
