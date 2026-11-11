package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.portals.PortalInvoice
import zw.zimlms.portals.PortalNotice
import zw.zimlms.portals.SchoolOperationsRepository
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.StateNotice

/** Notice and invoice pages intentionally remain online-only; no learner
 * account or payment details are cached in plaintext on shared devices.
 */
@Composable
fun SchoolNoticesPage(profile:CachedProfile,modifier:Modifier=Modifier){
  val context=LocalContext.current.applicationContext
  val repo=remember(profile.userId) {
    // A QA build, a device without an active server configuration, or a
    // revoked session must display an actionable state rather than crash.
    runCatching {
      SchoolOperationsRepository(BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
    }.getOrNull()
  }
  var notices by remember(profile.userId){ mutableStateOf<List<PortalNotice>>(emptyList()) }
  var loading by remember(profile.userId){mutableStateOf(false)}
  var error by remember(profile.userId){mutableStateOf<String?>(null)}
  var refresh by remember(profile.userId){mutableIntStateOf(0)}
  LaunchedEffect(profile.userId,refresh) {
    if(repo==null){
      notices=emptyList()
      error="This build has no configured school connection. The offline learning library remains available."
      loading=false
      return@LaunchedEffect
    }
    loading=true
    val result=runCatching{withContext(Dispatchers.IO){repo.notices(profile)}}
    result.fold(onSuccess={notices=it;error=null},onFailure={
      notices=emptyList()
      error="School notices are unavailable. Check your connection and permissions."
    })
    loading=false
  }
  LazyColumn(modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)) {
    item {
      Eyebrow("SCHOOL COMMUNICATIONS")
      Text("Notices",style=MaterialTheme.typography.headlineMedium)
      Text("Announcements addressed to your school account.",
        style=MaterialTheme.typography.bodyMedium)
    }
    item {
      StateNotice("In-app only","Messages on this screen have not been sent as SMS or push notifications.")
      OutlinedButton(onClick={refresh++},enabled=!loading&&repo!=null,modifier=Modifier.fillMaxWidth()){
        Text(if(loading)"Loading notices…" else "Refresh notices")
      }
    }
    if(error!=null)item { StateNotice("Unable to load",error!!) }
    else if(notices.isEmpty()&&!loading)item {
      StateNotice("No notices","There are no current announcements for your account.")
    }
    items(notices,key={it.id}){notice->
      AppPanel(Modifier.fillMaxWidth()){
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
          Text(notice.title,style=MaterialTheme.typography.titleMedium)
          Text(notice.createdAt.take(10)+" · "+notice.audience,
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
          Text(notice.body,style=MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
fun SchoolFeesPage(profile:CachedProfile,modifier:Modifier=Modifier){
  val context=LocalContext.current.applicationContext
  val repo=remember(profile.userId) {
    // A QA build, a device without an active server configuration, or a
    // revoked session must display an actionable state rather than crash.
    runCatching {
      SchoolOperationsRepository(BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        SessionBroker(context,BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY))
    }.getOrNull()
  }
  var invoices by remember(profile.userId){mutableStateOf<List<PortalInvoice>>(emptyList())}
  var loading by remember(profile.userId){mutableStateOf(false)}
  var error by remember(profile.userId){mutableStateOf<String?>(null)}
  var refresh by remember(profile.userId){mutableIntStateOf(0)}
  LaunchedEffect(profile.userId,refresh) {
    if(repo==null){
      invoices=emptyList()
      error="This build has no configured school connection. No invoices can be loaded offline."
      loading=false
      return@LaunchedEffect
    }
    loading=true
    val result=runCatching{withContext(Dispatchers.IO){repo.invoices(profile)}}
    result.fold(onSuccess={invoices=it;error=null},onFailure={
      invoices=emptyList()
      error="Invoices are unavailable. Check your school membership and connection."
    })
    loading=false
  }
  LazyColumn(modifier=modifier.fillMaxSize(),
    contentPadding=PaddingValues(horizontal=18.dp,vertical=22.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)) {
    item {
      Eyebrow("SCHOOL FINANCE")
      Text("Fee invoices",style=MaterialTheme.typography.headlineMedium)
      Text("Only charges visible to your authorised school account.",
        style=MaterialTheme.typography.bodyMedium)
    }
    item {
      StateNotice("Not a payment screen","An invoice is not proof of payment. Online payment and receipts are not enabled.")
      OutlinedButton(onClick={refresh++},enabled=!loading,modifier=Modifier.fillMaxWidth()){
        Text(if(loading)"Loading invoices…" else "Refresh invoices")
      }
    }
    if(error!=null)item {StateNotice("Unable to load",error!!)}
    else if(invoices.isEmpty()&&!loading)item {
      StateNotice("No invoices","No authorised invoices are available for this account.")
    }
    items(invoices,key={it.id}){invoice->
      AppPanel(Modifier.fillMaxWidth()){
        Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
          Text(invoice.description,style=MaterialTheme.typography.titleMedium)
          Text(invoice.currency+" "+invoice.amount,style=MaterialTheme.typography.headlineSmall)
          Text("Status: "+invoice.status+
            (invoice.dueOn?.let {" · Due: $it"} ?: ""),
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}
