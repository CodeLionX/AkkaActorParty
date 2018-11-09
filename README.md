# AkkaActorParty

## Usage

You need sbt to build this project.
See [Set up development environment](#set-up-development-environment-linux) for instructions to install sbt locally.

Follow these steps to run the hasher application:

- Clone this repository, via `git clone https://github.com/LeanaNeuber/AkkaActorParty.git` and checkout the desired version (`master` branch)
- Build artifact (fat jar)
  ```sh
  > sbt assembly
  ```
- Run application with java:
  ```sh
  > java -jar target/scala-2.12/hasher-assembly-<version>.jar
  ```

An alternative method to running this application via java is starting it directly with sbt.
In this case sbt will build it for you automatically.
Use the following command to run hasher with sbt:

```sh
> sbt "run"
```

You may be asked to chose a main class, if there were multiple main classes detected.
Select `com.github.leananeuber.hasher.HasherActorSystem` for the hasher application.

## Set up development environment (Linux)

### Use commandline tools
- Install JDK, eg. JDK-1.8:

  ```sh
  > sudo apt install openjdk-8-jdk
  ```

- Install `sbt`, see [Download SBT](https://www.scala-sbt.org/download.html):

  ```sh
  > echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
  > sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
  > sudo apt-get update
  > sudo apt-get install sbt
  ```


### Use IDE, eg. Intellij IDEA

- Install JDK, eg. JDK-1.8:

  ```sh
  > sudo apt install openjdk-8-jdk
  ```

- [Download](https://www.jetbrains.com/idea/download/#section=linux) and install Intellij IDEA
- Install _Scala_ and _SBT_ plugins
- Import project _as an SBT project_ (only available after installing the _SBT_ plugin)
