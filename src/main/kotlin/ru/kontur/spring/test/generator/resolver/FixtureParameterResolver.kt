package ru.kontur.spring.test.generator.resolver

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import ru.kontur.spring.test.generator.annotations.Fixture
import ru.kontur.spring.test.generator.annotations.Generate
import ru.kontur.spring.test.generator.annotations.JavaxFixture
import ru.kontur.spring.test.generator.api.SpringTestDataGenerator
import ru.kontur.spring.test.generator.api.ValidationConstructor
import ru.kontur.spring.test.generator.constructors.UUIDConstructor
import ru.kontur.spring.test.generator.resolver.strategy.FixtureResolverStrategy
import ru.kontur.spring.test.generator.resolver.strategy.JavaxFixtureResolverStrategy
import java.util.*
import kotlin.reflect.KClass

class FixtureParameterResolver : ParameterResolver {
    private val defaultConstructors: Map<KClass<*>, ValidationConstructor<*>> = mapOf(
        UUID::class to UUIDConstructor()
    )

    private val resolveStrategies = mapOf(
        Generate::class to JavaxFixtureResolverStrategy(defaultConstructors),
        JavaxFixture::class to JavaxFixtureResolverStrategy(defaultConstructors)
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.annotations.filterIsInstance<Generate>().isNotEmpty() ||
                parameterContext.parameter.annotations.filterIsInstance<Fixture>().isNotEmpty() ||
                parameterContext.parameter.annotations.filterIsInstance<JavaxFixture>().isNotEmpty()
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val fixture = parameterContext.parameter.annotations.filterIsInstance<Fixture>()
        val legacyFixture = parameterContext.parameter.annotations.filterIsInstance<Generate>()
        val javaxFixture = parameterContext.parameter.annotations.filterIsInstance<JavaxFixture>()

        return when {
            legacyFixture.isNotEmpty() -> {
                resolveStrategies[legacyFixture.first().annotationClass]?.resolve(parameterContext, extensionContext)
                    ?: throw IllegalArgumentException("Can't find strategy for such annotation")
            }
            fixture.isNotEmpty() -> {
                val annotation =
                    extensionContext.testInstance.get()::class.annotations.firstOrNull { it is SpringTestDataGenerator } as? SpringTestDataGenerator
                val fixtureStrategy = FixtureResolverStrategy(
                    defaultConstructors,
                    requireNotNull(annotation?.value) { "Please annotate your class with path" })
                fixtureStrategy.resolve(parameterContext, extensionContext)
            }
            javaxFixture.isNotEmpty() -> {
                resolveStrategies[javaxFixture.first().annotationClass]?.resolve(parameterContext, extensionContext)
                    ?: throw IllegalArgumentException("Can't find strategy for such annotation")
            }
            else -> {
                throw IllegalArgumentException("Class was not annotated, something went wrong")
            }
        }
    }
}