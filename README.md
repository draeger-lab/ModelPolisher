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

# Caveat

COBRAparser currently does not work with Java 9 due to an incompatibility in its dependency. Please use Java 8.

# How to cite ModelPolisher?

The online version of ModelPolisher is described in this article: http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0149263

The article ["BiGG Models: A platform for integrating, standardizing and sharing genome-scale models"](https://nar.oxfordjournals.org/content/44/D1/D515) describes BiGG Models knowledge-base including ModelPolisher.

# How to build?

ModelPolisher uses `gradle` to build. Make sure you have `gradle` installed in your system before following the procedure below.

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

#How to polish models?

Note: use Java 8 to run ModelPolisher.

For polishing models, you essentially need to run ModelPolisher using either of the `jar` built from above instructions. It is easiest to run ModelPolisher using `fatJar`. 

####Using `fatJar`
Run the following command:
```concept
java -jar "<path>/ModelPolisher/target/ModelPolisher-fat-1.7.jar" --input=<input> --output=<output> --annotate-with-bigg=true
```
####Using `lightJar`
For using `lightJar`, you need to host the BiGG Database on `PostgreSQL` on your local system. So, after installing `PostgreSQL` in your system download the database dump from [here](https://www.dropbox.com/sh/yayfmcrsrtrcypw/AACDoew92pCYlSJa8vCs5rSMa?dl=0).

Create a new empty database in `PostgreSQL` and restore `database.dump`.

Now, run the following command:
```concept
java -jar "<path>/ModelPolisher/target/ModelPolisher-noDB-1.7.jar" --input=<input> --output=<output> --annotate-with-bigg=true --host=127.0.0.1 --port=5432 --dbname=<postgres_dbname> --user=<postgres_username> --passwd=<user_password>
```
Make changes in the postgres credentials as required.

Note that, one may pass directories as `input` and `output`. In that case each file from `input` will be polished and saved with same name in the `output`.

# Licenses

ModelPolisher is distributed under the MIT License (see LICENSE).
An Overview of all dependencies is provided in [THIRD-PARTY.txt](https://github.com/draeger-lab/ModelPolisher/blob/master/THIRD-PARTY.txt), their respective licenses can be found in the licenses folder.
