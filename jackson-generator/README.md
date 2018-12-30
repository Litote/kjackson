[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.litote.jackson/jackson-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.litote.jackson/jackson-generator)
[![Apache2 license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

Generates jackson serializers and deserializers. The goal is to avoid Kotlin reflection, that can be costly.

Only class with no inheritance, and all not transient properties in constructor are supported.

How to use:

1) Add the [jackson-data](../jackson-data) dependency (to get @JacksonData annotation) 
and [jackson-module-loader](../jackson-module-loader) dependency (to be allowed to load dynamic Jackson modules)
to your project dependencies 
2) Annotate with @JacksonData the target class
3) Declare the Annotation processor:

For example in maven:

```xml
<plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin}</version>

                <executions>
                    <execution>
                        <id>kapt</id>
                        <goals>
                            <goal>kapt</goal>
                        </goals>
                        <configuration>
                            <annotationProcessorPaths>>
                                </annotationProcessorPath>
                                <annotationProcessorPath>
                                    <groupId>org.litote.jackson</groupId>
                                    <artifactId>jackson-generator</artifactId>
                                    <version>${jackson-generator}</version>
                                </annotationProcessorPath>
                            </annotationProcessorPaths>
                        </configuration>
                    </execution>
</plugin>                    

```

4) Use `ObjectMapper.registerModulesFromServiceLoader()` extension to register all generated modules at runtime. 