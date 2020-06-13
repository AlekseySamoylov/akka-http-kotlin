package com.alekseysamoylov.akka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.model.*
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import akka.stream.Materializer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import java.util.concurrent.CompletionStage
import java.util.function.Supplier
import javax.ws.rs.Path
import akka.stream.javadsl.Flow;


@Api(value = "/", produces = "text/html")
@Path("/")
class MinimalHttpApp : AllDirectives() {
    @Path("/hello")
    @ApiOperation(value = "hello", code = 200, nickname = "hello", httpMethod = "GET", response = String::class)
    @ApiResponses(value = [ApiResponse(code = 500, message = "Internal server error")])
    fun createRoute(): Route? {
        val entity: HttpEntity.Strict =
            HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, "<h1>Say hello to akka-http</h1>")
        return concat(
            path(
                "hello"
            ) {
                get {
                    complete(
                        entity
                    )
                }
            }
        )
    }
}

//fun main() {
//    // boot up server using the route as defined below
//    // boot up server using the route as defined below
//    val system = ActorSystem.create("routes")
//
//    val http: Http = Http.get(system)
//    val materializer = Materializer.createMaterializer(system)
//
//    // In order to access all directives we need an instance where the routes
//    // are define.
//
//    // In order to access all directives we need an instance where the routes
//    // are define.
//    val app = MinimalHttpApp()
//
//    val swaggerDocService = SwaggerDocService()
//    val routes: Route = app.concat(
//        app.createRoute(),
//        swaggerDocService.createRoute(),
//        swaggerDocService.createUiRoute()
//    )
//    val routeFlow: Flow<HttpRequest, HttpResponse, NotUsed> = routes.flow(system, materializer)
//    val binding: CompletionStage<ServerBinding> =
//        http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 12345), materializer)
//
//    println("Server online at http://localhost:12345/\nPress RETURN to stop...")
//    System.`in`.read()
//
//    binding
//        .thenCompose(ServerBinding::unbind)
//        .thenAccept { system.terminate() }
//
//
//}
