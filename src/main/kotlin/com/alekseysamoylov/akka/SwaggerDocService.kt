package com.alekseysamoylov.akka

import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers
import akka.http.javadsl.server.Route
import com.github.swagger.akka.javadsl.Converter
import com.github.swagger.akka.javadsl.SwaggerGenerator
import io.swagger.v3.oas.models.info.Info
import java.util.*


class SwaggerDocService : AllDirectives() {
    var generator = object : SwaggerGenerator {
        override fun apiClasses(): Set<Class<*>?>? {
            return Collections.singleton(MinimalHttpApp::class.java)
        }

        override fun info(): Info {
            return Info().description("Simple akka-http application").version("1.0")
        }

        override fun converter(): Converter {
            return Converter(this)
        }
    }

    fun createRoute(): Route {
        return concat(
            path(
                PathMatchers.segment(generator.apiDocsPath()).slash("swagger.json")
            ) { get { complete(generator.generateSwaggerJson()) } }
        )
    }

    fun createUiRoute(): Route {
        return concat(
            path(
                PathMatchers.segment(generator.apiDocsPath()).slash("swagger-ui")
            ) { getFromResourceDirectory("swagger-ui") }
        )
    }

}
