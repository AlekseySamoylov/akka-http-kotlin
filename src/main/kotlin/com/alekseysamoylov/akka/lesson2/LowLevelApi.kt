package com.alekseysamoylov.akka.lesson2

import akka.NotUsed
import akka.actor.ActorSystem
import akka.dispatch.Dispatcher
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.IncomingConnection
import akka.http.javadsl.model.*
import akka.stream.Materializer
import akka.stream.javadsl.Flow
import akka.stream.javadsl.JavaFlowSupport
import akka.stream.javadsl.Sink
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import java.time.Duration
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.function.Function


class LowLevelApi {
}

fun main() {
    val system = ActorSystem.create("LowLevelServerApi")
    val materializer = Materializer.createMaterializer(system)
//    val dispatcher = ExecutionContextExecutor()
    val http = Http.get(system)
    val serverSource =
        http.bind(ConnectHttp.toHost("localhost", 8000))
    val connectionSink =
        Sink.foreach<IncomingConnection> { incomingConnection ->
            println("Accepted incoming connection from ${incomingConnection.remoteAddress()}")
        }

    val serverBindingFuture = serverSource.to(connectionSink).run(materializer)
    serverBindingFuture.thenAccept { binding ->
        println("Server binding successful")
//        binding.terminate(Duration.of(5, ChronoUnit.SECONDS))
    }.exceptionally { ex ->
        println("exception $ex")
        null
    }
    // Method 1: Synchronously

    val requestHandler: (HttpRequest) -> HttpResponse = { httpRequest ->
        when {
            httpRequest.method() == HttpMethods.GET -> HttpResponse.create()
                .withStatus(StatusCodes.OK)
                .withEntity(
                    HttpEntities.create(
                        ContentTypes.TEXT_HTML_UTF8, """
                    <html>
                    <body>
                    <h1>Hello</h1>
                    </body>
                    </html
                """.trimIndent()
                    )
                )
            httpRequest.method() == HttpMethods.POST -> HttpResponse.create()
                .withStatus(StatusCodes.OK)
                .withEntity(
                    HttpEntities.create(
                        ContentTypes.TEXT_HTML_UTF8, """
                    <html>
                    <body>
                    <h1>Hello Post</h1>
                    </body>
                    </html
                """.trimIndent()
                    )
                )
            else -> HttpResponse.create()
                .withStatus(StatusCodes.NOT_FOUND)
                .withEntity(
                    HttpEntities.create(
                        ContentTypes.TEXT_HTML_UTF8, """
                    <html>
                    <body>
                    Resource cannod be found
                    </body>
                    </html
                """.trimIndent()
                    )
                )
        }
    }

    val httpSyncConnectionHandler = Sink.foreach<IncomingConnection> { connection ->
        connection.handleWithSyncHandler(requestHandler, materializer)
    }
    http.bind(ConnectHttp.toHost("localhost", 8080)).to(httpSyncConnectionHandler).run(materializer)
//    http.bindAndHandleSync(requestHandler, ConnectHttp.toHost("localhost", 8080), materializer)


    // Akka streams
    val streamsBasedRequestHandler: Flow<HttpRequest, HttpResponse, NotUsed> =
        Flow.create<HttpRequest>().map(object : akka.japi.function.Function<HttpRequest, HttpResponse> {
            override fun apply(httpRequest: HttpRequest): HttpResponse {
                return when {
                    httpRequest.method() == HttpMethods.GET && httpRequest.uri.path() == "/" -> HttpResponse.create()
                        .withStatus(StatusCodes.OK)
                        .withEntity(
                            HttpEntities.create(
                                ContentTypes.TEXT_HTML_UTF8, """
                    <html>
                    <body>
                    <h1>Hello Akka streams</h1>
                    <a href="/home">go home</a>
                    </body>
                    </html
                """.trimIndent()
                            )
                        )
                    httpRequest.method() == HttpMethods.GET && httpRequest.uri.path() == "/home" -> HttpResponse.create()
                        .withStatus(StatusCodes.OK)
                        .withEntity(
                            HttpEntities.create(
                                ContentTypes.TEXT_HTML_UTF8, """
                    <html>
                    <body>
                    <h1>Hello Akka streams HOME</h1>
                    </body>
                    </html
                """.trimIndent()
                            )
                        )
                    else -> HttpResponse.create()
                        .withStatus(StatusCodes.NOT_FOUND)
                        .withEntity(
                            HttpEntities.create(
                                ContentTypes.TEXT_HTML_UTF8, """
                    <html>
                    <body>
                    Streams Resource cannot be found
                    </body>
                    </html
                """.trimIndent()
                            )
                        )
                }
            }
        }
        )

//    http.bind(ConnectHttp.toHost("localhost", 8081)).runForeach({ connection ->
//        connection.handleWith(streamsBasedRequestHandler, materializer)
//    }, materializer)

    val connectionSource = http.bindAndHandle(streamsBasedRequestHandler, ConnectHttp.toHost("localhost", 8081), materializer)




}
