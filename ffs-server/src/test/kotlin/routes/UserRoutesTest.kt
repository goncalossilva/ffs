package doist.ffs.routes

import doist.ffs.db.SelectById
import doist.ffs.ext.bodyAsJson
import doist.ffs.ext.setBodyForm
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UserRoutesTest {
    @Test
    fun registerLogin() = testApplication {
        val client = createClient { }

        // Register a user and verify it was created.
        val registerResponse = client.post("$PATH_USERS$PATH_REGISTER") {
            setBodyForm(
                "name" to "Gonçalo Silva",
                "email" to "goncalo@doist.com",
                "password" to "password123"
            )
        }
        assert(registerResponse.status == HttpStatusCode.Created)

        // Login as the user and verify the details match.
        val loginResponse = client.post("$PATH_USERS$PATH_LOGIN") {
            setBodyForm("email" to "goncalo@doist.com", "password" to "password123")
        }
        assert(loginResponse.status == HttpStatusCode.OK)
        val user = loginResponse.bodyAsJson<SelectById>()
        assert(user.name == "Gonçalo Silva")
        assert(user.email == "goncalo@doist.com")
    }

    @Test
    fun registerInvalidEmail() = testApplication {
        listOf("goncalo@doist.c", "goncalo@127.0.0.1", "@doist.com").forEach { email ->
            assertFailsWith<ClientRequestException> {
                client.post("$PATH_USERS$PATH_REGISTER") {
                    setBodyForm("name" to "Gonçalo", "email" to email, "password" to "password123")
                }
            }
        }
    }

    @Test
    fun registerInvalidPassword() = testApplication {
        assertFailsWith<ClientRequestException> {
            client.post("$PATH_USERS$PATH_REGISTER") {
                setBodyForm(
                    "name" to "Gonçalo",
                    "email" to "goncalo@doist.com",
                    "password" to "1234567"
                )
            }
        }
    }

    @Test
    fun update() = testApplication {
        val client = createClient {
            install(HttpCookies)
        }
        val id = client.post("$PATH_USERS$PATH_REGISTER") {
            setBodyForm(
                "name" to "Gonçalo",
                "email" to "goncalo@doist.com",
                "password" to "password123"
            )
        }.headers[HttpHeaders.Location]!!.substringAfterLast('/')

        // Update various user details.
        client.put(PATH_USER(id)) {
            setBodyForm("name" to "Gonçalo")
        }
        client.put(PATH_USER(id)) {
            setBodyForm("email" to "goncalo@doist.io", "current_password" to "password123")
        }
        client.put(PATH_USER(id)) {
            setBodyForm("name" to "Gonçalo", "current_password" to "password123")
        }

        // Login to obtain user info again, and verify it.
        val loginResponse = client.post("$PATH_USERS$PATH_LOGIN") {
            setBodyForm("email" to "goncalo@doist.io", "password" to "password123")
        }
        val user = loginResponse.bodyAsJson<SelectById>()
        assert(user.name == "Gonçalo")
        assert(user.email == "goncalo@doist.io")
    }

    @Test
    fun updateEmailInvalid() = testApplication {
        val client = createClient {
            install(HttpCookies)
        }
        val id = client.post("$PATH_USERS$PATH_REGISTER") {
            setBodyForm(
                "name" to "Gonçalo",
                "email" to "goncalo@doist.com",
                "password" to "password123"
            )
        }.headers[HttpHeaders.Location]!!.substringAfterLast('/')

        listOf("goncalo@doist.c", "goncalo@127.0.0.1", "@doist.com").forEach { email ->
            assertFailsWith<ClientRequestException> {
                client.put(PATH_USER(id)) {
                    setBodyForm("email" to email, "current_password" to "password123")
                }
            }
        }
    }

    @Test
    fun updateCurrentPasswordInvalid() = testApplication {
        val client = createClient {
            install(HttpCookies)
        }
        val id = client.post("$PATH_USERS$PATH_REGISTER") {
            setBodyForm(
                "name" to "Gonçalo",
                "email" to "goncalo@doist.com",
                "password" to "password123"
            )
        }.headers[HttpHeaders.Location]!!.substringAfterLast('/')

        assertFailsWith<ClientRequestException> {
            client.put(PATH_USER(id)) {
                setBodyForm("email" to "goncalo@doist.io")
            }
        }
        assertFailsWith<ClientRequestException> {
            client.put(PATH_USER(id)) {
                setBodyForm("email" to "goncalo@doist.io", "current_password" to "wrongpassword")
            }
        }
        assertFailsWith<ClientRequestException> {
            client.put(PATH_USER(id)) {
                setBodyForm("password" to "newpassword")
            }
        }
        assertFailsWith<ClientRequestException> {
            client.put(PATH_USER(id)) {
                setBodyForm("password" to "newpassword", "current_password" to "wrongpassword")
            }
        }
    }

    @Test
    fun delete() = testApplication {
        val client = createClient {
            install(HttpCookies)
        }
        val id = client.post("$PATH_USERS$PATH_REGISTER") {
            setBodyForm(
                "name" to "Gonçalo",
                "email" to "goncalo@doist.com",
                "password" to "password123"
            )
        }.headers[HttpHeaders.Location]!!.substringAfterLast('/')

        val deleteResponse = client.delete(PATH_USER(id)) {
            setBodyForm("current_password" to "password123")
        }
        assert(deleteResponse.status == HttpStatusCode.NoContent)

        // Ensure one can't login as a deleted user.
        assertFailsWith<ClientRequestException> {
            client.post("$PATH_USERS$PATH_LOGIN") {
                setBodyForm("email" to "goncalo@doist.com", "password" to "password123")
            }
        }
    }

    @Test
    fun deleteCurrentPasswordInvalid() = testApplication {
        val client = createClient {
            install(HttpCookies)
        }
        val id = client.post("$PATH_USERS$PATH_REGISTER") {
            setBodyForm(
                "name" to "Gonçalo",
                "email" to "goncalo@doist.com",
                "password" to "password123"
            )
        }.headers[HttpHeaders.Location]!!.substringAfterLast('/')

        assertFailsWith<ClientRequestException> {
            client.delete(PATH_USER(id)) {
                setBodyForm("current_password" to "wrongpassword")
            }
        }
    }
}
