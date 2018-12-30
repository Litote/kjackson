[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.litote.jackson/jackson-module-loader/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.litote.jackson/jackson-module-loader)
[![Apache2 license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

## Jackson Module Loader

How to use:

- Implement `org.litote.jackson.JacksonModuleServiceLoader`
- Add the implementation class name in `META-INF/services/org.litote.jackson.JacksonModuleServiceLoader`
- Use `ObjectMapper.registerModulesFromServiceLoader()` extension to register all modules at runtime.