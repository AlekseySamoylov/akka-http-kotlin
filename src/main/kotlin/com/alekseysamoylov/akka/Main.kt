package com.alekseysamoylov.akka

import akka.actor.ActorSystem
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.model.ContentTypes
import akka.http.javadsl.model.HttpEntities
import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.server.Directives.complete
import akka.http.javadsl.server.Directives.pathEndOrSingleSlash
import akka.stream.Materializer


fun main() {
    val system = ActorSystem.create("AkkaHttpPlayground")
    val http = Http.get(system)
    val materializer = Materializer.createMaterializer(system)

    val simpleRoute =
        pathEndOrSingleSlash { complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, """
            <html>
             <body>
               Rock the JVM with Akka HTTP!
             </body>
            </html>
        """.trimIndent())) }
            .flow(system, materializer)
    val bindingFuture = http.bindAndHandle(simpleRoute, ConnectHttp.toHost("localhost", 8080), materializer)

    println("Type RETURN to exit")
    System.`in`.read()
    bindingFuture
        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
        .thenAccept { unbound -> system.terminate() } // and shutdown when done
}
