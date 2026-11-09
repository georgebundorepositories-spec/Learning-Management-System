package zw.zimlms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.AuthRepository
import zw.zimlms.core.auth.AuthenticatedSchoolLogin
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.OfflineLogin
import zw.zimlms.core.auth.OfflineVault
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.core.auth.SessionVault
import zw.zimlms.core.sync.CalendarSyncScheduler
import zw.zimlms.ui.BrandMark
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice
import zw.zimlms.ui.ZimSchoolTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ZimSchoolTheme { ZimSchoolApp() }
    }
  }
}

@Composable
private fun ZimSchoolApp() {
  val context = LocalContext.current.applicationContext
  val vault = remember { OfflineVault(context) }
  val repository = remember {
    AuthRepository(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY)
  }
  val scope = rememberCoroutineScope()
  var identifier by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var pin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  var pendingProfile by remember { mutableStateOf<CachedProfile?>(null) }
  var pendingSession by remember { mutableStateOf<AuthenticatedSchoolLogin?>(null) }
  var loggedInProfile by remember { mutableStateOf<CachedProfile?>(null) }

  val current = loggedInProfile
  if (current!=null) {
    // The authenticated workspace owns the entire screen. It is no longer
    // nested in the old LoginScreen Column/verticalScroll.
    SchoolWorkspace(profile=current, onSignOut={
      if(!busy) {
        busy=true
        CalendarSyncScheduler.cancel(context)
        scope.launch {
          val removed=runCatching {
            SessionBroker(context,BuildConfig.SUPABASE_URL,
              BuildConfig.SUPABASE_PUBLISHABLE_KEY).signOut()
          }.recoverCatching {
            // Offline sign-out must fail closed locally even if Auth is down.
            SessionVault(context).clear()
            false
          }
          if(removed.isSuccess) {
            vault.clear()
            loggedInProfile=null
            pendingProfile=null
            pendingSession=null
            identifier=""
            password=""
            pin=""
            error=null
          } else {
            error="Could not securely clear this device. Retry sign-out."
          }
          busy=false
        }
      }
    })
    return
  }

  Column(
    modifier=Modifier.fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .imePadding()
      .padding(horizontal=22.dp,vertical=30.dp),
    verticalArrangement=Arrangement.spacedBy(18.dp)
  ) {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      BrandMark(Modifier.size(52.dp))
      Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text("ZimSchool",style=MaterialTheme.typography.headlineSmall)
        Text("LEARNING MANAGEMENT SYSTEM",
          style=MaterialTheme.typography.labelSmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Spacer(Modifier.height(12.dp))
    Eyebrow("WELCOME TO YOUR SCHOOL")
    Text(
      if(pendingProfile!=null) "Secure your offline access"
      else "A better school day starts here.",
      style=MaterialTheme.typography.headlineMedium
    )
    Text(
      if(pendingProfile!=null)
        "Create a private PIN to access approved offline tools on this device."
      else "Sign in to your school workspace, people, learning materials and calendar.",
      style=MaterialTheme.typography.bodyMedium,
      color=MaterialTheme.colorScheme.onSurfaceVariant
    )
    Card(
      colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
      shape=RoundedCornerShape(24.dp),
      border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
      modifier=Modifier.fillMaxWidth()
    ) {
      Column(
        modifier=Modifier.padding(21.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
      ) {
        if(pendingProfile!=null) {
          Text("Create offline PIN",style=MaterialTheme.typography.titleLarge)
          Text("Choose 4–8 digits. Your PIN and identity are protected by Android Keystore.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          OutlinedTextField(
            value=pin,
            onValueChange={ pin=it.filter(Char::isDigit).take(8) },
            modifier=Modifier.fillMaxWidth(),
            singleLine=true,
            label={Text("New offline PIN")},
            visualTransformation=PasswordVisualTransformation(),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),
            shape=RoundedCornerShape(13.dp)
          )
          Button(
            enabled=!busy && pin.length in 4..8,
            onClick={
              val profileToEnroll=pendingProfile!!
              val submittedPin=pin
              busy=true
              error=null
              scope.launch {
                val result=runCatching {
                  withContext(Dispatchers.Default) { vault.enroll(profileToEnroll,submittedPin) }
                  val login=pendingSession ?: error("No online session is available")
                  withContext(Dispatchers.IO) {
                    SessionBroker(context,BuildConfig.SUPABASE_URL,
                      BuildConfig.SUPABASE_PUBLISHABLE_KEY).store(login)
                  }
                }
                if(result.isFailure) {
                  vault.clear()
                  SessionVault(context).clear()
                }
                result.fold(
                  onSuccess={
                    loggedInProfile=profileToEnroll
                    pendingProfile=null
                    pendingSession=null
                    pin=""
                    password=""
                    error=null
                    CalendarSyncScheduler.schedule(context)
                  },
                  onFailure={error=context.getString(R.string.pin_save_error)}
                )
                busy=false
              }
            },
            modifier=Modifier.fillMaxWidth().height(50.dp),
            shape=RoundedCornerShape(13.dp)
          ) {
            if(busy) CircularProgressIndicator(Modifier.size(22.dp),
              strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary)
            else Text("Protect PIN & open workspace")
          }
        } else {
          Text("School member sign in",style=MaterialTheme.typography.titleLarge)
          Text("Use the email or phone number provided to your school.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          OutlinedTextField(
            value=identifier,onValueChange={identifier=it},
            label={Text(stringResource(R.string.identifier))},
            singleLine=true,modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(13.dp),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email)
          )
          OutlinedTextField(
            value=password,onValueChange={password=it},
            label={Text(stringResource(R.string.password))},
            singleLine=true,modifier=Modifier.fillMaxWidth(),
            visualTransformation=PasswordVisualTransformation(),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
            shape=RoundedCornerShape(13.dp)
          )
          Button(
            enabled=!busy && identifier.isNotBlank() && password.isNotBlank(),
            onClick={
              busy=true
              error=null
              scope.launch {
                val result=withContext(Dispatchers.IO){
                  runCatching {repository.loginWithSession(identifier,password)}
                }
                result.fold(
                  onSuccess={
                    pendingProfile=it.profile
                    pendingSession=it
                    password=""
                  },
                  onFailure={error=context.getString(R.string.login_failure)}
                )
                busy=false
              }
            },
            modifier=Modifier.fillMaxWidth().height(50.dp),
            shape=RoundedCornerShape(13.dp)
          ) {
            if(busy) CircularProgressIndicator(Modifier.size(22.dp),
              strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary)
            else Text("Sign in securely")
          }
          HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
          Text("Already enrolled on this device?",
            style=MaterialTheme.typography.titleSmall)
          Text("Unlock offline learning and locally saved calendar work with your PIN.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          OutlinedTextField(
            value=pin,onValueChange={pin=it.filter(Char::isDigit).take(8)},
            modifier=Modifier.fillMaxWidth(),
            singleLine=true,
            label={Text("Offline PIN")},
            visualTransformation=PasswordVisualTransformation(),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),
            shape=RoundedCornerShape(13.dp)
          )
          OutlinedButton(
            enabled=!busy && pin.isNotBlank(),
            onClick={
              busy=true
              error=null
              scope.launch {
                val submittedPin=pin
                val result=withContext(Dispatchers.Default) {vault.unlock(submittedPin)}
                pin=""
                when(result) {
                  is OfflineLogin.Success -> {
                    loggedInProfile=result.profile
                    val session=SessionVault(context).load()
                    if(session?.profile?.userId==result.profile.userId &&
                      session.profile.schoolId==result.profile.schoolId) {
                      CalendarSyncScheduler.schedule(context)
                    }
                  }
                  is OfflineLogin.Rejected -> {error=result.reason}
                }
                busy=false
              }
            },
            modifier=Modifier.fillMaxWidth().height(49.dp),
            shape=RoundedCornerShape(13.dp)
          ) {
            Text("Unlock offline")
          }
        }
      }
    }
    error?.let { problem->
      StateNotice("Action needed",problem,MaterialTheme.colorScheme.error)
    }
    Text(
      "PRIVATE BY DESIGN  •  School invitation required  •  Offline-first",
      style=MaterialTheme.typography.labelSmall,
      color=MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
