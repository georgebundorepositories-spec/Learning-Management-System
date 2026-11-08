package zw.zimlms.attendance

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import zw.zimlms.core.auth.CachedProfile
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class AttendanceRepositoryTest {
  private val school="11111111-1111-4111-8111-111111111111"
  private val user="22222222-2222-4222-8222-222222222222"
  private val group="33333333-3333-4333-8333-333333333333"
  private val learnerA="44444444-4444-4444-8444-444444444444"
  private val learnerB="55555555-5555-4555-8555-555555555555"
  private val profile=CachedProfile(user,"Test Head","head",school)
  private fun reply(request: Request,body:String,code:Int=200) =
    Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
      .code(code).message(if(code==200)"OK" else "Denied")
      .body(body.toResponseBody("application/json".toMediaType())).build()

  @Test fun rosterIsJWTAuthenticatedAndParsesOnlyNameAndStudentId() = runBlocking {
    val calls=AtomicInteger()
    val http=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      val req=chain.request()
      calls.incrementAndGet()
      assertEquals("POST",req.method)
      assertEquals("Bearer synthetic-jwt",req.header("Authorization"))
      assertEquals("/rest/v1/rpc/get_attendance_roster",req.url.encodedPath)
      val body=Buffer().apply { req.body!!.writeTo(this) }.readUtf8()
      assertEquals(group,org.json.JSONObject(body).getString("p_class_id"))
      reply(req,"[{\"student_user_id\":\"$learnerA\",\"display_name\":\"Fictional Learner\"}]")
    }).build()
    val client=AttendanceRepository("https://test.invalid","sb_publishable_ci",
      { "synthetic-jwt" },http)
    val roster=client.loadRoster(profile,group)
    assertEquals(1,calls.get())
    assertEquals(1,roster.size)
    assertEquals("Fictional Learner",roster.first().name)
    assertEquals(learnerA,roster.first().userId)
  }

  @Test fun dailyAttendanceFiltersByClassAndDate() = runBlocking {
    val http=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      val req=chain.request()
      assertEquals("GET",req.method)
      assertEquals("/rest/v1/attendance",req.url.encodedPath)
      assertEquals("eq.$group",req.url.queryParameter("class_id"))
      assertEquals("eq.2026-11-08",req.url.queryParameter("date"))
      reply(req,"[{\"student_user_id\":\"$learnerA\",\"status\":\"late\"}]")
    }).build()
    val client=AttendanceRepository("https://test.invalid","sb_publishable_ci",
      { "synthetic-jwt" },http)
    val day=client.loadDay(profile,group,LocalDate.parse("2026-11-08"))
    assertEquals(listOf(AttendanceMark(learnerA,"late")),day)
  }

  @Test fun bulkUpsertRequiresFullConfirmedRoster() = runBlocking {
    val calls=AtomicInteger()
    val http=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      val req=chain.request()
      calls.incrementAndGet()
      assertEquals("POST",req.method)
      assertEquals("/rest/v1/attendance",req.url.encodedPath)
      assertEquals("class_id,student_user_id,date",req.url.queryParameter("on_conflict"))
      assertTrue(req.header("Prefer")!!.contains("merge-duplicates"))
      val sent=JSONArray(Buffer().apply { req.body!!.writeTo(this) }.readUtf8())
      assertEquals(2,sent.length())
      for(i in 0 until sent.length()) {
        val row=sent.getJSONObject(i)
        assertEquals(school,row.getString("school_id"))
        assertEquals(user,row.getString("marked_by"))
        assertEquals("2026-11-08",row.getString("date"))
        assertFalse(row.has("email"))
      }
      reply(req,"[{\"student_user_id\":\"$learnerA\",\"status\":\"present\"}," +
        "{\"student_user_id\":\"$learnerB\",\"status\":\"absent\"}]")
    }).build()
    val client=AttendanceRepository("https://test.invalid","sb_publishable_ci",
      { "synthetic-jwt" },http)
    val saved=client.saveDay(profile,group,LocalDate.parse("2026-11-08"),
      mapOf(learnerA to "present",learnerB to "absent"))
    assertEquals(2,saved)
    assertEquals(1,calls.get())
  }

  @Test fun rejectsPartialAckNoFalseSaveMessage() = runBlocking {
    val http=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      reply(chain.request(),"[{\"student_user_id\":\"$learnerA\",\"status\":\"present\"}]")
    }).build()
    val client=AttendanceRepository("https://test.invalid","sb_publishable_ci",
      { "synthetic-jwt" },http)
    try {
      client.saveDay(profile,group,LocalDate.parse("2026-11-08"),
        mapOf(learnerA to "present",learnerB to "absent"))
      fail("Partial attendance save cannot be accepted")
    } catch(expected:IllegalArgumentException) {
      assertTrue(expected.message!!.contains("did not confirm"))
    }
  }

  @Test fun invalidStudentAndStatusRejectedWithoutNetwork() = runBlocking {
    val calls=AtomicInteger()
    val http=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
      calls.incrementAndGet()
      throw IOException("Unexpected network request")
    }).build()
    val client=AttendanceRepository("https://test.invalid","sb_publishable_ci",
      { "synthetic-jwt" },http)
    try {
      client.saveDay(profile,group,LocalDate.parse("2026-11-08"),
        mapOf(learnerA to "not-a-status"))
      fail("Invalid status must be denied")
    }catch(_:IllegalArgumentException) {}
    assertEquals(0,calls.get())
  }

  @Test fun learnerCannotUseStaffSaveEndpoint() = runBlocking {
    val client=AttendanceRepository("https://test.invalid","sb_publishable_ci",
      { "synthetic-jwt" })
    try {
      client.saveDay(profile.copy(role="student"),group,LocalDate.parse("2026-11-08"),
        mapOf(learnerA to "present"))
      fail("Learner must not mark attendance")
    }catch(_:IllegalArgumentException) {}
  }
}
