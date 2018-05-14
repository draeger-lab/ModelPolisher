# ModelPolisher 
**Annotating Systems Biology Models**

<img align="right" src="doc/img/ModelPolisherIcon256.png" width="64"/>

*Authors:* [Andreas Dräger](https://github.com/draeger/), [Thomas J. Zajac](https://github.com/mephenor/), [Matthias König](https://github.com/matthiaskoenig)

[![Build Status](https://travis-ci.org/SBRG/ModelPolisher.svg?branch=master&style=plastic)](https://travis-ci.org/SBRG/ModelPolisher)
[![Stable version](https://img.shields.io/badge/Stable_version-1.7-brightgreen.svg?style=plastic)](https://github.com/draeger-lab/ModelPolisher/releases/)
[![DOI](http://img.shields.io/badge/DOI-10.1371%20%2F%20journal.pone.0149263-blue.svg?style=plastic)](https://doi.org/10.1371/journal.pone.0149263)
[![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=plastic)](http://opensource.org/licenses/MIT)

ModelPolisher accesses the [BiGG Models knowledgebase](http://bigg.ucsd.edu) to annotate and autocomplete [SBML](http://sbml.org) models.
Thereby, the program mainly relies on [BiGG identifiers](https://github.com/SBRG/bigg_models/wiki/BiGG-Models-ID-Specification-and-Guidelines) for model components.
Moreover, it fixes some apparent errors in the models.

ModelPolisher is primarily a command-line based tool. You can run it locally using your installation of BiGG Models database (see https://github.com/SBRG/bigg_models). A list of all available command-line options is printed when starting ModelPolisher with the option `-?`, i.e., by typing `java -jar ModelPolisher-VERSION.jar -?`, where "VERSION" needs to be replaced with the current release version of the program. If you run into trouble with larger files, especially _java.lang.OutOfMemoryError: GC overhead limit exceeded_, please use the JVM `Xmx` flag (e.g., `java -Xmx4G -jar ModelPolisher-VERSION.jar` ).

# How to cite ModelPolisher?

The online version of ModelPolisher is described in this article: http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0149263

The article http://nar.oxfordjournals.org/content/44/D1/D515 describes BiGG Models knowledge-base including ModelPolisher, http://nar.oxfordjournals.org/content/44/D1/D515

# Usage

After cloning or updating this project, make sure to delete `bigg.zip` and `bigg.sqlite` within the folder `resources`. Afterwards run `configureSQLiteDB` to download and prepare the correct version of BiGG Models database from Dropbox.

# Licenses

ModelPolisher is distributed under the MIT License (see LICENSE).
An Overview of all dependencies is provided in [THIRD-PARTY.txt](https://github.com/draeger-lab/ModelPolisher/blob/master/THIRD-PARTY.txt), their respective licenses can be found in the licenses folder.
