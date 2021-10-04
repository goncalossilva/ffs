import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO

fun main() {
    embeddedServer(CIO, port = 8080) {
        routing {
            get("/") {
                call.respond(200)
            }
        }
    }.start(wait = true)
}
