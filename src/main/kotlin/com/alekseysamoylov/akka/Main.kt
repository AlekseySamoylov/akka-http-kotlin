package com.alekseysamoylov.akka

import akka.actor.ActorSystem
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.model.ContentTypes
import akka.http.javadsl.model.HttpEntities
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.Directives.*
import akka.http.javadsl.server.Route
import akka.http.javadsl.server.directives.FileInfo
import akka.http.javadsl.unmarshalling.Unmarshaller
import akka.japi.tuple.Tuple3
import akka.stream.Materializer
import akka.util.ByteString
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiFunction
import akka.japi.Pair;

var fileBuffer = ByteArray(0)

fun main() {
    val system = ActorSystem.create("AkkaHttpPlayground")
    val http = Http.get(system)
    val materializer = Materializer.createMaterializer(system)

    val simpleRoute =
        get {
            println("Received request")
            complete(
                HttpEntities.create(
                    ContentTypes.TEXT_HTML_UTF8, """
            <html>
             <body>
               <h1>Rock the JVM with Akka HTTP!</h1>
               <input id="image-file" type="file" onchange="SavePhoto(this)" >
                <br><br>
                Before selecting the file open chrome console > network tab to see the request details.
                <br><br>
                <small>Because in this example we send request to https://stacksnippets.net/upload/image the response code will be 404 ofcourse...</small>
                
                <br><br>
             </body>
            </html>
            <script>
            async function SavePhoto(inp) 
            {
                let user = { name:'john', age:34 };
                let formData = new FormData();
                let photo = inp.files[0];      
                     
                formData.append("photo", photo);
                formData.append("user", JSON.stringify(user)); 
                
                const ctrl = new AbortController()    // timeout
                setTimeout(() => ctrl.abort(), 5000);
                
                try {
                   let r = await fetch('http://localhost:8080', 
                     {method: "POST", body: formData, signal: ctrl.signal}); 
                   console.log('HTTP response code:',r.status); 
                } catch(e) {
                   console.log('Huston we have problem...:', e);
                }
                
            }
            </script>
        """.trimIndent()
                )
            )
        }
    val routePost = post {
        println("Post")
        entity(Unmarshaller.entityToMultipartFormData()) { formData ->
            println("Entity " + formData.mediaType)
            fileBuffer = ByteArray(0)
            val fileNameFuture = formData.parts.mapAsync(1) { p ->
                println("Hello file " + p.filename)
                System.out.println("Counting size...");
                val lastReport = AtomicLong(System.currentTimeMillis());
                val lastSize = AtomicLong(0L);
                val zero = Pair.create(0L,0L)
                return@mapAsync p.entity.dataBytes.runFold(zero, { acc, curr ->
                    // toodo receive chunk
                    return@runFold receiveChunk(lastReport, lastSize, acc, curr)
                }, materializer)
                    .toCompletableFuture()
                    .thenApply { stat ->
                        println("Size is " + stat!!.first() + " in chunks: " + stat.second() + " " + fileBuffer.size)
                        if (p.filename.isPresent) {
                            File("./${p.filename.get()}").writeBytes(fileBuffer)
                        }
                        return@thenApply Tuple3.create(p.name, p.filename, stat.first())
                    }
            }
                .runFold("", {acc, curr -> acc + curr}, materializer)
            return@entity completeOKWithFutureString(fileNameFuture)
        }
    }

    val temporaryDestination: java.util.function.Function<FileInfo, File?> = object: java.util.function.Function<FileInfo, File?> {
        override fun apply(info: FileInfo): File? {
            return try {
                println("File name {}" + info.fieldName)
                File.createTempFile("./${info.fileName}", ".tmp")
            } catch (e: Exception) {
                null
            }
        }
    }

    val route: Route = storeUploadedFile(
        "", temporaryDestination, object : BiFunction<FileInfo, File, Route> {

            override fun apply(info: FileInfo, file: File): Route {
                println("File received {}" + info)
                file.delete()
                return complete(StatusCodes.OK)
            }
        }
    )

    val flow = route(concat(routePost, simpleRoute)).flow(system, materializer)
//    val flow = route(concat(simpleRoute, route)).flow(system, materializer)
    val bindingFuture2 = http.bindAndHandle(flow, ConnectHttp.toHost("localhost", 8080), materializer)

    println("Type RETURN to exit")
    System.`in`.read()
//    bindingFuture
//        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
//        .thenAccept { unbound -> system.terminate() } // and)
    bindingFuture2
        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
        .thenAccept { unbound -> system.terminate() } // and shutdown when done
}


private fun receiveChunk(
    lastReport: AtomicLong,
    lastSize: AtomicLong,
    counter: Pair<Long, Long>,
    chunk: ByteString
): Pair<Long?, Long?>? {
    val oldSize: Long = counter.first()
    val oldChunks: Long = counter.second()
    val newSize = oldSize + chunk.size()
    val newChunks = oldChunks + 1
    val now = System.currentTimeMillis()
    if (now > lastReport.get() + 10) {
        val lastedTotal = now - lastReport.get()
        val bytesSinceLast = newSize - lastSize.get()
        val speedMBPS =
            bytesSinceLast.toDouble() / 1000000 /* bytes per MB */ / lastedTotal * 1000 /* millis per second */
        println("Already got " + newChunks + " chunks with total size " + newSize + " bytes avg chunksize " + newSize / newChunks + " bytes/chunk speed: " + speedMBPS + " MB/s")
        lastReport.set(now)
        lastSize.set(newSize)
    }
    println("received chunk" + chunk.size())
    fileBuffer += chunk.toArray()
    return Pair.create(newSize, newChunks)
}
