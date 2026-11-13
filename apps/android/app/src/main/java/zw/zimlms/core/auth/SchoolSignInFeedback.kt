package zw.zimlms.core.auth

import java.io.IOException

/** Stable, safe user-facing reasons. Never print a raw Auth response or token. */
enum class SchoolSignInProblem {
  INVALID_IDENTIFIER,
  INCORRECT_CREDENTIALS,
  EMAIL_NOT_CONFIRMED,
  TOO_MANY_ATTEMPTS,
  SCHOOL_NOT_LINKED,
  SCHOOL_INACTIVE,
  SERVER_UNAVAILABLE,
  APP_NOT_CONFIGURED,
  INVALID_SESSION,
  PROFILE_UNAVAILABLE
}

class SchoolSignInFailure(val problem: SchoolSignInProblem) : IOException(
  when (problem) {
    SchoolSignInProblem.INVALID_IDENTIFIER -> "Enter a valid email address or Zimbabwe mobile number."
    SchoolSignInProblem.INCORRECT_CREDENTIALS -> "Email, mobile number or password not recognised. Check your details and try again."
    SchoolSignInProblem.EMAIL_NOT_CONFIRMED -> "Confirm your email address using the message sent by your school before signing in."
    SchoolSignInProblem.TOO_MANY_ATTEMPTS -> "Too many sign-in attempts. Try again later or contact your school administrator."
    SchoolSignInProblem.SCHOOL_NOT_LINKED -> "Password accepted, but this account is not yet registered with a school. Ask your school administrator to assign your school and role."
    SchoolSignInProblem.SCHOOL_INACTIVE -> "Your school account is inactive. Ask your school administrator to reactivate it."
    SchoolSignInProblem.SERVER_UNAVAILABLE -> "The school sign-in service is temporarily unavailable. Please retry."
    SchoolSignInProblem.APP_NOT_CONFIGURED -> "This app is not configured to connect to the school server. Install the school's official build."
    SchoolSignInProblem.INVALID_SESSION -> "Sign-in succeeded but the server did not provide a usable session. Please retry."
    SchoolSignInProblem.PROFILE_UNAVAILABLE -> "The school account could not be checked securely. Please retry or contact your school administrator."
  }
)

/** The UI never uses exception.toString() or raw API bodies. */
fun safeSchoolSignInMessage(error: Throwable): String = when (error) {
  is SchoolSignInFailure -> error.message.orEmpty()
  is IllegalArgumentException -> SchoolSignInFailure(SchoolSignInProblem.INVALID_IDENTIFIER).message.orEmpty()
  is IOException -> "Cannot reach the school server. Check mobile data or Wi-Fi, then try again."
  else -> "Could not sign in securely. Please retry or contact your school administrator."
}
