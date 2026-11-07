package zw.zimlms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val isDark = isSystemInDarkTheme()
      val scheme = if (isDark) darkColorScheme(
        primary = Color(0xFF7AD8AE), secondary = Color(0xFFF4A261)
      ) else lightColorScheme(
        primary = Color(0xFF0B6E4F), secondary = Color(0xFFF4A261)
      )
      MaterialTheme(colorScheme = scheme) { LoginScreen() }
    }
  }
}

@Composable private fun LoginScreen() {
  val context = LocalContext.current
  val vault = remember { OfflineVault(context.applicationContext) }
  val repo = remember { AuthRepository(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) }
  val scope = rememberCoroutineScope()

  var identifier by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var pin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  var pendingProfile by remember { mutableStateOf<CachedProfile?>(null) }
  var loggedInProfile by remember { mutableStateOf<CachedProfile?>(null) }
  val scroll = rememberScrollState()

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge)

    when {
      loggedInProfile != null -> {
        val current = loggedInProfile!!
        Text(stringResource(R.string.welcome, current.fullName),
          style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.role, current.role))
        Text(stringResource(R.string.offline_note))
        Button(onClick = {
          vault.clear()
          loggedInProfile = null
          password = ""; pin = ""; error = null
        }) { Text(stringResource(R.string.sign_out)) }
      }

      pendingProfile != null -> {
        Text(stringResource(R.string.set_pin), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.set_pin_help))
        OutlinedTextField(
          value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(8) },
          label = { Text(stringResource(R.string.offline_pin)) }, singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier.fillMaxWidth()
        )
        Button(enabled = pin.length in 4..8, onClick = {
          try {
            vault.enroll(pendingProfile!!, pin)
            loggedInProfile = pendingProfile
            pendingProfile = null
            pin = ""; password = ""; error = null
          } catch (_: Exception) { error = context.getString(R.string.pin_save_error) }
        }) { Text(stringResource(R.string.save_pin)) }
      }

      else -> {
        Text(stringResource(R.string.sign_in), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
          value = identifier, onValueChange = { identifier = it },
          label = { Text(stringResource(R.string.identifier)) },
          singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = password, onValueChange = { password = it },
          label = { Text(stringResource(R.string.password)) },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Button(
          enabled = !busy && identifier.isNotBlank() && password.isNotBlank(),
          onClick = {
            busy = true; error = null
            scope.launch {
              val result = withContext(Dispatchers.IO) {
                runCatching { repo.login(identifier, password) }
              }
              busy = false
              result.fold(
                onSuccess = { pendingProfile = it; password = "" },
                onFailure = { error = context.getString(R.string.login_failure) }
              )
            }
          }
        ) { Text(stringResource(if (busy) R.string.signing_in else R.string.sign_in)) }

        HorizontalDivider()
        Text(stringResource(R.string.offline_access), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
          value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(8) },
          label = { Text(stringResource(R.string.offline_pin)) },
          singleLine = true, visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
          enabled = !busy && pin.isNotBlank(),
          onClick = {
            error = null
            // Run the expensive PIN derivation off the main thread.
            busy = true
            scope.launch {
              val result = withContext(Dispatchers.Default) { vault.unlock(pin) }
              busy = false; pin = ""
              when(result) {
                is OfflineLogin.Success -> loggedInProfile = result.profile
                is OfflineLogin.Rejected -> error = result.reason
              }
            }
          }
        ) { Text(stringResource(R.string.unlock_offline)) }

        Text(stringResource(R.string.invite_only), style = MaterialTheme.typography.bodySmall)
      }
    }
    error?.let {
      Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
  }
}
