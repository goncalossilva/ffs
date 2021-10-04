import react.dom.render
import kotlinx.browser.document
import react.dom.h1

fun main() {
    render(document.getElementById("root")) {
        h1 {
            +"Hello, world!"
        }
    }
}
