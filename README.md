# Lumber
A logger based entirely on [Timber]'s excellent small, extensible API.

I needed similar logging functionality in a pure Kotlin module, and so Lumber was born.

Behavior is added through `Tree` instances. You can install an instance by calling `Lumber.plant`.
Installation of `Tree`s should be done as early as possible. 

The `DebugTree` implementation will automatically figure out from which class it's being called and
use that class name as its tag. While it uses `java.util.logging.Logger` for the actual logging, the 
`DebugTree` can be extended and made to use what ever utility you want.

_"There are no `Tree` implementations installed by default because every time you log in production, a
puppy dies."_ - Jake Wharton

Usage
-----

Two easy steps:

1. Install any `Tree` instances you want early on in your application.
2. Call `Lumber`'s static methods everywhere throughout your app.


Download
--------

```groovy
repositories {
  mavenCentral()
}

dependencies {
  implementation 'is.hth:lumber:0.1.0'
}
```

<details>
<summary>Snapshots of the development version are available in Sonatype's snapshots repository.</summary>
<p>

```groovy
repositories {
  mavenCentral()
  maven {
    url 'https://oss.sonatype.org/content/repositories/snapshots/'
  }
}

dependencies {
  implementation 'is.hth:lumber:0.1.0-SNAPSHOT'
}
```

</p>
</details>


License
-------

    Copyright 2022 Hrafn Thorvaldsson

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[Timber]: https://github.com/JakeWharton/timber