# ModelPolisher 
**Annotating Systems Biology Models**

<img align="right" src="doc/img/ModelPolisherIcon256.png" width="64"/>

*Authors:* [Andreas Dräger](https://github.com/draeger/), [Thomas J. Zajac](https://github.com/mephenor/), [Matthias König](https://github.com/matthiaskoenig)

[![Build Status](https://travis-ci.org/draeger-lab/ModelPolisher.svg?branch=master?style=plastic)](https://travis-ci.org/draeger-lab/ModelPolisher)
[![Stable version](https://img.shields.io/badge/Stable_version-1.7-brightgreen.svg?style=plastic)](https://github.com/draeger-lab/ModelPolisher/releases/)
[![DOI](http://img.shields.io/badge/DOI-10.1371%20%2F%20journal.pone.0149263-blue.svg?style=plastic)](https://doi.org/10.1371/journal.pone.0149263)
[![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=plastic)](http://opensource.org/licenses/MIT)

ModelPolisher accesses the [BiGG Models knowledgebase](http://bigg.ucsd.edu) to annotate and autocomplete [SBML](http://sbml.org) models.
Thereby, the program mainly relies on [BiGG identifiers](https://github.com/SBRG/bigg_models/wiki/BiGG-Models-ID-Specification-and-Guidelines) for model components.
Moreover, it fixes some apparent errors in the models.

ModelPolisher is primarily a command-line based tool. You can run it locally using your installation of BiGG Models database (see https://github.com/SBRG/bigg_models). A list of all available command-line options is printed when starting ModelPolisher with the option `-?`, i.e., by typing `java -jar ModelPolisher-VERSION.jar -?`, where `VERSION` needs to be replaced with the current release version of the program. If you run into trouble with larger files, especially `_java.lang.OutOfMemoryError: GC overhead limit exceeded_`, please use the JVM `Xmx` flag (e.g., `java -Xmx4G -jar ModelPolisher-VERSION.jar` ).

# How to cite ModelPolisher?

The online version of ModelPolisher is described in this article: http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0149263

The article ["BiGG Models: A platform for integrating, standardizing and sharing genome-scale models"](https://nar.oxfordjournals.org/content/44/D1/D515) describes BiGG Models knowledge-base including ModelPolisher.

# How to build?

NOTE: You may run ModelPolisher, without building, using Docker. See [here](#using-docker).

ModelPolisher uses `gradle` to build. Make sure you have `gradle (version >= 5.0)` installed in your system before following the procedure below.

First clone this github project and go to directory `<path>/ModelPolisher/`. Then, ModelPolisher can be built using Gradle, choosing one of four relevant tasks:
* `fatJar`: (default, if running Gradle without a specified task): builds ModelPolisher with dependencies and SQLite version of BiGG packaged
* `lightJar`: with dependencies, without SQLite DB
* `slimJar`: without dependencies, but with SQLite DB included
* `bareJar`: without dependencies and SQLite DB

For example to build fatJar, you can use the command:
```
gradle fatJar
```
Each such command will build a `jar` file in `<path>/ModelPolisher/target/` folder. Providing no task will automatically build a `fatJar`.

Running ModelPolisher will be easiest using `fatJar`, as then no database needs to be hosted by you, though it would be rather slow. We would recommend building lightJar and hosting database using `PostgreSQL`, see details below.

# How to polish models?

## <a name="using-docker"></a>Using Docker
ModelPolisher can be run in docker containers. This allows user to skip the build process, and database setup required to run ModelPolisher. Please install `docker` and `docker-compose`, if not installed already.

Clone ModelPolisher and change directory to ModelPolisher/:
```
git clone https://github.com/draeger-lab/ModelPolisher/
cd <path>/ModelPolisher
```
Now, bring up docker containers required for ModelPolisher(NOTE: You must be in `ModelPolisher/` directory):
```
docker-compose up
```

We recommend using docker-compose in non-detached mode when building containers for first time. If you have previously built ModelPolisher  containers use `docker-compose up --detach`.

On running these commands, databases will be restored in respective containers. After databases are successfully set up, use the following command to run ModelPolisher(in `ModelPolisher/` directory):
```
docker-compose run -v <path_directory_having_models>:/models/ java java -jar /ModelPolisher-noDB-1.7.jar --input=/models/<model_name> --output=/models/output/<output_name> --annotate-with-bigg=true --add-adb-annotations=true
```
Note: You must pass *absolute path for host directory*. This command will mount volume `<path_directory_having_models>` to `/models/` in container. Thus, outputs will be produced in directory `<path_directory_having_models>/output`.

User may use `-u <username_or_uid>` with `docker-compose run` in above command to generate output with correct ownership. Preferably use uid (obtained by `id -u <username>`) due to some existing bugs in docker.

To bring down the containers, you can run `docker-compose stop`. This will only stop the containers but won't delete them. Otherwise, you can use `docker-compose down -v` to stop and remove all containers and related volumes.

Note: `docker-compose down -v` will cause all databases to be restored again on running `docker-compose up`, as restored databases exist in containers not in images. 

Using `docker` might cause system space to be filled up, and `docker-compose up` may fail due to insufficient space. Use the following commands if there is need to empty space used by ModelPolisher_docker:
```
docker stop modelpolisher_biggdb modelpolisher_adb modelpolisher_java
docker rm modelpolisher_biggdb modelpolisher_adb modelpolisher_java
docker rmi modelpolisher_java:latest modelpolisher_adb:latest modelpolisher_biggdb:latest postgres:11.4 openjdk:11-slim
docker volume prune
```

## Without Docker
For polishing models, you essentially need to run ModelPolisher using either of the `jar` built from above instructions. It is easiest to run ModelPolisher using `fatJar`. 

#### Using `fatJar`
Run the following command:
```concept
java -jar "<.../ModelPolisher/target/ModelPolisher-fat-1.7.jar" --input=<input> --output=<output> --annotate-with-bigg=true
```

Note: You cannot add annotations from [AnnotateDB](https://github.com/matthiaskoenig/annotatedb) if you use fatJar.
#### Using `lightJar`
For using `lightJar`, you need to host the [BiGG](https://github.com/SBRG/bigg_models) Database & [AnnotateDB](https://github.com/matthiaskoenig/annotatedb) on `PostgreSQL` on your local system.

Now, you can run the following command:
```concept
java -jar "<path>/ModelPolisher/target/ModelPolisher-noDB-1.7.jar" --input=<input> --output=<output> --annotate-with-bigg=true --bigg-host=<host> --bigg-port=<port> --bigg-dbname=<postgres_dbname> --bigg-user=<postgres_username> --bigg-passwd=<user_password> --add-adb-annotations=true --adb-host=<host> --adb-port=<port> --adb-dbname=<postgres_dbname> --adb-user=<postgres_username> --adb-passwd=<user_password>
```
Make changes in the postgres credentials as required.

Note that, one may pass directories as `input` and `output`. In that case each file from `input` will be polished and saved with same name in the `output`.

## Note for Developers
We understand that a developer would need to build ModelPolisher multiple times and making required changes in `java` Dockerfile will be a tedious task.
We recommend the following practice for developers:
1. Set up required databases by running `docker-compose up`.
2. After making required changes in codebase build `jar` by `gradle lightJar`.
3. Run the newly build jar using:
```
java -jar ./target/ModelPolisher-noDB-1.7.jar --input=<input> --output=<output> --annotate-with-bigg=true --bigg-host=0.0.0.0 --bigg-port=1310 --add-adb-annotations=true --adb-host=0.0.0.0 --adb-port=1013
```
Note: All above commands must be run in `<path>/ModelPolisher/` directory and you must have installed Java `version >= 8` and Gradle `version >= 5.0`.
# Licenses

ModelPolisher is distributed under the MIT License (see LICENSE).
An Overview of all dependencies is provided in [THIRD-PARTY.txt](https://github.com/draeger-lab/ModelPolisher/blob/master/THIRD-PARTY.txt), their respective licenses can be found in the licenses folder.
