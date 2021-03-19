# RxData ![Version](https://img.shields.io/github/license/revolut-mobile/RxData) ![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)

RxData is Android mobile library for building reactive data flow in Android application.

![](images/rxdata_background.png)

## Installation

Gradle is the only supported build configuration. Since 1.4 RxData is hosted on mavenCentral, 
in order to fetch the dependency, add the following lines to your project level `build.gradle.kts`:

```
allprojects {
    repositories {
        mavenCentral()
    }
}
```

And then to the module level `build.gradle.kts`:

```
dependencies {
    implementation 'com.revolut.rxdata:dod:1.4'
    implementation 'com.revolut.rxdata:core:1.4'
    implementation 'com.revolut.rxdata:scheduler:1.4'
}
```

You can add only the necessary modules. Core would be required everywhere for defining interfaces 
and working with models, but core only needed in data layer, where you are going to be implementing 
DataObservableDelegate. 

Scheduler is completely optional library for Android modules. 

## Snapshots

For using snapshot dependencies use the separate repository:

```
allprojects {
    repositories {
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
```


## Examples

You can find several examples of how RxData is used in Revolut application [in this Revolut Tech article.][1]
Also, fully documentation TBD soon [here.][3]

Here is the exemplary code that get you started in your application:

```kotlin
private val observePortfolio: DataObservableDelegate<Any, String, Portfolio> = DataObservableDelegate(
    fromNetwork = {
        tradingService.getPortfolio()
            .flatMap { portfolioDto ->
                getConfig().map { stocksConfig -> portfolioDto.toDomain(stocksConfig) }
            }         
    }

    // You can define other network / memory / storage lambdas here
)
```

## Contribution

You can also take part in improving RxData codebase! We do appreciate community engagement in that project.

You can propose bugfix or improvement to this project by [submitting a pull request.][2]

When sharing the code, please make sure that your contribution follows the existing code convention to let keep the code clean and readable.

## License


    Copyright 2019 Revolut

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 [1]: https://medium.com/revolut/reactive-data-flow-in-revolut-android-app-30a49fa1572e
 [2]: https://github.com/revolut-mobile/RxData/pulls
 [3]: https://github.com/ReactiveX/RxJava/wiki
