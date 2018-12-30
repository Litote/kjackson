[![Apache2 license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

Kotlin utilities for jackson.

Used in conjunction with [jackson-module-kotlin](https://github.com/FasterXML/jackson-module-kotlin)

Two modules are available:

- [jackson-module-loader](jackson-module-loader): Dynamically load Jackson Modules using ServiceLoader
- [jackson-generator](jackson-generator): Jackson Kotlin Annotation Processor that generates Jackson serializers and deserializers, in order to avoid reflection and improve performance.