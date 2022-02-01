
# Raft simulator

## Requirements

The following requirements must be met in order to run the project:

- JDK 17.0.1 (or later)
- Apache Maven 3.8.4 (or later)

To install these dependencies, refer to your OS's package manager, if available. For Debian-like systems, download the jdk directly

```
wget https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.deb
```

Then, install it with thee `dpkg` command.

```
sudo dpkg -i jdk-17_linux-x64_bin.deb
```

Project dependencies (such as JavaFX) are managed by Maven, and will be pulled and installed once the project is imported and set up. At the moment, we do not provide an executable jar file.

To get started, obtain the source code from the GitHub repository:

```
git clone git@github.com:andreastedile/raft-simulator.git
```

Then, move into the directory, and use Maven to start the project:

```
mvn javafx:run
```

Maven stores all the information about the project in the `pom.xml` file, which is available in the project root. This includes the JavaFX plugin, which enables Maven to automatically link JavaFX libraries with the compiled Java code.

Once the aformentioned command is run, Maven will query the repository, pull the relevant dependencies, build the project, and finally open a pop-up Java window.
