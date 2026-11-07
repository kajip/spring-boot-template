# クラス図

`./gradlew generateDocs` で生成する。手で編集しない。

```mermaid
classDiagram
  class Greeting {
    <<record>>
    -Long id
    -String message
    +id() Long
    +message() String
  }
  class GreetingController {
    -GreetingRepository repository
    ~greetings() List~Greeting~
  }
  class GreetingRepository {
    <<interface>>
  }
  class HelloController {
    ~hello() String
  }
  class SpringBootTemplateApplication {
    +main(String[]) void
  }
  GreetingController --> GreetingRepository : repository
```
